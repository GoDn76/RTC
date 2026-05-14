package org.godn.rc.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.godn.rc.auth.model.User;
import org.godn.rc.auth.payload.UpdateProfileDto;
import org.godn.rc.auth.payload.UserProfileDto;
import org.godn.rc.auth.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Get the current authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getUserProfile(Authentication authentication) {

        String email = authentication.getName();

        User user = profileService.getUserByEmail(email);

        UUID userId = user.getId();

        UserProfileDto profile = profileService.getUserProfile(userId.toString());
        return ResponseEntity.ok(profile);
    }

    /**
     * Update the current authenticated user's profile.
     */
    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileDto updateProfileDto
    ) {

        String email = authentication.getName();

        User user = profileService.getUserByEmail(email);
        UUID userId = user.getId();

        UserProfileDto updatedProfile =
                profileService.updateProfile(userId.toString(), updateProfileDto);

        return ResponseEntity.ok(updatedProfile);
    }
}