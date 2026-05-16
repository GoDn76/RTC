package org.godn.rc.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String appName;

    public EmailServiceImpl(JavaMailSender mailSender,
                            @Value("${spring.mail.from}") String fromEmail, // Inject 'from' email address from application properties
                            @Value("${name.application}") String appName // Inject application name from application properties
    ) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.appName = appName;
    }

    /**
     * Sends an email with a 6-digit verification OTP.
     */
    @Override
    @Async
    public void sendVerificationEmail(String to, String token) {
        String subject = appName + " - Email Verification";

        String messageText = "Thank you for registering for "+appName+".\n\n" +
                "Your email verification code is: " + token + "\n\n" +
                "This code will expire in 15 minutes.";

        sendSimpleEmail(to, subject, messageText);
    }

    /**
     * Sends an email with a 6-digit password reset OTP.
     */
    @Override
    @Async
    public void sendPasswordResetEmail(String to, String token) {
        String subject = appName + " - Password Reset Request";

        String messageText = "You have requested to reset your password.\n\n" +
                "Your password reset code is: " + token + "\n\n" +
                "This code will expire in 15 minutes.\n" +
                "If you did not request this, please ignore this email.";

        sendSimpleEmail(to, subject, messageText);
    }

    /**
     * Helper method to create and send a simple text email.
     */
    private void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage email = new SimpleMailMessage();

        email.setFrom(fromEmail);
        email.setTo(to);
        email.setSubject(subject);
        email.setText(text);
        mailSender.send(email);
    }
}