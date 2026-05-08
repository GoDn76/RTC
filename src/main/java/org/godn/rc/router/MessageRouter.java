package org.godn.rc.router;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.godn.rc.dto.Chat;
import org.godn.rc.dto.IncomingPayload;
import org.godn.rc.dto.OutgoingMessage;
import org.godn.rc.dto.OutgoingType;
import org.godn.rc.store.RedisChatStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    public void routeIncomingMessage(WebSocketSession session, IncomingPayload payload) throws Exception {

        switch (payload.getAction()) {
            case JOIN:
                handleJoin(session, payload);
                break;
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

    private void handleCreateRoom(WebSocketSession session, IncomingPayload payload) throws Exception {
        Map<String, String> roomData = new HashMap<>();
        roomData.put("roomId", payload.getRoomId());
        roomData.put("userId", payload.getUserId());
        redisChatStore.initializeRoom(payload.getRoomId(), payload.getName());
        OutgoingMessage outMsg = new OutgoingMessage(OutgoingType.ROOM_CREATED, roomData);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(outMsg)));
    }

    private void handleGetChats(WebSocketSession session, IncomingPayload payload) throws Exception {
        int limit = payload.getLimit() != null ? payload.getLimit() : 50;
        int offset = payload.getOffset() != null ? payload.getOffset() : 0;

        List<Chat> history = redisChatStore.getChats(payload.getRoomId(), limit, offset);

        OutgoingMessage response = new OutgoingMessage(OutgoingType.HISTORY, history);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void handleJoin(WebSocketSession session, IncomingPayload payload) throws Exception {
        String roomId = payload.getRoomId();

        Long memberCount = redisChatStore.getMemberCount(roomId);

        Map<String, Object> joinData = new HashMap<>();
        joinData.put("roomId", roomId);
        joinData.put("memberCount", memberCount);
        joinData.put("message", "Successfully joined room " + roomId);

        OutgoingMessage joinSuccess = new OutgoingMessage(OutgoingType.JOIN_SUCCESS, joinData);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(joinSuccess)));

        handleGetChats(session, payload);

        Chat systemAlert = new Chat();
        systemAlert.setId(UUID.randomUUID().toString());
        systemAlert.setRoomId(roomId);
        systemAlert.setMessage("A new user "+payload.getName()+" has joined the room! Say hi!");
        systemAlert.setName("System");
        systemAlert.setUserId("SYSTEM");

        publishToRedis(OutgoingType.ADD_CHAT, systemAlert);
    }

    private void handleNewChat(IncomingPayload payload) throws Exception {
        // Save to Database
        Chat resultingChat = redisChatStore.addChat(
                payload.getUserId(),
                payload.getName(),
                payload.getRoomId(),
                payload.getMessage()
        );

        publishToRedis(OutgoingType.ADD_CHAT, resultingChat);
    }

    private void handleUpvote(IncomingPayload payload) throws Exception {

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
}