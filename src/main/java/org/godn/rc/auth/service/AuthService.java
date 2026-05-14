package org.godn.rc.auth.service;

import org.godn.rc.auth.payload.*;

public interface AuthService {

    /**
     * Registers a new user.
     * Throws exception if email already exists.
     * @return A success message or DTO (e.g., ApiResponseDto).
     */
    ApiResponseDto registerUser(RegisterDto registerDto);

    /**
     * Verifies email OTP.
     * Throws exception if OTP is invalid/expired.
     * @return Success response.
     */
    ApiResponseDto verifyEmail(OtpVerificationDto verificationDto);

    /**
     * Authenticates user.
     * Throws BadCredentialsException if failed.
     * @return The JWT Token wrapped in a DTO.
     */
    AuthResponseDto loginUser(LoginDto loginDto);

    /**
     * Google OAuth Login.
     * @return The JWT Token wrapped in a DTO.
     */
    AuthResponseDto loginWithGoogle(GoogleLoginDto googleLoginDto);

    /**
     * Generates password reset OTP.
     * Throws exception if email not found.
     * @return Success message.
     */
    ApiResponseDto requestPasswordReset(EmailDto emailDto);

    /**
     * Resets the password.
     * Throws exception if OTP invalid.
     * @return Success message.
     */
    ApiResponseDto resetPassword(ResetPasswordDto resetPasswordDto);
}