package org.godn.rc.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.godn.rc.model.Chat;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class RedisChatStore {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisChatStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Chat addChat(String userId, String name, String roomId, String message) {
        Chat newChat = new Chat();
        newChat.setId(UUID.randomUUID().toString());
        newChat.setUserId(userId);
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

    // FIX 2: Added userId to the parameters
    public Chat upvotes(String userId, String roomId, String chatId){
        Object result = redisTemplate.opsForHash().get("room:" + roomId, chatId);

        if(result == null){ return null; }

        try {
            Chat chat = objectMapper.readValue(result.toString(), Chat.class);

            // FIX 2: Add the USER ID to the set, not the chat ID!
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
}