package org.godn.rc.websocket.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.godn.rc.entity.ChatRoomType;
import org.hibernate.validator.constraints.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InternalPayload {
    private String roomName;

    private IncomingAction action;

    private String name;

    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "Invalid User ID format. Must be a valid UUID.")
    private String userId;

    @Pattern(regexp = "^$|^[A-Z0-9]{3}-[A-Z0-9]{3}$",
            message = "Invalid Room ID format. Must be like 4W1-RM2.")
    private String roomId;

    @UUID
    private String targetUserId;
    private ChatRoomType roomType;
    private String message;
    private String chatId;

    private Integer limit;
    private Integer offset;
}
