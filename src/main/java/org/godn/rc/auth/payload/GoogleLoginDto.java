package org.godn.rc.auth.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginDto {
    @NotBlank(message = "Google token is required")
    private String idToken;
}
