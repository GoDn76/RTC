package org.godn.rc.websocket.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncomingPayload {
    private IncomingAction action;
    private String roomName;
    @Pattern(regexp = "^$|^[A-Z0-9]{3}-[A-Z0-9]{3}$",
            message = "Invalid Room ID format. Must be like 4W1-RM2.")
    private String roomId;

    @UUID
    private String targetUserId;
    private String message;
    private String chatId;

    private Integer limit;
    private Integer offset;
}
