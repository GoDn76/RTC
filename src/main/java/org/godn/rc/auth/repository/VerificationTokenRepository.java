package org.godn.rc.auth.repository;

import org.godn.rc.auth.model.User;
import org.godn.rc.auth.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    // A method to find a token by its string value
    Optional<VerificationToken> findByToken(String token);

    // A method to find a token associated with a specific user
    Optional<VerificationToken> findByUser(User user);
}
