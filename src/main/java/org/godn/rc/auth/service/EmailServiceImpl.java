package org.godn.rc.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private static final String BREVO_API =
            "https://api.brevo.com/v3/smtp/email";

    private final WebClient webClient;
    private final String apiKey;
    private final String fromEmail;
    private final String appName;

    public EmailServiceImpl(
            WebClient.Builder builder,
            @Value("${brevo.api-key}") String apiKey,
            @Value("${brevo.from}") String fromEmail,
            @Value("${name.application}") String appName
    ) {
        this.webClient = builder.build();
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.appName = appName;
    }

    @Override
    @Async
    public void sendVerificationEmail(String to, String token) {

        String subject = "Verify your email • " + appName;

        String html = loadTemplate("templates/verification-email.html")
                .replace("{{APP_NAME}}", appName)
                .replace("{{OTP}}", token)
                .replace("{{EXPIRY}}", "15 minutes")
                .replace("{{YEAR}}", String.valueOf(Year.now().getValue()));

        sendEmail(to, subject, html);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String token) {

        String subject = "Password Reset Request • " + appName;

        String html = loadTemplate("templates/reset-password.html")
                .replace("{{APP_NAME}}", appName)
                .replace("{{OTP}}", token)
                .replace("{{EXPIRY}}", "15 minutes")
                .replace("{{YEAR}}", String.valueOf(Year.now().getValue()));

        sendEmail(to, subject, html);
    }

    private void sendEmail(String to, String subject, String text) {

        Map<String, Object> payload = Map.of(
                "sender", Map.of(
                        "name", appName,
                        "email", fromEmail
                ),
                "to", List.of(
                        Map.of("email", to)
                ),
                "subject", subject,
                "htmlContent", text
        );
        try {

            String response = webClient.post()
                    .uri(BREVO_API)
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("Brevo Response: " + response);

        } catch (Exception e) {
            log.error("Error while sending email", e);
        }
    }
    private String loadTemplate(String path) {

        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {

            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {
            throw new RuntimeException("Failed to load email template: " + path, e);
        }
    }
}