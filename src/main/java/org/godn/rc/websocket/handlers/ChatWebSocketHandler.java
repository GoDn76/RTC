package org.godn.rc.websocket.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.godn.rc.entity.ChatRoomType;
import org.godn.rc.websocket.dto.*;
import org.godn.rc.websocket.manager.UserManager;
import org.godn.rc.websocket.router.MessageRouter;
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
        String userId = (String)session.getAttributes().get("USER_ID");
        String name = (String)session.getAttributes().get("name");
        userManager.markActive(userId, name, session);
    }


    @Override
    protected void handleTextMessage(WebSocketSession session,
                                     TextMessage message) throws IOException {

        IncomingPayload incomingPayload;

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

        Set<ConstraintViolation<IncomingPayload>> violations =
                validator.validate(incomingPayload);

        if (!violations.isEmpty()) {
            sendError(session,
                    violations.iterator().next().getMessage());
            return;
        }

        String userId =
                (String) session.getAttributes().get("USER_ID");

        String name =
                (String) session.getAttributes().get("name");

        if (userId == null || name == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        InternalPayload payload = new InternalPayload();

        payload.setAction(incomingPayload.getAction());
        payload.setMessage(incomingPayload.getMessage());
        payload.setChatId(incomingPayload.getChatId());
        payload.setLimit(incomingPayload.getLimit());
        payload.setOffset(incomingPayload.getOffset());

        payload.setUserId(userId);
        payload.setName(name);

        String targetUserId =
                incomingPayload.getTargetUserId();

        String roomId =
                incomingPayload.getRoomId() == null
                        ? ""
                        : incomingPayload.getRoomId().trim();

        try {

            // =====================================================
            // CREATE ROOM
            // =====================================================
            if (payload.getAction() == IncomingAction.CREATE_ROOM) {

                // ---------- DM ----------
                if (targetUserId != null && !targetUserId.isBlank()) {

                    ChatUser target =
                            userManager.getUser(targetUserId);

                    if (target == null) {
                        sendError(session, "User not found");
                        return;
                    }

                    payload.setRoomType(ChatRoomType.PRIVATE);

                    roomId = messageRouter.registerDMRoom(
                            userId,
                            targetUserId
                    );

                    userManager.addUserToRoom(
                            roomId,
                            userId
                    );

                    userManager.addUserToRoom(
                            roomId,
                            targetUserId
                    );
                }

                // ---------- GROUP ----------
                else {

                    payload.setRoomType(ChatRoomType.GROUP);

                    roomId = generateRoomCode();

                    userManager.addUserToRoom(
                            roomId,
                            userId
                    );
                }

                payload.setRoomId(roomId);

                messageRouter.routeIncomingMessage(
                        session,
                        payload
                );

                return;
            }

            // =====================================================
            // CHAT + GET_CHATS
            // =====================================================
            if (payload.getAction() == IncomingAction.CHAT
                    || payload.getAction() == IncomingAction.GET_CHATS) {

                // ---------- DM ----------
                if (targetUserId != null && !targetUserId.isBlank()) {

                    ChatUser target =
                            userManager.getUser(targetUserId);

                    if (target == null) {
                        sendError(session, "User not found");
                        return;
                    }

                    roomId = messageRouter.registerDMRoom(
                            userId,
                            targetUserId
                    );
                }

                // ---------- GROUP ----------
                else {

                    if (roomId.isBlank()) {

                        sendError(
                                session,
                                "Room ID required."
                        );

                        return;
                    }
                }

                payload.setRoomId(roomId);

                messageRouter.routeIncomingMessage(
                        session,
                        payload
                );

                return;
            }

            // =====================================================
            // OTHER ACTIONS
            // =====================================================
            payload.setRoomId(roomId);

            messageRouter.routeIncomingMessage(
                    session,
                    payload
            );

        } catch (Exception e) {

            log.error(
                    "Action failed for user {}: {}",
                    userId,
                    e.getMessage(),
                    e
            );

            sendError(
                    session,
                    "Internal server error."
            );
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

        String userId = (String)session.getAttributes().get("USER_ID");
        userManager.markOffline(userId, session);
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