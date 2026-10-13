package com.example.orderlab.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String ERROR_ATTRIBUTE = "security.error";
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) { this.jwtService = jwtService; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        System.out.println("[SECURITY] JWT received");
        String token = header.substring(7);
        try {
            Claims claims = jwtService.parseAccessToken(token);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            var authentication = new UsernamePasswordAuthenticationToken(
                    username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            System.out.printf("[SECURITY] JWT valid username=%s role=%s%n", username, role);
            System.out.printf("[SECURITY] Authentication set username=%s%n", username);
        } catch (ExpiredJwtException expired) {
            System.out.println("[SECURITY] JWT expired");
            request.setAttribute(ERROR_ATTRIBUTE, "Access token expired");
        } catch (Exception invalid) {
            System.out.println("[SECURITY] JWT invalid");
            request.setAttribute(ERROR_ATTRIBUTE, "Invalid access token");
        }
        chain.doFilter(request, response);
    }
}
