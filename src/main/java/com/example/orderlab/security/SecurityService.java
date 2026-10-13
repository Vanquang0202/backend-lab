package com.example.orderlab.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class SecurityService {
    private final AppUserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Duration refreshTokenTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public SecurityService(AppUserRepository users, RefreshTokenRepository refreshTokens,
                           PasswordEncoder passwordEncoder, JwtService jwtService,
                           @Value("${app.security.refresh-token-ttl:7d}") Duration refreshTokenTtl) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Transactional
    public void register(SecurityDtos.RegisterRequest request) {
        String username = request.username().trim();
        if (users.existsByUsername(username)) {
            throw new SecurityApiException(HttpStatus.CONFLICT, "USERNAME_EXISTS", "Username already exists");
        }
        users.save(new AppUser(username, passwordEncoder.encode(request.password()), AppRole.USER));
        System.out.printf("[SECURITY] Registered username=%s role=USER%n", username);
    }

    @Transactional
    public SecurityDtos.TokenResponse login(SecurityDtos.LoginRequest request) {
        AppUser user = users.findByUsername(request.username().trim())
                .orElseThrow(() -> unauthorized("Invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw unauthorized("Invalid username or password");
        }
        System.out.printf("[SECURITY] Login success username=%s role=%s%n", user.getUsername(), user.getRole());
        return issueTokens(user);
    }

    @Transactional
    public SecurityDtos.TokenResponse refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokens.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> unauthorized("Invalid refresh token"));
        if (!stored.isUsable(Instant.now())) {
            throw unauthorized("Refresh token expired or revoked");
        }
        stored.revoke();
        AppUser user = users.findByUsername(stored.getUsername())
                .orElseThrow(() -> unauthorized("User no longer exists"));
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokens.findByTokenHash(hash(rawRefreshToken)).ifPresent(RefreshToken::revoke);
    }

    private SecurityDtos.TokenResponse issueTokens(AppUser user) {
        String accessToken = jwtService.createAccessToken(user);
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant refreshExpiresAt = Instant.now().plus(refreshTokenTtl);
        refreshTokens.save(new RefreshToken(hash(rawRefreshToken), user.getUsername(), refreshExpiresAt));
        return new SecurityDtos.TokenResponse("Bearer", accessToken, rawRefreshToken,
                jwtService.getAccessTokenTtl().toSeconds(), Instant.now().plus(jwtService.getAccessTokenTtl()));
    }

    private SecurityApiException unauthorized(String message) {
        return new SecurityApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
