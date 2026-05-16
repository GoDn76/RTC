package org.godn.rc.redis.pubsub;

import lombok.extern.slf4j.Slf4j;
import org.godn.rc.websocket.handlers.ChatWebSocketHandler; // Make sure this matches your package!
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;


@Slf4j
@Service
public class RedisSubscriber {

    @Autowired
    @Lazy
    private ChatWebSocketHandler chatHandler;
    public void onMessage(String message, String channel) {
        try {
            chatHandler.broadcastToLocalUsers(new TextMessage(message));
        } catch (Exception e) {
            log.error("Failed to broadcast message: {}", e.getMessage());
        }
    }
}