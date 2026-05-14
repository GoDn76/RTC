package org.godn.rc.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class    VerificationToken {

    // Use a UUID for the primary key, just like in our User model
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // The unique, random token string
    @Column(nullable = false, unique = true)
    private String token;

    // Link this token to a User
    // We use FetchType.LAZY to improve performance
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // The timestamp when this token will expire
    @Column(nullable = false)
    private Instant expiryDate;

    /**
     * Helper constructor to easily create a new token for a user.
     * @param user The user who is registering.
     * @param token The unique token string.
     * @param expiryDurationInMinutes How long the token should be valid.
     */
    public VerificationToken(User user, String token, long expiryDurationInMinutes) {
        this.user = user;
        this.token = token;
        this.expiryDate = Instant.now().plusSeconds(expiryDurationInMinutes * 60);
    }
}
