package org.godn.rc.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.godn.rc.entity.ChatRoomType;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveRoom {
    private String roomId;
    private ChatRoomType roomType;
    private Set<ChatUser> users = ConcurrentHashMap.newKeySet();
}
