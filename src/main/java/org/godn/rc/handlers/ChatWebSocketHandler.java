package org.godn.rc.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.godn.rc.dto.IncomingAction;
import org.godn.rc.dto.IncomingPayload;
import org.godn.rc.dto.OutgoingMessage;
import org.godn.rc.dto.OutgoingType;
import org.godn.rc.manager.UserManager;
import org.godn.rc.router.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Random;
import java.util.Set;
import java.util.UUID;

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
                                     TextMessage message) {

        IncomingPayload payload;

        // ---------------- JSON Parsing ----------------
        try {
            payload = objectMapper.readValue(
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
                validator.validate(payload);

        if (!violations.isEmpty()) {
            sendError(session,
                    violations.iterator().next().getMessage());
            return;
        }

        IncomingAction action = payload.getAction();
        String name = payload.getName() != null
                ? payload.getName().trim()
                : "Anonymous";

        String roomId = payload.getRoomId() != null
                ? payload.getRoomId().trim()
                : "";

        // ---------------- Identity Setup ----------------
        String userId =
                (String) session.getAttributes().get("USER_ID");

        if (userId == null) {
            userId = UUID.randomUUID().toString();
            session.getAttributes().put("USER_ID", userId);
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
            if (!userManager.isUserInRoom(session.getId())) {

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

    // ================= THREAD SAFE BROADCAST =================

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
        Random rnd = new Random();

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