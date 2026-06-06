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
        RecoveryContact contact = recoveryContact(request.getEmail(), request.getPhone());
        var user = contact.type().equals("EMAIL")
                ? userRepository.findByEmail(contact.value()).filter(found -> found.isEnabled()).orElse(null)
                : userRepository.findByPhone(contact.value()).filter(found -> found.isEnabled()).orElse(null);

        if (user == null) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        long recentRequests = tokenRepository.countByContactTypeAndContactValueAndCreatedAtAfter(
                contact.type(),
                contact.value(),
                now.minusMinutes(REQUEST_WINDOW_MINUTES)
        );
        if (recentRequests >= MAX_REQUESTS_PER_WINDOW) {
            return;
        }

        String code = generateCode();
        String salt = generateSalt();

        PasswordRecoveryToken token = new PasswordRecoveryToken();
        token.setUser(user);
        token.setEmail(contact.type().equals("EMAIL") ? contact.value() : null);
        token.setContactType(contact.type());
        token.setContactValue(contact.value());
        token.setCodeSalt(salt);
        token.setCodeHash(hashCode(salt, code));
        token.setExpiresAt(now.plusMinutes(CODE_TTL_MINUTES));
        tokenRepository.save(token);

        if (contact.type().equals("EMAIL")) {
            notificationService.sendEmail(
                    contact.value(),
                    "Ataraksia password recovery",
                    "Your Ataraksia password recovery code is: %s%nIt expires in %d minutes.".formatted(code, CODE_TTL_MINUTES)
            );
            return;
        }

        notificationService.sendSms(
                contact.value(),
                "Ataraksia recovery code: %s. Expires in %d minutes.".formatted(code, CODE_TTL_MINUTES)
        );
    }

    @Transactional
    public void confirm(PasswordRecoveryConfirmRequest request) {
        RecoveryContact contact = recoveryContact(request.getEmail(), request.getPhone());
        OffsetDateTime now = OffsetDateTime.now();
        PasswordRecoveryToken token = tokenRepository
                .findFirstByContactTypeAndContactValueAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        contact.type(),
                        contact.value(),
                        now
                )
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

    private RecoveryContact recoveryContact(String email, String phone) {
        String normalizedEmail = email == null || email.isBlank() ? null : normalizeEmail(email);
        String normalizedPhone = phone == null || phone.isBlank() ? null : normalizePhone(phone);

        if (normalizedEmail == null && normalizedPhone == null) {
            throw new BadRequestException("Email or phone is required");
        }
        if (normalizedEmail != null && normalizedPhone != null) {
            throw new BadRequestException("Use either email or phone");
        }

        return normalizedEmail != null
                ? new RecoveryContact("EMAIL", normalizedEmail)
                : new RecoveryContact("PHONE", normalizedPhone);
    }

    private String normalizePhone(String phone) {
        String cleaned = phone
                .trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");

        if (cleaned.matches("^0\\d{9}$")) {
            cleaned = "+380" + cleaned.substring(1);
        }

        if (!cleaned.startsWith("+")) {
            cleaned = "+" + cleaned;
        }

        if (!cleaned.matches("^\\+[1-9]\\d{9,14}$")) {
            throw new BadRequestException("Invalid phone format");
        }

        return cleaned;
    }

    private record RecoveryContact(String type, String value) {
    }
}
