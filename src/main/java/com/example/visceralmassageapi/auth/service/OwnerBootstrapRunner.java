package com.example.visceralmassageapi.auth.service;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.common.audit.AuditLogger;
import com.example.visceralmassageapi.common.config.OwnerBootstrapProps;
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
@ConditionalOnProperty(prefix = "app.owner.bootstrap", name = "enabled", havingValue = "true")
public class OwnerBootstrapRunner implements ApplicationRunner {

    private static final String PHONE_PATTERN = "^\\+[1-9]\\d{9,14}$";
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final OwnerBootstrapProps properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByAssignedRole(UserRole.MASTER)) {
            log.info("Owner bootstrap skipped because an owner user already exists.");
            return;
        }

        String phone = required(properties.getPhone(), "OWNER_BOOTSTRAP_PHONE");
        String password = required(properties.getPassword(), "OWNER_BOOTSTRAP_PASSWORD");
        String email = normalizeEmail(properties.getEmail());

        if (!phone.matches(PHONE_PATTERN)) {
            throw new IllegalStateException("OWNER_BOOTSTRAP_PHONE must be in E.164 format.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("OWNER_BOOTSTRAP_PASSWORD must contain at least 12 characters.");
        }
        if (userRepository.existsByPhone(phone) || email != null && userRepository.existsByEmail(email)) {
            throw new IllegalStateException("Configured bootstrap identity is already used by another user.");
        }

        User owner = new User();
        owner.setPhone(phone);
        owner.setEmail(email);
        owner.setPasswordHash(passwordEncoder.encode(password));
        owner.getRoles().add(UserRole.USER);
        owner.getRoles().add(UserRole.MASTER);
        owner.getRoles().add(UserRole.SPECIALIST);
        owner.getRoles().add(UserRole.FINANCE_MANAGER);
        owner.getRoles().add(UserRole.SMM);
        owner.setEnabled(true);
        userRepository.save(owner);

        auditLogger.adminBootstrapCreated();
    }

    private String required(String value, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(environmentVariable + " is required when owner bootstrap is enabled.");
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
