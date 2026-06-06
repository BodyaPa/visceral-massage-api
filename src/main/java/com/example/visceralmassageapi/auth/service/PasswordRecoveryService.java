package com.example.visceralmassageapi.auth.service;

import com.example.visceralmassageapi.auth.domain.PasswordRecoveryToken;
import com.example.visceralmassageapi.auth.dto.PasswordRecoveryConfirmRequest;
import com.example.visceralmassageapi.auth.dto.PasswordRecoveryRequest;
import com.example.visceralmassageapi.auth.repo.PasswordRecoveryTokenRepository;
import com.example.visceralmassageapi.auth.repo.RefreshTokenRepository;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private static final int CODE_TTL_MINUTES = 15;
    private static final int MAX_CONFIRM_ATTEMPTS = 5;
    private static final int MAX_REQUESTS_PER_WINDOW = 3;
    private static final int REQUEST_WINDOW_MINUTES = 15;
    private static final String GENERIC_INVALID_CODE = "Invalid or expired recovery code";

    private final UserRepository userRepository;
    private final PasswordRecoveryTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void request(PasswordRecoveryRequest request) {
        String email = normalizeEmail(request.getEmail());
        var user = userRepository.findByEmail(email).filter(found -> found.isEnabled()).orElse(null);

        if (user == null) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        long recentRequests = tokenRepository.countByEmailAndCreatedAtAfter(email, now.minusMinutes(REQUEST_WINDOW_MINUTES));
        if (recentRequests >= MAX_REQUESTS_PER_WINDOW) {
            return;
        }

        String code = generateCode();
        String salt = generateSalt();

        PasswordRecoveryToken token = new PasswordRecoveryToken();
        token.setUser(user);
        token.setEmail(email);
        token.setCodeSalt(salt);
        token.setCodeHash(hashCode(salt, code));
        token.setExpiresAt(now.plusMinutes(CODE_TTL_MINUTES));
        tokenRepository.save(token);

        notificationService.sendEmail(
                email,
                "Ataraksia password recovery",
                "Your Ataraksia password recovery code is: %s%nIt expires in %d minutes.".formatted(code, CODE_TTL_MINUTES)
        );
    }

    @Transactional
    public void confirm(PasswordRecoveryConfirmRequest request) {
        String email = normalizeEmail(request.getEmail());
        OffsetDateTime now = OffsetDateTime.now();
        PasswordRecoveryToken token = tokenRepository
                .findFirstByEmailAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(email, now)
                .orElseThrow(() -> new BadRequestException(GENERIC_INVALID_CODE));

        if (token.getAttempts() >= MAX_CONFIRM_ATTEMPTS) {
            throw new BadRequestException(GENERIC_INVALID_CODE);
        }

        token.setAttempts(token.getAttempts() + 1);
        if (!hashCode(token.getCodeSalt(), request.getCode()).equals(token.getCodeHash())) {
            throw new BadRequestException(GENERIC_INVALID_CODE);
        }

        var user = token.getUser();
        if (!user.isEnabled()) {
            throw new BadRequestException(GENERIC_INVALID_CODE);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        token.setUsedAt(now);
        refreshTokenRepository.revokeActiveTokensForUser(user.getId(), now);
    }

    private String generateCode() {
        return "%06d".formatted(secureRandom.nextInt(1_000_000));
    }

    private String generateSalt() {
        byte[] bytes = new byte[18];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashCode(String salt, String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((salt + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
