package org.godn.rc.websocket.router;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.godn.rc.entity.ChatRoomType;
import org.godn.rc.websocket.dto.Chat;
import org.godn.rc.websocket.dto.InternalPayload;
import org.godn.rc.websocket.dto.OutgoingMessage;
import org.godn.rc.websocket.dto.OutgoingType;
import org.godn.rc.redis.store.RedisChatStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.security.SecureRandom;
import java.util.*;

@Component
public class MessageRouter {

    private static final Logger log = LoggerFactory.getLogger(MessageRouter.class);
    private final RedisChatStore redisChatStore;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MessageRouter(RedisChatStore redisChatStore, StringRedisTemplate redisTemplate) {
        this.redisChatStore = redisChatStore;
        this.redisTemplate = redisTemplate;
    }

    public void routeIncomingMessage(WebSocketSession session, InternalPayload payload) throws Exception {

        switch (payload.getAction()) {
            case GET_CHATS:
                handleGetChats(session, payload);
                break;
            case CHAT:
                handleNewChat(payload);
                break;
            case UPVOTE:
                handleUpvote(payload);
                break;
            case CREATE_ROOM:
                handleCreateRoom(session, payload);
                break;
            default:
                log.error("Unhandled action reached the router: {}", payload.getAction());
        }
    }

    public String registerDMRoom (String user1, String user2) {
        List<String> users = List.of(user1, user2);
        List<String> sorted = new ArrayList<>(users);
        Collections.sort(sorted);

        String dmKey = "dm:"+sorted.get(0) + ":" + sorted.get(1);

        String roomId = redisTemplate.opsForValue().get(dmKey);

        if(roomId != null) {
            return roomId;
        }

        roomId = generateRoomCode();

        redisTemplate.opsForValue().set(dmKey, roomId);

        redisChatStore.initializeRoom(roomId, "SYSTEM", "SYSTEM", ChatRoomType.PRIVATE);

        return roomId;
    }

    private void handleCreateRoom(WebSocketSession session, InternalPayload payload) throws Exception {
        Map<String, String> roomData = new HashMap<>();
        roomData.put("roomId", payload.getRoomId());
        roomData.put("userId", payload.getUserId());
        redisChatStore.initializeRoom(payload.getRoomId(), payload.getName(), payload.getUserId(), payload.getRoomType());
        OutgoingMessage outMsg = new OutgoingMessage(OutgoingType.ROOM_CREATED, roomData);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(outMsg)));
    }

    private void handleGetChats(WebSocketSession session, InternalPayload payload) throws Exception {
        int limit = payload.getLimit() != null ? payload.getLimit() : 50;
        int offset = payload.getOffset() != null ? payload.getOffset() : 0;

        List<Chat> history = redisChatStore.getChats(payload.getRoomId(), limit, offset);

        OutgoingMessage response = new OutgoingMessage(OutgoingType.HISTORY, history);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void handleNewChat(InternalPayload payload) throws Exception {
        if(!payload.getMessage().isEmpty()) {
            Chat resultingChat = redisChatStore.addChat(
                    payload.getUserId(),
                    payload.getName(),
                    payload.getRoomId(),
                    payload.getMessage()
            );

            publishToRedis(OutgoingType.ADD_CHAT, resultingChat);
        }
    }

    private void handleUpvote(InternalPayload payload) throws Exception {

        long updatedCount = redisChatStore.upvote(
                payload.getUserId(),
                payload.getRoomId(),
                payload.getChatId()
        );

        Chat updatedChat = new Chat();
        updatedChat.setId(payload.getChatId());
        updatedChat.setRoomId(payload.getRoomId());
        updatedChat.setUpvotes(updatedCount);

        publishToRedis(OutgoingType.UPDATE_CHAT, updatedChat);
    }

    private void publishToRedis(OutgoingType actionType, Chat chat) throws Exception {

        OutgoingMessage outgoing = new OutgoingMessage(actionType, chat);
        String jsonToSend = objectMapper.writeValueAsString(outgoing);
        redisTemplate.convertAndSend("chatRoom:" + chat.getRoomId(), jsonToSend);

    }

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
}