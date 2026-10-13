package com.example.orderlab.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class SecurityDtos {
    private SecurityDtos() {}

    public record RegisterRequest(@NotBlank @Size(min = 3, max = 100) String username,
                                  @NotBlank @Size(min = 6, max = 200) String password) {}
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record LogoutRequest(@NotBlank String refreshToken) {}
    public record TokenResponse(String tokenType, String accessToken, String refreshToken,
                                long accessTokenExpiresInSeconds, Instant accessTokenExpiresAt) {}
    public record MeResponse(String username, String role, String authenticationType) {}
    public record MessageResponse(String status, String message) {}
}
