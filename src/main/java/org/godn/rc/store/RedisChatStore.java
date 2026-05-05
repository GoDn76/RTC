package org.godn.rc.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.godn.rc.dto.Chat;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

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

        try {
            String chatJson = objectMapper.writeValueAsString(newChat);
            redisTemplate.opsForHash().put("room:"+roomId, newChat.getId(), chatJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return newChat;
    }

    public List<Chat> getChat(String roomId, int limit, int offset) {
        List<Object> rawChats = redisTemplate.opsForHash().values("room:" + roomId);
        List<Chat> parsedChats = new ArrayList<>();

        for(Object rawChat : rawChats){
            try {
                parsedChats.add(objectMapper.readValue(rawChat.toString(), Chat.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return parsedChats.stream()
                .sorted(Comparator.comparing(Chat::getTimestamp).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
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
        return redisTemplate.opsForValue().get("room:" + roomId + ":count") != null ? Long.parseLong(Objects.requireNonNull(redisTemplate.opsForValue().get("room:" + roomId + ":count"))) : 0L;
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
}