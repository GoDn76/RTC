package org.godn.rc.websocket.router;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.godn.rc.entity.ChatRoomType;
import org.godn.rc.websocket.dto.*;
import org.godn.rc.redis.store.RedisChatStore;
import org.godn.rc.websocket.manager.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MessageRouter {

    private static final Logger log = LoggerFactory.getLogger(MessageRouter.class);
    private final RedisChatStore redisChatStore;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserManager userManager;

    public MessageRouter(RedisChatStore redisChatStore, StringRedisTemplate redisTemplate, UserManager userManager) {
        this.redisChatStore = redisChatStore;
        this.redisTemplate = redisTemplate;
        this.userManager = userManager;
    }

    public void routeIncomingMessage(WebSocketSession session, InternalPayload payload) throws Exception {

        switch (payload.getAction()) {
            case GET_CHATS:
                handleGetChats(session, payload);
                break;
            case CHAT:
                handleNewChat(session, payload);
                break;
            case UPVOTE:
                handleUpvote(payload);
                break;
            case CREATE_ROOM:
                handleCreateRoom(session, payload);
                break;
            case GET_ROOMS:
                handleGetRoom(session, payload.getUserId());
                break;
            case JOIN:
                handleJoinGroup(session, payload);
                break;
            default:
                log.error("Unhandled action reached the router: {}", payload.getAction());
        }
    }

    public String registerDMRoom (String user1, String user2, String user1Name, String user2Name) {
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

        InternalPayload ip = new InternalPayload();
        ip.setRoomId(roomId);
        ip.setUserId("SYSTEM");
        ip.setName("SYSTEM");
        ip.setRoomName(roomId);
        ip.setRoomType(ChatRoomType.PRIVATE);

        redisChatStore.initializeRoom(ip, user1, user2, user1Name, user2Name);

        return roomId;
    }

    private void handleGetRoom (WebSocketSession session, String userId) throws IOException {
        List<RoomInfoDto> roomInfoDtoList = redisChatStore.getUserRooms(userId)
                .stream()
                .map(roomId -> redisChatStore.getRoomInfo(roomId, userId))
                .filter(Objects::nonNull)
                .toList();


        session.sendMessage(
                new TextMessage(
                        objectMapper.writeValueAsString(
                                new OutgoingMessage(
                                        OutgoingType.ROOMS,
                                        roomInfoDtoList
                                )
                        )
                )
        );
    }
    private void handleCreateRoom(WebSocketSession session, InternalPayload payload) throws Exception {
        Map<String, String> roomData = new HashMap<>();
        roomData.put("roomId", payload.getRoomId());
        roomData.put("userId", payload.getUserId());
        roomData.put("roomName", payload.getRoomName());
        roomData.put("displayName", payload.getTargetUserId());
        redisChatStore.initializeRoom(payload);
        OutgoingMessage outMsg = new OutgoingMessage(OutgoingType.ROOM_CREATED, roomData);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(outMsg)));
    }

    private void handleJoinGroup(WebSocketSession session, InternalPayload payload) throws Exception {

        if (payload.getRoomId() == null || payload.getRoomId().isBlank()) {
            session.sendMessage(
                    new TextMessage(
                            objectMapper.writeValueAsString(
                                    new OutgoingMessage(
                                            OutgoingType.ERROR,
                                            "Room ID is required."
                                    )
                            )
                    )
            );
            return;
        }

        RoomInfoDto room = redisChatStore.getRoomInfo(
                payload.getRoomId(),
                payload.getUserId()
        );

        if (room == null) {
            session.sendMessage(
                    new TextMessage(
                            objectMapper.writeValueAsString(
                                    new OutgoingMessage(
                                            OutgoingType.ERROR,
                                            "Room not found."
                                    )
                            )
                    )
            );
            return;
        }

        if (room.getRoomType() != ChatRoomType.GROUP) {
            session.sendMessage(
                    new TextMessage(
                            objectMapper.writeValueAsString(
                                    new OutgoingMessage(
                                            OutgoingType.ERROR,
                                            "Cannot join a private room."
                                    )
                            )
                    )
            );
            return;
        }

        userManager.addUserToRoom(
                payload.getRoomId(),
                payload.getUserId()
        );
        session.sendMessage(
                new TextMessage(
                        objectMapper.writeValueAsString(
                                new OutgoingMessage(
                                        OutgoingType.JOIN_SUCCESS,
                                        payload.getName() + " Joined the Room Successfully."
                                )
                        )
                )
        );

        handleGetChats(session, payload);
    }

    private void handleGetChats(WebSocketSession session, InternalPayload payload) throws Exception {
        int limit = payload.getLimit() != null ? payload.getLimit() : 50;
        int offset = payload.getOffset() != null ? payload.getOffset() : 0;

        List<Chat> history = redisChatStore.getChats(payload.getRoomId(), limit, offset);

        OutgoingMessage response = new OutgoingMessage(OutgoingType.HISTORY, history);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void handleNewChat(WebSocketSession session, InternalPayload payload) throws Exception {
        if(!payload.getMessage().isEmpty()) {
            Chat resultingChat = redisChatStore.addChat(
                    payload.getUserId(),
                    payload.getName(),
                    payload.getRoomId(),
                    payload.getMessage()
            );

            publishToRedis(OutgoingType.ADD_CHAT, resultingChat);

            session.sendMessage(
                    new TextMessage(
                            objectMapper.writeValueAsString(new OutgoingMessage(OutgoingType.ADD_CHAT, resultingChat))
                    )
            );
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