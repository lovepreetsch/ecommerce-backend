package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.*;
import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.common.event.NotificationEvent;
import com.ecommerce.common.event.RabbitMQConstants;
import com.ecommerce.common.event.UserRegisteredEvent;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.DuplicateResourceException;
import com.ecommerce.common.exception.UnauthorizedException;
import com.ecommerce.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String OTP_PREFIX = "otp:";
    private static final long OTP_EXPIRY_MINUTES = 5;

    /**
     * Register a new user account.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User.Role role = User.Role.ROLE_CUSTOMER;
        if (request.getRole() != null) {
            try {
                role = User.Role.valueOf("ROLE_" + request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid role: " + request.getRole());
            }
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(role)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {} with role {}", user.getEmail(), user.getRole());

        // Publish UserRegisteredEvent
        publishUserRegisteredEvent(user);

        // Generate tokens
        return generateAuthResponse(user);
    }

    /**
     * Authenticate a user and return tokens.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }

        log.info("User logged in: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    /**
     * Refresh the access token using a valid refresh token.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.isExpired()) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new UnauthorizedException("Refresh token has expired. Please login again.");
        }

        User user = refreshToken.getUser();
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name()
        );

        log.info("Access token refreshed for user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    /**
     * Logout: blacklist the access token and revoke all refresh tokens.
     */
    @Transactional
    public void logout(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            jwtTokenProvider.blacklistToken(token);

            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                refreshTokenRepository.revokeAllByUser(user);
                log.info("User logged out: {}", user.getEmail());
            }
        }
    }

    /**
     * Send OTP to email for password reset.
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("No account found with this email"));

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Store OTP in Redis with 5-minute TTL
        redisTemplate.opsForValue().set(
                OTP_PREFIX + request.getEmail(),
                otp,
                OTP_EXPIRY_MINUTES,
                TimeUnit.MINUTES
        );

        // Send OTP notification via RabbitMQ
        NotificationEvent event = NotificationEvent.builder()
                .userId(user.getId())
                .recipientEmail(user.getEmail())
                .type("PASSWORD_RESET_OTP")
                .channel("EMAIL")
                .subject("Password Reset OTP")
                .templateName("password-reset-otp")
                .templateVariables(Map.of(
                        "firstName", user.getFirstName(),
                        "otp", otp
                ))
                .createdAt(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConstants.NOTIFICATION_EXCHANGE,
                RabbitMQConstants.NOTIFICATION_SEND_KEY,
                event
        );

        log.info("OTP sent to email: {}", request.getEmail());
    }

    /**
     * Reset password using OTP verification.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String storedOtp = redisTemplate.opsForValue().get(OTP_PREFIX + request.getEmail());

        if (storedOtp == null) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        if (!storedOtp.equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("No account found with this email"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Delete OTP from Redis
        redisTemplate.delete(OTP_PREFIX + request.getEmail());

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllByUser(user);

        log.info("Password reset successful for: {}", request.getEmail());
    }

    // ===== Private Helpers =====

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name()
        );

        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getId());

        // Store refresh token in DB
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenStr)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    private void publishUserRegisteredEvent(User user) {
        try {
            UserRegisteredEvent event = UserRegisteredEvent.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .role(user.getRole().name())
                    .registeredAt(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.USER_EXCHANGE,
                    RabbitMQConstants.USER_REGISTERED_KEY,
                    event
            );
            log.info("Published UserRegisteredEvent for: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish UserRegisteredEvent: {}", e.getMessage());
        }
    }
}
