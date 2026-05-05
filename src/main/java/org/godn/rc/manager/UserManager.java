package org.godn.rc.manager;

import lombok.extern.slf4j.Slf4j;
import org.godn.rc.store.RedisChatStore;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class UserManager {

    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();
    private final RedisChatStore redisChatStore;

    public UserManager(RedisChatStore redisChatStore) {
        this.redisChatStore = redisChatStore;
    }

    public void addUser(String roomId, String userId, String name, WebSocketSession session) {
        if (roomId == null || roomId.trim().isEmpty() || !redisChatStore.roomExists(roomId)) {
            throw new InvalidParameterException("Room ID is not Valid, Empty or does not Exists.");
        }

        roomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(session);
        sessionToRoom.put(session.getId(), roomId);
        redisChatStore.incrementMemberCount(roomId);

        log.info("User {} ({}) added to room: {}", name, userId, roomId);
    }

    public Set<WebSocketSession> getSessionsForRoom(String roomId) {
        return roomSessions.getOrDefault(roomId, Collections.emptySet());
    }

    public void removeUser(WebSocketSession session) {
        String sessionId = session.getId();
        String roomId = sessionToRoom.remove(sessionId);

        if (roomId != null) {
            Set<WebSocketSession> sessions = roomSessions.get(roomId);
            if (sessions != null) {
                sessions.remove(session);
                redisChatStore.decrementMemberCount(roomId);

                if (redisChatStore.getMemberCount(roomId) <= 0) {
                    roomSessions.remove(roomId);
                    try{
                        redisChatStore.deleteRoom(roomId);
                    } catch (Exception e){
                        log.error("Failed to delete room {} from Redis: {}", roomId, e.getMessage());
                    }
                }
            }

            log.info("Session {} removed from room: {}", sessionId, roomId);
        }
    }

    public boolean isUserInRoom(String id) {
        return sessionToRoom.containsKey(id);
    }
}