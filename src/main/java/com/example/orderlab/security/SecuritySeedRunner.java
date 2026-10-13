package com.example.orderlab.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SecuritySeedRunner implements ApplicationRunner {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public SecuritySeedRunner(AppUserRepository users, PasswordEncoder passwordEncoder,
                              @Value("${app.security.seed-admin-username:}") String username,
                              @Value("${app.security.seed-admin-password:}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank()) return;
        users.findByUsername(username).ifPresentOrElse(existing -> {
            existing.changeRole(AppRole.ADMIN);
        }, () -> users.save(new AppUser(username, passwordEncoder.encode(password), AppRole.ADMIN)));
        System.out.printf("[SECURITY] Seeded ADMIN username=%s%n", username);
    }
}
