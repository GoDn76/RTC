package org.godn.rc.auth.repository;

import org.godn.rc.auth.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    @Query("""
    SELECT u
    FROM User u
    WHERE u.email <> :currentUserEmail
      AND LOWER(u.email) LIKE LOWER(CONCAT(:query, '%'))
""")
    List<User> searchUsers(
            String currentUserEmail,
            String query,
            Pageable pageable
    );
}