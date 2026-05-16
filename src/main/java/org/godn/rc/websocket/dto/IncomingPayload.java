package org.godn.rc.websocket.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncomingPayload {
    private IncomingAction action;

    @Pattern(regexp = "^$|^[A-Z0-9]{3}-[A-Z0-9]{3}$",
            message = "Invalid Room ID format. Must be like 4W1-RM2.")
    private String roomId;

    private String message;
    private String chatId;

    private Integer limit;
    private Integer offset;
}
