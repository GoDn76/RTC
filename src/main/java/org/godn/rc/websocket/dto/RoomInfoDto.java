package org.godn.rc.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.godn.rc.entity.ChatRoomType;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomInfoDto {
    String roomId;

    ChatRoomType roomType;

    String displayName;

    String targetUserId;
    String displayAvatar;
}
