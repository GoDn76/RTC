package org.godn.rc.websocket.manager;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.godn.rc.redis.store.RedisChatStore;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class UserManager {

    // roomId -> (userId -> sessions)
    private final Map<String, Map<String, Set<WebSocketSession>>> roomUserSessions =
            new ConcurrentHashMap<>();

    // sessionId -> metadata
    private final Map<String, SessionMeta> sessionIndex =
            new ConcurrentHashMap<>();

    private final RedisChatStore redisChatStore;

    public UserManager(RedisChatStore redisChatStore) {
        this.redisChatStore = redisChatStore;
    }

    public void addUser(String roomId,
                        String userId,
                        String name,
                        WebSocketSession session) {

        if (roomId == null || roomId.isBlank() || !redisChatStore.roomExists(roomId)) {
            throw new InvalidParameterException("Invalid room ID.");
        }

        roomUserSessions.putIfAbsent(roomId, new ConcurrentHashMap<>());

        Map<String, Set<WebSocketSession>> userSessions =
                roomUserSessions.get(roomId);

        userSessions.putIfAbsent(userId, ConcurrentHashMap.newKeySet());

        Set<WebSocketSession> sessions = userSessions.get(userId);

        boolean firstSessionForUser = sessions.isEmpty();
        sessions.add(session);

        sessionIndex.put(session.getId(), new SessionMeta(roomId, userId));

        // 🔥 Increment member count ONLY if this is first session for that user
        if (firstSessionForUser) {
            redisChatStore.incrementMemberCount(roomId);
            log.info("User {} ({}) joined room {}", name, userId, roomId);
        } else {
            log.info("User {} ({}) opened additional session in room {}", name, userId, roomId);
        }
    }

    public Set<WebSocketSession> getSessionsForRoom(String roomId) {

        Map<String, Set<WebSocketSession>> userMap =
                roomUserSessions.getOrDefault(roomId, Collections.emptyMap());

        Set<WebSocketSession> allSessions =
                ConcurrentHashMap.newKeySet();

        userMap.values().forEach(allSessions::addAll);

        return allSessions;
    }

    public void removeUser(WebSocketSession session) {

        String sessionId = session.getId();
        SessionMeta meta = sessionIndex.remove(sessionId);

        if (meta == null) return;

        String roomId = meta.roomId;
        String userId = meta.userId;

        Map<String, Set<WebSocketSession>> userMap =
                roomUserSessions.get(roomId);

        if (userMap == null) return;

        Set<WebSocketSession> sessions = userMap.get(userId);

        if (sessions != null) {
            sessions.remove(session);

            // 🔥 If no more sessions for that user → decrement member count
            if (sessions.isEmpty()) {
                userMap.remove(userId);
                redisChatStore.decrementMemberCount(roomId);

                log.info("User {} left room {}", userId, roomId);
            }
        }

        // Cleanup room if empty
        if (userMap.isEmpty()) {
            roomUserSessions.remove(roomId);

            try {
                redisChatStore.deleteRoom(roomId);
            } catch (Exception e) {
                log.error("Failed deleting room {}: {}", roomId, e.getMessage());
            }
        }
    }

    public boolean isUserInRoom(String roomId, String userId) {

        Map<String, Set<WebSocketSession>> userMap =
                roomUserSessions.get(roomId);

        return userMap != null && userMap.containsKey(userId);
    }

    @PreDestroy
    public void cleanupOnShutdown() {
        roomUserSessions.forEach((roomId, userMap) -> {
            int userCount = userMap.size();
            for (int i = 0; i < userCount; i++) {
                redisChatStore.decrementMemberCount(roomId);
            }
        });

        roomUserSessions.clear();
        sessionIndex.clear();
    }

    private static class SessionMeta {
        String roomId;
        String userId;

        SessionMeta(String roomId, String userId) {
            this.roomId = roomId;
            this.userId = userId;
        }
    }
}