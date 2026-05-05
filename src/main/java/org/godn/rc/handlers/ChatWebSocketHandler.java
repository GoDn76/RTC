package org.godn.rc.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
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
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserManager userManager;
    private final MessageRouter messageRouter;
    private final Validator validator;

    public ChatWebSocketHandler(UserManager userManager, MessageRouter messageRouter, Validator validator) {
        this.userManager = userManager;
        this.messageRouter = messageRouter;
        this.validator = validator;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("User Connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        IncomingPayload payload;

        // JSON Parsing
        try {
            payload = objectMapper.readValue(message.getPayload(), IncomingPayload.class);
        } catch (Exception e) {
            log.error("JSON Parsing failed: {}", e.getMessage());
            sendError(session, "Malformed JSON or invalid data types.");
            return;
        }

        // Validation
        Set<ConstraintViolation<IncomingPayload>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            sendError(session, violations.iterator().next().getMessage());
            return;
        }

        // Identity Setup
        IncomingAction action = payload.getAction();
        String name = (payload.getName() != null) ? payload.getName().trim() : "Anonymous";
        String roomId = (payload.getRoomId() != null) ? payload.getRoomId().trim() : "";

        String userId = (String) session.getAttributes().get("SECURE_USER_ID");
        if (userId == null) {
            userId = UUID.randomUUID().toString();
            session.getAttributes().put("SECURE_USER_ID", userId);
        }
        payload.setUserId(userId);

        // Logic
        try {
            if (roomId.isEmpty()) {
                if (action == IncomingAction.CREATE_ROOM) {
                    roomId = generateRoomCode();
                    payload.setRoomId(roomId);

                    // IMPORTANT: Route FIRST to initialize the room in Redis
                    messageRouter.routeIncomingMessage(session, payload);

                    // THEN Add to manager (the Redis check will now pass)
                    userManager.addUser(roomId, userId, name, session);
                    return;
                } else {
                    sendError(session, "Room ID is required for this action.");
                    return;
                }
            }

            if (!userManager.isUserInRoom(session.getId())) {
                userManager.addUser(roomId, userId, name, session);
            }
            messageRouter.routeIncomingMessage(session, payload);
        } catch (Exception e) {
            log.error("Action failed for user {}: {}", userId, e.getMessage());
            sendError(session, e.getMessage());
        }
    }

    public void broadcastToLocalUsers(TextMessage message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message.getPayload());
            JsonNode payloadNode = rootNode.path("payload");
            log.info(rootNode.toString());
            String roomId = payloadNode.path("roomId").asText(null);
            String senderUserId = payloadNode.path("userId").asText(null);

            if (roomId != null) {
                for (WebSocketSession webSocketSession : userManager.getSessionsForRoom(roomId)) {
                    if (webSocketSession.isOpen()) {
                        String sessionUserId = (String) webSocketSession.getAttributes().get("USER_ID");
                        if (senderUserId != null && senderUserId.equals(sessionUserId)) {
                            continue;
                        }
                        webSocketSession.sendMessage(message);
                    }
                }
            } else {
                log.error("Broadcast failed: Could not find roomId in Redis message.");
            }
        } catch (Exception e) {
            log.error("Failed to broadcast to local users: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
       log.info("User Disconnected: {}", session.getId());
       try {
           userManager.removeUser(session);
       }catch (Exception e) {
              log.error("Error during user removal: {}", e.getMessage());
       }
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        // Consistent format: 4W1-RM2
        return code.substring(0, 3) + "-" + code.substring(3);
    }

    private void sendError(WebSocketSession session, String errorMessage) throws Exception {
        OutgoingMessage errorOut = new OutgoingMessage(OutgoingType.ERROR, errorMessage);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorOut)));
    }
}