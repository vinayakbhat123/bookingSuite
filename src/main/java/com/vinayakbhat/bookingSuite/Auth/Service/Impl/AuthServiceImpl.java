package com.vinayakbhat.bookingSuite.Auth.Service.Impl;

import com.vinayakbhat.bookingSuite.Auth.Entity.RefreshToken;
import com.vinayakbhat.bookingSuite.Auth.Entity.User;
import com.vinayakbhat.bookingSuite.Auth.Enum.Role;
import com.vinayakbhat.bookingSuite.Auth.Service.AuthService;
import com.vinayakbhat.bookingSuite.Auth.dto.*;
import com.vinayakbhat.bookingSuite.Auth.repository.RefreshTokenRepository;
import com.vinayakbhat.bookingSuite.Auth.repository.UserRepository;
import com.vinayakbhat.bookingSuite.common.notification.EmailService;
import com.vinayakbhat.bookingSuite.common.security.Exception.ResourceNotFoundException;
import com.vinayakbhat.bookingSuite.common.security.Exception.UnAuthorisedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j

public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;
    private final RefreshTokenRepository refreshTokenRepository; // If stored in DB

    @Autowired
    private AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JWTService jwtService, EmailService emailService, StringRedisTemplate redisTemplate, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.redisTemplate = redisTemplate;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest signupRequest) {
        String normalizedEmail = signupRequest.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResourceNotFoundException("User already exists with email: " + normalizedEmail);
        }
        String hashedPassword = passwordEncoder.encode(signupRequest.password());
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(hashedPassword);
        user.setName(signupRequest.name());
        user.setRoles(Set.of(Role.GUEST));
        User savedUser = userRepository.save(user);

        Map<String, Object> emailVariables = new HashMap<>();
        emailVariables.put("userName", user.getName());
        emailVariables.put("loginUrl", "http://localhost:5173/"); // Update with your actual frontend URL

        // 4. Fire off the email asynchronously
        // Because EmailService has @Async, this returns instantly.
        // The user doesn't have to wait 2-3 seconds for SMTP to finish to see the success screen!
        emailService.sendDynamicHtmlEmail(
                user.getEmail(), "Welcome to BookingSuite!", "account-welcome", // This matches account-welcome.html
                emailVariables
        );

        return new AuthResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail()
        );
    }

    @Override
    @Transactional
    public String[] login(LoginRequest loginRequest) {
        // Authenticate the user; throws AuthenticationException on failure
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password())
        );
        // Retrieve the principal and safely cast it
        User user = (User) authentication.getPrincipal();
        // Generate tokens using standard camelCase naming
        String[] arr = new String[2];
        arr[0] = jwtService.generateAccessToken(user);
        arr[1] = jwtService.generateRefreshToken(user);
        
        // --- REFRESH TOKEN LIMITER (Max 3 per user) ---
        List<RefreshToken> existingTokens = refreshTokenRepository.findByUserOrderByIdAsc(user);
        if (existingTokens.size() >= 3) {
            // Delete the oldest tokens so we never exceed 3 total per user
            int tokensToDelete = existingTokens.size() - 2; // Keep room for the new one
            for (int i = 0; i < tokensToDelete; i++) {
                refreshTokenRepository.delete(existingTokens.get(i));
                log.info("Deleted expired/old refresh token ID: {} for user ID: {}", existingTokens.get(i).getId(), user.getId());
            }
        }
        // 3. Create and Save Refresh Token in PostgreSQL (long-lived, e.g., 7 days)
        RefreshToken refreshToken = RefreshToken.builder()
                .token(arr[1])
                .user(user)
                .expiryDate(Instant.now().plus(Duration.ofDays(30)))
                .build();
        refreshTokenRepository.save(refreshToken);
        log.info("Refresh token generated and saved for user ID: {}", user.getId());
        return arr;
    }

    @Override
    @Transactional
    public String refreshToken(String tokenVal) {
        // 1. Fetch token entity with a single indexed query
        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenVal)
                .orElseThrow(() -> new UnAuthorisedException("Invalid refresh token"));

        // 2. Validate token state (expiration & revocation)
        if ( refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new UnAuthorisedException("Refresh token is expired or revoked");
        }

        // 3. Extract user directly from the relationship (avoiding extra user table lookup if fetched with JOIN)
        User user = refreshToken.getUser();

        // Optional: Implement Token Rotation (revoke current, issue a new refresh token)
        // refreshToken.setRevoked(true);
        // refreshTokenRepository.save(refreshToken);
        // String newRefreshToken = jwtService.generateRefreshToken(user);

        return jwtService.generateAccessToken(user);
    }

    @Override
    @Transactional
    public String logout(HttpServletRequest request, LogoutRequest logoutRequest) {
        // 1. Invalidate Access Token via Redis Blacklist
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            try {
                Date expiration = jwtService.extractExpiration(accessToken);
                long ttlMillis = expiration.getTime() - System.currentTimeMillis();

                if (ttlMillis > 0) {
                    String blacklistKey = "blacklist:token:" + accessToken;
                    redisTemplate.opsForValue().set(blacklistKey, "revoked", Duration.ofMillis(ttlMillis));
                    log.info("Access token successfully blacklisted in Redis.");
                }
            } catch (Exception e) {
                log.warn("Could not parse or blacklist access token during logout: {}", e.getMessage());
            }
        }

        // 2. Delete Refresh Token from PostgreSQL Database
        if (logoutRequest != null && StringUtils.hasText(logoutRequest.refreshToken())) {
            refreshTokenRepository.findByToken(logoutRequest.refreshToken())
                    .ifPresent(token -> {
                        refreshTokenRepository.delete(token);
                        log.info("Refresh token successfully deleted from database.");
                    });
        }
        return "Logged out successfully";
    }


}
