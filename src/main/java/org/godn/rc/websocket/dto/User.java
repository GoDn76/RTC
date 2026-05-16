package org.godn.rc.websocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.socket.WebSocketSession;

import java.util.Objects;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private UUID id;
    private String name;
    // CRITICAL: Prevents Spring from crashing if you send this object over a WebSocket
    @JsonIgnore
    private WebSocketSession session;

    // We override equals() and hashCode() so that our Set perfectly prevents duplicates
    // based on the User ID, not the memory address!
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User chatUser = (User) o;
        return Objects.equals(id, chatUser.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
