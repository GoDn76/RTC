package org.godn.rc.auth.service;

import com.google.api.client.json.webtoken.JsonWebSignature;
import lombok.extern.slf4j.Slf4j;
import org.godn.rc.auth.exception.BadRequestException;
import org.godn.rc.auth.exception.ResourceNotFoundException;
import org.godn.rc.auth.exception.UnauthorizedException;
import org.godn.rc.auth.model.*;
import org.godn.rc.auth.payload.*;
import org.godn.rc.auth.repository.PasswordResetTokenRepository;
import org.godn.rc.auth.repository.UserRepository;
import org.godn.rc.auth.repository.VerificationTokenRepository;
import org.godn.rc.auth.security.GoogleTokenVerifier;
import org.godn.rc.auth.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final SecureRandom secureRandom = new SecureRandom();
    private static final int OTP_EXPIRY_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            VerificationTokenRepository verificationTokenRepository,
            EmailService emailService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            GoogleTokenVerifier googleTokenVerifier
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailService = emailService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    /**
     * Helper: Generates OTP and Updates/Creates the token in DB.
     */
    private void createAndSendVerificationOtp(User user) {
        String otp = generateOtp();

        verificationTokenRepository.save(otp, user.getId().toString(), 15L);

        // NOTE: Ensure sendVerificationEmail is annotated with @Async in EmailService
        emailService.sendVerificationEmail(user.getEmail(), otp);
    }

    /**
     * Helper: Password Reset OTP
     */
    private void createAndSendPasswordResetOtp(User user) {
        String otp = generateOtp();

        passwordResetTokenRepository.save(otp, user.getId().toString(), 15L);

        emailService.sendPasswordResetEmail(user.getEmail(), otp);
    }

    private String generateOtp() {
        // Generate 6-digit OTP using secure random
        return String.valueOf(secureRandom.nextInt(900000) + 100000);
    }

    @Override
    @Transactional
    public ApiResponseDto registerUser(RegisterDto registerDto) {
        Optional<User> existingUserOpt = userRepository.findByEmail(registerDto.getEmail());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            if (existingUser.getProvider() == AuthProvider.GOOGLE) {
                throw new BadRequestException("This email is registered with Google. Please use Google Login.");
            }

            if (!existingUser.getEmailVerified()) {
                // Overwrite OLD details with NEW details
                existingUser.setName(registerDto.getName());
                existingUser.setPassword(passwordEncoder.encode(registerDto.getPassword()));
                existingUser.setProvider(AuthProvider.LOCAL); // Ensure provider is reset to LOCAL if needed
                userRepository.save(existingUser);
                createAndSendVerificationOtp(existingUser);

                return new ApiResponseDto(true, "A new verification code has been sent to your email.");
            }

            // 3. Handle Verified Account Conflict
            throw new BadRequestException("Email is already in use.");
        }

        // 4. Handle New User Registration
        User user = new User();
        user.setName(registerDto.getName());
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setProvider(AuthProvider.LOCAL);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        createAndSendVerificationOtp(savedUser);

        return new ApiResponseDto(true, "User registered successfully. Please check your email for the verification code.");
    }


    @Override
    @Transactional
    public ApiResponseDto verifyEmail(OtpVerificationDto verificationDto) {
        User user = userRepository.findByEmail(verificationDto.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", verificationDto.getEmail()));

        String token = verificationTokenRepository.getToken(user.getId().toString());

        if (token == null || !token.equals(verificationDto.getOtp())) {
            throw new BadRequestException("Invalid OTP.");
        }

        user.setEmailVerified(true);
        userRepository.save(user);
        verificationTokenRepository.delete(user.getId().toString());
        return new ApiResponseDto(true, "Email verified successfully.");
    }

    @Override
    public AuthResponseDto loginUser(LoginDto loginDto) {
        User user = userRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new BadRequestException("This account is registered with Google. Please use Google Login.");
        }

        if (!user.getEmailVerified()) {
            throw new UnauthorizedException("Please verify your email before logging in.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtTokenProvider.generateToken(user);
            return new AuthResponseDto(jwt);

        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid email or password.");
        }
    }

    @Override
    @Transactional
    public AuthResponseDto loginWithGoogle(GoogleLoginDto googleLoginDto) {

        JsonWebSignature idToken =
                googleTokenVerifier.verify(googleLoginDto.getIdToken());

        if (idToken == null) {
            throw new RuntimeException("Invalid Google token");
        }

        JsonWebSignature.Payload payload = idToken.getPayload();

        String email = payload.get("email").toString();
        String name = payload.get("name").toString();
        String googleId = payload.getSubject();
        Boolean emailVerified = (Boolean) payload.get("email_verified");

        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new RuntimeException("Google email not verified");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {

            user = userOptional.get();

            if (user.getProviderId() != null &&
                    !user.getProviderId().equals(googleId)) {
                throw new RuntimeException("Google account mismatch");
            }

            user.setProviderId(googleId);
            user.setEmailVerified(true);

        } else {

            user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setProvider(AuthProvider.GOOGLE);
            user.setProviderId(googleId);
            user.setEmailVerified(true);

            user = userRepository.save(user);
        }

        String jwt = jwtTokenProvider.generateToken(user);
        return new AuthResponseDto(jwt);
    }

    @Override
    public ApiResponseDto requestPasswordReset(EmailDto emailDto) {
        Optional<User> userOptional = userRepository.findByEmail(emailDto.getEmail());

        if (userOptional.isEmpty()) {
            return new ApiResponseDto(true, "If an account with this email exists, a reset code has been sent.");
        }

        User user = userOptional.get();

        createAndSendPasswordResetOtp(user);
        return new ApiResponseDto(true, "If an account with this email exists, a reset code has been sent.");
    }

    @Override
    @Transactional
    public ApiResponseDto resetPassword(ResetPasswordDto resetDto) {
        User user = userRepository.findByEmail(resetDto.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid request."));


        String token = passwordResetTokenRepository.getToken(user.getId().toString());
        if (token == null || !token.equals(resetDto.getOtp())) {
            throw new BadRequestException("Invalid OTP.");
        }

        user.setPassword(passwordEncoder.encode(resetDto.getNewPassword()));
        userRepository.save(user);


        passwordResetTokenRepository.delete(user.getId().toString());
        return new ApiResponseDto(true, "Password reset successfully.");
    }

}