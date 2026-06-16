package org.godn.rc.websocket.manager;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.godn.rc.auth.payload.UserProfileDto;
import org.godn.rc.auth.service.ProfileService;
import org.godn.rc.redis.store.RedisChatStore;
import org.godn.rc.websocket.dto.ChatUser;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

    // userId -> sessions
    private final Map<String, Set<WebSocketSession>> userSessions =
            new ConcurrentHashMap<>();

    private final RedisChatStore redisChatStore;
    private final ProfileService profileService;

    public UserManager(RedisChatStore redisChatStore,  ProfileService profileService) {
        this.redisChatStore = redisChatStore;
        this.profileService = profileService;
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

        userSessions.putIfAbsent(userId, ConcurrentHashMap.newKeySet());
        userSessions.get(userId).add(session);

        sessionIndex.compute(session.getId(), (id, meta) -> {

            if (meta == null) {
                meta = new SessionMeta(userId);
            }

            meta.roomIds.add(roomId);

            return meta;
        });

        // 🔥 Increment member count ONLY if this is first session for that user
        if (firstSessionForUser) {
            redisChatStore.incrementActiveCount(roomId);
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
    public void disconnectSession(WebSocketSession session) {

        SessionMeta meta = sessionIndex.remove(session.getId());

        if (meta == null) return;

        String userId = meta.userId;

        Set<WebSocketSession> sessionsForUser = userSessions.get(userId);

        if (sessionsForUser != null) {

            sessionsForUser.remove(session);

            if (sessionsForUser.isEmpty()) {
                userSessions.remove(userId);
            }
        }

        for (String roomId : meta.roomIds) {

            Map<String, Set<WebSocketSession>> userMap =
                    roomUserSessions.get(roomId);

            if (userMap == null) continue;

            Set<WebSocketSession> sessions = userMap.get(userId);

            if (sessions != null) {

                sessions.remove(session);

                if (sessions.isEmpty()) {

                    userMap.remove(userId);

                    redisChatStore.decrementActiveCount(roomId);

                    log.info("User {} left room {}", userId, roomId);
                }
            }

            if (userMap.isEmpty()) {
                roomUserSessions.remove(roomId);
            }
        }
    }

    public boolean isUserInRoom(String roomId, String userId) {

        Map<String, Set<WebSocketSession>> userMap =
                roomUserSessions.get(roomId);

        return userMap != null && userMap.containsKey(userId);
    }

    public ChatUser getUser (String userId) {
        UserProfileDto user = profileService.getUserProfile(userId);
        if (user == null) return null;
        ChatUser newUser = new ChatUser();
        newUser.setEmail(user.getEmail());
        newUser.setName(user.getName());
        newUser.setId(UUID.fromString(userId));

        return newUser;
    }

    public void markActive(String userId,
                           String name,
                           WebSocketSession session) {

        redisChatStore.markUserOnline(userId);

        for (String roomId : redisChatStore.getUserRooms(userId)) {

            try {
                addUser(roomId, userId, name, session);
            } catch (Exception e) {
                log.error("Failed adding from room {}: {}",
                        roomId,
                        e.getMessage());
            }
        }
    }

    public void markOffline(String userId,
                            WebSocketSession session) {

        redisChatStore.markUserOffline(userId);

        disconnectSession(session);
    }

    public void addUserToRoom(String roomId,
                              String userId) {

        // Already a member? Nothing to do.
        if (redisChatStore.isMember(roomId, userId)) {
            return;
        }

        // Persist membership
        redisChatStore.addMembership(roomId, userId);

        // User offline? Membership is enough.
        if (!isOnline(userId)) {
            return;
        }

        ChatUser user = getUser(userId);

        if (user == null) {
            log.warn("User {} not found while adding to room {}", userId, roomId);
            return;
        }

        // Attach all active sessions
        for (WebSocketSession session : getSessionsForUser(userId)) {

            try {
                addUser(
                        roomId,
                        userId,
                        user.getName(),
                        session
                );
            } catch (Exception e) {
                log.error(
                        "Failed attaching user {} to room {}: {}",
                        userId,
                        roomId,
                        e.getMessage()
                );
            }
        }
    }

    public boolean isOnline (String userId) {
        return redisChatStore.isUserOnline(userId);
    }

    @PreDestroy
    public void cleanupOnShutdown() {
        roomUserSessions.forEach((roomId, userMap) -> {
            int userCount = userMap.size();
            for (int i = 0; i < userCount; i++) {
                redisChatStore.decrementActiveCount(roomId);
            }
        });

        roomUserSessions.clear();
        sessionIndex.clear();
    }

    private static class SessionMeta {
        String userId;
        Set<String> roomIds;

        SessionMeta(String userId) {
            this.userId = userId;
            this.roomIds = ConcurrentHashMap.newKeySet();
        }
    }

    public Set<WebSocketSession> getSessionsForUser(String userId) {

        return userSessions.getOrDefault(
                userId,
                Collections.emptySet()
        );
    }
}