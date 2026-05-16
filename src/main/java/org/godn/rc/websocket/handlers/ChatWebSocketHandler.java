package org.godn.rc.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.godn.rc.websocket.dto.*;
import org.godn.rc.websocket.manager.UserManager;
import org.godn.rc.router.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Set;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log =
            LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserManager userManager;
    private final MessageRouter messageRouter;
    private final Validator validator;

    public ChatWebSocketHandler(UserManager userManager,
                                MessageRouter messageRouter,
                                Validator validator) {
        this.userManager = userManager;
        this.messageRouter = messageRouter;
        this.validator = validator;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("User Connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session,
                                     TextMessage message) throws IOException {

        IncomingPayload incomingPayload;

        // ---------------- JSON Parsing ----------------
        try {
            incomingPayload = objectMapper.readValue(
                    message.getPayload(),
                    IncomingPayload.class
            );
        } catch (Exception e) {
            log.error("JSON Parsing failed: {}", e.getMessage());
            sendError(session, "Malformed JSON or invalid data types.");
            return;
        }

        // ---------------- Validation ----------------
        Set<ConstraintViolation<IncomingPayload>> violations =
                validator.validate(incomingPayload);

        if (!violations.isEmpty()) {
            sendError(session,
                    violations.iterator().next().getMessage());
            return;
        }

        InternalPayload payload = new InternalPayload();
        payload.setRoomId(incomingPayload.getRoomId());
        payload.setAction(incomingPayload.getAction());
        payload.setMessage(incomingPayload.getMessage());
        payload.setChatId(incomingPayload.getChatId());
        payload.setLimit(incomingPayload.getLimit());
        payload.setOffset(incomingPayload.getOffset());

        IncomingAction action = payload.getAction();

        String name = (String) session.getAttributes().get("name");
        if (name == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        String roomId = payload.getRoomId();
        if (roomId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // ---------------- Identity Setup ----------------
        String userId = (String) session.getAttributes().get("USER_ID");
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        payload.setUserId(userId);

        try {

            // -------- CREATE ROOM --------
            if (roomId.isEmpty()) {

                if (action == IncomingAction.CREATE_ROOM) {

                    roomId = generateRoomCode();
                    payload.setRoomId(roomId);

                    // Route first (initialize Redis room)
                    messageRouter.routeIncomingMessage(session, payload);

                    // Wrap session safely
                    WebSocketSession safeSession =
                            new ConcurrentWebSocketSessionDecorator(
                                    session,
                                    5000,
                                    1024 * 1024
                            );

                    userManager.addUser(roomId, userId, name, safeSession);
                    return;

                } else {
                    sendError(session,
                            "Room ID is required for this action.");
                    return;
                }
            }

            // -------- JOIN ROOM IF NOT PRESENT --------
            if (!userManager.isUserInRoom(roomId, userId)) {

                WebSocketSession safeSession =
                        new ConcurrentWebSocketSessionDecorator(
                                session,
                                5000,
                                1024 * 1024
                        );

                userManager.addUser(roomId, userId, name, safeSession);
            }

            messageRouter.routeIncomingMessage(session, payload);

        } catch (Exception e) {
            log.error("Action failed for user {}: {}",
                    userId, e.getMessage());
            sendError(session, "Internal server error.");
        }
    }

    public void broadcastToLocalUsers(TextMessage message) {

        try {
            JsonNode rootNode =
                    objectMapper.readTree(message.getPayload());

            JsonNode payloadNode = rootNode.path("payload");

            String roomId =
                    payloadNode.path("roomId").asText(null);

            String senderUserId =
                    payloadNode.path("userId").asText(null);

            if (roomId == null) {
                log.error("Broadcast failed: roomId missing.");
                return;
            }

            for (WebSocketSession session :
                    userManager.getSessionsForRoom(roomId)) {

                if (!session.isOpen()) continue;

                String sessionUserId =
                        (String) session.getAttributes()
                                .get("USER_ID");

                if (senderUserId != null &&
                        senderUserId.equals(sessionUserId)) {
                    continue;
                }

                synchronized (session) {
                    try {
                        session.sendMessage(message);
                    } catch (Exception ex) {
                        log.error("Send failed for session {}: {}",
                                session.getId(), ex.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            log.error("Broadcast parsing failed: {}",
                    e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session,
                                      CloseStatus status) {
        log.info("User Disconnected: {}", session.getId());

        try {
            userManager.removeUser(session);
        } catch (Exception e) {
            log.error("Error during user removal: {}",
                    e.getMessage());
        }
    }

    // ================= HELPERS =================

    private String generateRoomCode() {
        String chars =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        StringBuilder code = new StringBuilder();
        SecureRandom rnd = new SecureRandom();

        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(
                    rnd.nextInt(chars.length())));
        }

        return code.substring(0, 3) + "-" +
                code.substring(3);
    }

    private void sendError(WebSocketSession session,
                           String errorMessage) {

        try {
            OutgoingMessage errorOut =
                    new OutgoingMessage(
                            OutgoingType.ERROR,
                            errorMessage
                    );

            TextMessage textMessage =
                    new TextMessage(
                            objectMapper.writeValueAsString(errorOut)
                    );

            // 🔥 THREAD SAFE ERROR SEND
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }

        } catch (Exception e) {
            log.error("Failed to send error: {}",
                    e.getMessage());
        }
    }
}