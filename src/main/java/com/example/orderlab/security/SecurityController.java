package com.example.orderlab.security;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/security")
public class SecurityController {
    private final SecurityService service;

    public SecurityController(SecurityService service) { this.service = service; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public SecurityDtos.MessageResponse register(@Valid @RequestBody SecurityDtos.RegisterRequest request) {
        service.register(request);
        return new SecurityDtos.MessageResponse("REGISTERED", "User registered");
    }

    @PostMapping("/login")
    public SecurityDtos.TokenResponse login(@Valid @RequestBody SecurityDtos.LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/refresh")
    public SecurityDtos.TokenResponse refresh(@Valid @RequestBody SecurityDtos.RefreshRequest request) {
        return service.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public SecurityDtos.MessageResponse logout(@Valid @RequestBody SecurityDtos.LogoutRequest request) {
        service.logout(request.refreshToken());
        return new SecurityDtos.MessageResponse("REVOKED", "Refresh token revoked");
    }

    @GetMapping("/me")
    public SecurityDtos.MeResponse me(Authentication authentication) {
        return new SecurityDtos.MeResponse(authentication.getName(), role(authentication), "JWT");
    }

    @GetMapping("/user-area")
    public SecurityDtos.MessageResponse userArea(Authentication authentication) {
        return new SecurityDtos.MessageResponse("USER_AREA", "Hello " + authentication.getName());
    }

    @GetMapping("/admin-area")
    public SecurityDtos.MessageResponse adminArea(Authentication authentication) {
        return new SecurityDtos.MessageResponse("ADMIN_AREA", "Hello admin " + authentication.getName());
    }

    private String role(Authentication authentication) {
        return authentication.getAuthorities().stream().findFirst().map(a -> a.getAuthority().replace("ROLE_", "")).orElse("UNKNOWN");
    }
}
