package org.godn.rc.auth.service;

/**
 * Interface for the email sending service.
 */
public interface EmailService {

    /**
     * Sends a pre-formatted verification email to a new user.
     * @param to The recipient's email address.
     * @param token The unique verification token.
     */
    void sendVerificationEmail(String to, String token);

    /**
     * Sends a pre-formatted password reset email to an existing user.
     * @param to The recipient's email address.
     * @param token The unique password reset token.
     */
    void sendPasswordResetEmail(String to, String token); // <-- NEW METHOD
}
