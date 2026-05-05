package org.godn.rc.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.godn.rc.dto.Chat;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class RedisChatStore {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisChatStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void initializeRoom(String roomId, String creatorName) {
        String key = "room:" + roomId + ":metadata";

        Map<String, String> metadata = new HashMap<>();
        metadata.put("creator", creatorName);
        metadata.put("createdAt", String.valueOf(System.currentTimeMillis()));

        redisTemplate.opsForHash().putAll(key, metadata);

        redisTemplate.opsForSet().add("all_active_rooms", roomId);
    }

    public Chat addChat(String userId, String name, String roomId, String message) {
        Chat newChat = new Chat();
        newChat.setId(UUID.randomUUID().toString());
        newChat.setUserId(userId);
        newChat.setRoomId(roomId);
        newChat.setName(name);
        newChat.setMessage(message);
        newChat.setTimestamp(System.currentTimeMillis());

        try {
            String chatJson = objectMapper.writeValueAsString(newChat);

            String key = "room:" + roomId + ":chats";

            // score = timestamp
            redisTemplate.opsForZSet().add(
                    key,
                    chatJson,
                    newChat.getTimestamp()
            );

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize chat", e);
        }

        return newChat;
    }

    public List<Chat> getChat(String roomId, int limit, int offset) {

        String key = "room:" + roomId + ":chats";

        Set<String> rawChats = redisTemplate.opsForZSet()
                .reverseRange(key, offset, offset + limit - 1);

        if (rawChats == null || rawChats.isEmpty()) {
            return Collections.emptyList();
        }

        List<Chat> parsedChats = new ArrayList<>();

        for (String rawChat : rawChats) {
            try {
                parsedChats.add(
                        objectMapper.readValue(rawChat, Chat.class)
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize chat", e);
            }
        }

        return parsedChats;
    }

    public Chat upvotes(String userId, String roomId, String chatId){
        Object result = redisTemplate.opsForHash().get("room:" + roomId, chatId);

        if(result == null){ return null; }

        try {
            Chat chat = objectMapper.readValue(result.toString(), Chat.class);

            boolean isUniqueUpVote = chat.getUpvotes().add(userId);

            if(isUniqueUpVote){
                String updatedJson = objectMapper.writeValueAsString(chat);
                redisTemplate.opsForHash().put("room:"+roomId, chatId, updatedJson);
            }
            return chat;
        } catch (JsonProcessingException e){
            e.printStackTrace();
            return null;
        }
    }
    public Set<String> getAllActiveRooms() {
        return redisTemplate.opsForSet().members("all_active_rooms");
    }

    public void incrementMemberCount(String roomId) {
        redisTemplate.opsForValue().increment("room:" + roomId + ":count");
    }

    public void decrementMemberCount(String roomId) {
        redisTemplate.opsForValue().decrement("room:" + roomId + ":count");
    }

    public Long getMemberCount(String roomId) {
        String value = redisTemplate.opsForValue()
                .get("room:" + roomId + ":count");

        return value != null ? Long.parseLong(value) : 0L;
    }

    public void deleteRoom(String roomId) {
        redisTemplate.delete("room:" + roomId + ":metadata");
        redisTemplate.delete("room:" + roomId);
        redisTemplate.opsForValue().getOperations().delete("room:" + roomId + ":count");
        redisTemplate.opsForSet().remove("all_active_rooms", roomId);
    }

    public boolean roomExists(String roomId) {
        return redisTemplate.hasKey("room:" + roomId + ":metadata");
    }

    private static final Duration ROOM_TTL = Duration.ofHours(2);

    public void refreshRoomTTL(String roomId) {

        String metaKey = "room:" + roomId + ":meta";
        String chatKey = "room:" + roomId + ":chats";

        redisTemplate.expire(metaKey, ROOM_TTL);
        redisTemplate.expire(chatKey, ROOM_TTL);
    }
}