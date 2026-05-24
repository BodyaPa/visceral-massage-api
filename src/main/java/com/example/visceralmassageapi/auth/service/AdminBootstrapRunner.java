package com.example.visceralmassageapi.auth.service;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.common.audit.AuditLogger;
import com.example.visceralmassageapi.common.config.AdminBootstrapProps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.admin.bootstrap", name = "enabled", havingValue = "true")
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final String PHONE_PATTERN = "^\\+[1-9]\\d{9,14}$";
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final AdminBootstrapProps properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            log.info("Admin bootstrap skipped because an admin user already exists.");
            return;
        }

        String phone = required(properties.getPhone(), "ADMIN_BOOTSTRAP_PHONE");
        String password = required(properties.getPassword(), "ADMIN_BOOTSTRAP_PASSWORD");
        String email = normalizeEmail(properties.getEmail());

        if (!phone.matches(PHONE_PATTERN)) {
            throw new IllegalStateException("ADMIN_BOOTSTRAP_PHONE must be in E.164 format.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_BOOTSTRAP_PASSWORD must contain at least 12 characters.");
        }
        if (userRepository.existsByPhone(phone) || email != null && userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Configured bootstrap identity is already used by a non-admin user.");
        }

        User admin = new User();
        admin.setPhone(phone);
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);

        auditLogger.adminBootstrapCreated();
    }

    private String required(String value, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(environmentVariable + " is required when admin bootstrap is enabled.");
        }
        return value.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
