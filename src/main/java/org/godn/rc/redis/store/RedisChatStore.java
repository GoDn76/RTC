package org.godn.rc.redis.store;

import org.godn.rc.entity.ChatRoomType;
import org.godn.rc.websocket.dto.Chat;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class RedisChatStore {

    private final StringRedisTemplate redisTemplate;

    public RedisChatStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /* =====================================================
       ROOM MANAGEMENT
    ===================================================== */

    public void initializeRoom(String roomId, String creatorName, String creatorId, ChatRoomType roomType) {

        String metadataKey = "room:" + roomId + ":metadata";

        Map<String, String> metadata = new HashMap<>();
        metadata.put("roomId", roomId);
        metadata.put("creator", creatorName);
        metadata.put("creatorId", creatorId);
        metadata.put("createdAt", String.valueOf(System.currentTimeMillis()));
        metadata.put("roomType", roomType.name());

        redisTemplate.opsForHash().putAll(metadataKey, metadata);
        redisTemplate.opsForSet().add("all_active_rooms", roomId);
    }


    public boolean roomExists(String roomId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey("room:" + roomId + ":metadata")
        );
    }

    public void deleteRoom(String roomId) {

        Set<String> members = getMembers(roomId);

        for (String userId : members) {
            removeReverseMembership(roomId, userId);
        }

        String messagesIndexKey = "room:" + roomId + ":messages";

        Set<String> messageIds =
                redisTemplate.opsForZSet().range(messagesIndexKey, 0, -1);

        if (messageIds != null) {
            for (String messageId : messageIds) {
                redisTemplate.delete("room:" + roomId + ":message:" + messageId);
                redisTemplate.delete("room:" + roomId + ":message:" + messageId + ":upvotes");
            }
        }

        redisTemplate.delete("room:" + roomId + ":metadata");
        redisTemplate.delete(messagesIndexKey);
        redisTemplate.delete("room:" + roomId + ":count");
        redisTemplate.delete("room:" + roomId + ":members");

        redisTemplate.opsForSet().remove("all_active_rooms", roomId);
    }

    /* =====================================================
       MEMBER COUNT (ROOM LIFECYCLE CONTROL)
    ===================================================== */

    public void incrementActiveCount(String roomId) {
        redisTemplate.opsForValue().increment("room:" + roomId + ":count");
    }

    public void decrementActiveCount(String roomId) {

        String key = "room:" + roomId + ":count";

        Long remaining = redisTemplate.opsForValue().decrement(key);

        if (remaining == null || remaining <= 0) {
            deleteRoom(roomId);
        }
    }

    public Long getActiveCount(String roomId) {
        String value = redisTemplate.opsForValue()
                .get("room:" + roomId + ":count");

        return value != null ? Long.parseLong(value) : 0L;
    }

    /* =====================================================
       MESSAGE STORAGE (IMMUTABLE DESIGN)
    ===================================================== */

    public Chat addChat(String userId, String name, String roomId, String message) {

        String chatId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();

        Chat chat = new Chat();
        chat.setId(chatId);
        chat.setUserId(userId);
        chat.setRoomId(roomId);
        chat.setName(name);
        chat.setMessage(message);
        chat.setTimestamp(timestamp);

        String messageKey = "room:" + roomId + ":message:" + chatId;
        String messagesIndexKey = "room:" + roomId + ":messages";

        Map<String, String> chatMap = new HashMap<>();
        chatMap.put("id", chatId);
        chatMap.put("userId", userId);
        chatMap.put("roomId", roomId);
        chatMap.put("name", name);
        chatMap.put("message", message);
        chatMap.put("timestamp", String.valueOf(timestamp));

        redisTemplate.opsForHash().putAll(messageKey, chatMap);
        redisTemplate.opsForZSet().add(messagesIndexKey, chatId, timestamp);

        return chat;
    }

    public List<Chat> getChats(String roomId, int limit, int offset) {

        String messagesIndexKey = "room:" + roomId + ":messages";

        Set<String> chatIds = redisTemplate.opsForZSet()
                .reverseRange(messagesIndexKey, offset, offset + limit - 1);

        if (chatIds == null || chatIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Chat> chats = new ArrayList<>();

        for (String chatId : chatIds) {

            String messageKey = "room:" + roomId + ":message:" + chatId;

            Map<Object, Object> data = redisTemplate.opsForHash().entries(messageKey);
            if (data.isEmpty()) continue;

            Chat chat = new Chat();
            chat.setId((String) data.get("id"));
            chat.setUserId((String) data.get("userId"));
            chat.setRoomId((String) data.get("roomId"));
            chat.setName((String) data.get("name"));
            chat.setMessage((String) data.get("message"));
            chat.setTimestamp(Long.parseLong((String) data.get("timestamp")));

            chat.setUpvotes(getUpvoteCount(roomId, chatId));

            chats.add(chat);
        }

        return chats;
    }

    /* =====================================================
       UPVOTES (ATOMIC & DISTRIBUTED SAFE)
    ===================================================== */

    public long upvote(String userId, String roomId, String chatId) {

        String upvoteKey = "room:" + roomId + ":message:" + chatId + ":upvotes";

        redisTemplate.opsForSet().add(upvoteKey, userId);

        return getUpvoteCount(roomId, chatId);
    }

    public long getUpvoteCount(String roomId, String chatId) {

        String upvoteKey = "room:" + roomId + ":message:" + chatId + ":upvotes";

        Long size = redisTemplate.opsForSet().size(upvoteKey);

        return size != null ? size : 0;
    }

    public void addMembership(String roomId, String userId) {
        String membershipKey = "room:" + roomId + ":members";
        String revMembershipKey = "user:" + userId + ":rooms";
        redisTemplate.opsForSet().add(membershipKey, userId);
        redisTemplate.opsForSet().add(revMembershipKey, roomId);
    }

    public void removeMembership(String roomId, String userId) {
        String membershipKey = "room:" + roomId + ":members";

        redisTemplate.opsForSet().remove(membershipKey, userId);

    }

    private void removeReverseMembership(String roomId, String userId) {
        String revMembershipKey = "user:" + userId + ":rooms";
        redisTemplate.opsForSet().remove(revMembershipKey, roomId);
    }


    public boolean isMember(String roomId, String userId) {
        String membershipKey = "room:" + roomId + ":members";
        return redisTemplate.opsForSet().isMember(membershipKey, userId);
    }

    public Set<String> getMembers(String roomId) {
        String membershipKey = "room:" + roomId + ":members";
        Set<String> members =
                redisTemplate.opsForSet().members(membershipKey);

        return members != null ? members : Collections.emptySet();
    }


    public long getMemberCount(String roomId) {
        String membershipKey = "room:" + roomId + ":members";

        Long size = redisTemplate.opsForSet().size(membershipKey);

        return size != null ? size : 0;
    }
    public Set<String> getUserRooms(String userId) {

        String key = "user:" + userId + ":rooms";

        Set<String> rooms =
                redisTemplate.opsForSet().members(key);

        return rooms != null
                ? rooms
                : Collections.emptySet();
    }


    public void markUserOnline(String userId) {
        redisTemplate.opsForValue()
                .set("online:user:" + userId, "true");
    }

    public void markUserOffline(String userId) {
        redisTemplate.delete("online:user:" + userId);
    }

    public boolean isUserOnline(String userId) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey("online:user:" + userId)
        );
    }
}