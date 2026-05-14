package org.godn.rc.auth.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private String name;
    private String email;
    private boolean emailVerified;
    // You could add other fields here later,
    // like profilePictureUrl or bio
}
