package org.godn.rc.auth.security;

import com.google.api.client.json.webtoken.JsonWebSignature;
import com.google.auth.oauth2.TokenVerifier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class GoogleTokenVerifier {
    private final TokenVerifier verifier;
    public GoogleTokenVerifier(
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId
    ) {
            this.verifier = TokenVerifier.newBuilder()
                    .setAudience(clientId)
                    .build();
        }

        public JsonWebSignature verify(String idTokenString) {
            try {
                return verifier.verify(idTokenString);
            } catch (Exception e) {
                throw new RuntimeException("Invalid Google token", e);
            }
        }
}
