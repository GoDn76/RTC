package org.godn.rc.config;

import org.godn.rc.auth.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

@Configuration
public class AppConfig {

    private final UserRepository userRepository;

    public AppConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * This is used by AuthenticationManager during LOGIN to check if the user exists
     * and if the password matches.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            org.godn.rc.auth.model.User appUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

            // Map our custom User entity to Spring Security's UserDetails
            return new org.springframework.security.core.userdetails.User(
                    appUser.getEmail(),
                    appUser.getPassword(),
                    appUser.getEmailVerified(), // Enabled (only if email verified)
                    true, // Account Non Expired
                    true, // Credentials Non Expired
                    true, // Account Non-Locked
                    Collections.emptyList() // Authorities/Roles
            );
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}