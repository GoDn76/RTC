package org.godn.rc.auth.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileDto {

    @NotBlank(message = "Name cannot be blank")
    private String name;

    // Will add more fields in the future as needed
}