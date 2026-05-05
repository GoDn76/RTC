package org.godn.rc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncomingPayload {
    private IncomingAction action;

    @NotBlank(message = "Name is required.")
    @Size(max = 100, message = "Name cannot be longer than 100 characters.")
    private String name;

    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "Invalid User ID format. Must be a valid UUID.")
    private String userId;

    @Pattern(regexp = "^$|^[A-Z0-9]{3}-[A-Z0-9]{3}$",
            message = "Invalid Room ID format. Must be like 4W1-RM2.")
    private String roomId;

    private String message;
    private String chatId;

    private Integer limit;
    private Integer offset;
}
