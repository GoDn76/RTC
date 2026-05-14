package org.godn.rc.auth.controller;

import jakarta.validation.Valid;
import org.godn.rc.auth.payload.*;
import org.godn.rc.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user.
     * Returns 201 CREATED on success.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto> registerUser(@Valid @RequestBody RegisterDto registerDto) {
        ApiResponseDto response = authService.registerUser(registerDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Verify Email OTP.
     * Returns 200 OK on success.
     */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponseDto> verifyEmail(@Valid @RequestBody OtpVerificationDto verificationDto) {
        return ResponseEntity.ok(authService.verifyEmail(verificationDto));
    }

    /**
     * Login with Email/Password.
     * Returns 200 OK with JWT Token on success.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> loginUser(@Valid @RequestBody LoginDto loginDto) {
        return ResponseEntity.ok(authService.loginUser(loginDto));
    }

    /**
     * Login with Google.
     * Returns 200 OK with JWT Token on success.
     */
    @PostMapping("/login/google")
    public ResponseEntity<AuthResponseDto> loginWithGoogle(@Valid @RequestBody GoogleLoginDto googleLoginDto) {
        return ResponseEntity.ok(authService.loginWithGoogle(googleLoginDto));
    }

    /**
     * Request Password Reset OTP.
     * Returns 200 OK.
     */
    @PostMapping("/request-password-reset")
    public ResponseEntity<ApiResponseDto> requestPasswordReset(@Valid @RequestBody EmailDto emailDto) {
        return ResponseEntity.ok(authService.requestPasswordReset(emailDto));
    }

    /**
     * Reset Password using OTP.
     * Returns 200 OK.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDto> resetPassword(@Valid @RequestBody ResetPasswordDto resetPasswordDto) {
        return ResponseEntity.ok(authService.resetPassword(resetPasswordDto));
    }
}