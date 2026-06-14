package com.example.visceralmassageapi.auth.service;

import com.example.visceralmassageapi.auth.domain.RefreshToken;
import com.example.visceralmassageapi.auth.domain.RegistrationVerificationToken;
import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.dto.LoginRequest;
import com.example.visceralmassageapi.auth.dto.RegisterConfirmRequest;
import com.example.visceralmassageapi.auth.dto.RegisterRequest;
import com.example.visceralmassageapi.auth.dto.ProfileUpdateRequest;
import com.example.visceralmassageapi.auth.dto.UserDto;
import com.example.visceralmassageapi.auth.repo.RefreshTokenRepository;
import com.example.visceralmassageapi.auth.repo.RegistrationVerificationTokenRepository;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.common.audit.AuditLogger;
import com.example.visceralmassageapi.common.config.CookieProps;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.common.security.JwtService;
import com.example.visceralmassageapi.notifications.service.NotificationService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";
    private static final int REGISTRATION_CODE_TTL_MINUTES = 15;
    private static final int MAX_REGISTRATION_CONFIRM_ATTEMPTS = 5;
    private static final int MAX_REGISTRATION_REQUESTS_PER_WINDOW = 3;
    private static final int REGISTRATION_REQUEST_WINDOW_MINUTES = 15;
    private static final String GENERIC_INVALID_REGISTRATION_CODE = "Invalid or expired registration code";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RegistrationVerificationTokenRepository registrationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CookieProps cookieProps;
    private final AuditLogger auditLogger;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void requestRegistration(RegisterRequest req) {
        String phone = normalizeOptionalPhone(req.getPhone());
        String email = normalizeEmail(req.getEmail());
        String firstName = normalizeName(req.getFirstName());
        String lastName = normalizeName(req.getLastName());

        if (phone == null && email == null) {
            throw new BadRequestException("Phone or email is required");
        }
        if (phone != null && userRepository.existsByPhone(phone)
                || email != null && userRepository.existsByEmail(email)) {
            throw new BadRequestException("Account already registered");
        }

        RegistrationContact contact = registrationContact(email, phone);
        OffsetDateTime now = OffsetDateTime.now();
        long recentRequests = registrationTokenRepository.countByContactTypeAndContactValueAndCreatedAtAfter(
                contact.type(),
                contact.value(),
                now.minusMinutes(REGISTRATION_REQUEST_WINDOW_MINUTES)
        );
        if (recentRequests >= MAX_REGISTRATION_REQUESTS_PER_WINDOW) {
            throw new BadRequestException("Too many registration requests");
        }

        String code = generateCode();
        String salt = generateSalt();

        RegistrationVerificationToken token = new RegistrationVerificationToken();
        token.setPhone(phone);
        token.setEmail(email);
        token.setFirstName(firstName);
        token.setLastName(lastName);
        token.setDateOfBirth(req.getDateOfBirth());
        token.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        token.setContactType(contact.type());
        token.setContactValue(contact.value());
        token.setCodeSalt(salt);
        token.setCodeHash(hashCode(salt, code));
        token.setExpiresAt(now.plusMinutes(REGISTRATION_CODE_TTL_MINUTES));
        registrationTokenRepository.save(token);

        if (contact.type().equals("EMAIL")) {
            notificationService.sendEmail(
                    contact.value(),
                    "Ataraksia registration confirmation",
                    "Your Ataraksia registration code is: %s%nIt expires in %d minutes.".formatted(code, REGISTRATION_CODE_TTL_MINUTES)
            );
            return;
        }

        notificationService.sendSms(
                contact.value(),
                "Ataraksia registration code: %s. Expires in %d minutes.".formatted(code, REGISTRATION_CODE_TTL_MINUTES)
        );
    }

    @Transactional
    public AuthResult confirmRegistration(RegisterConfirmRequest req) {
        RegistrationContact contact = registrationContact(normalizeEmail(req.getEmail()), normalizeOptionalPhone(req.getPhone()));
        OffsetDateTime now = OffsetDateTime.now();
        RegistrationVerificationToken token = registrationTokenRepository
                .findFirstByContactTypeAndContactValueAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        contact.type(),
                        contact.value(),
                        now
                )
                .orElseThrow(() -> new BadRequestException(GENERIC_INVALID_REGISTRATION_CODE));

        if (token.getAttempts() >= MAX_REGISTRATION_CONFIRM_ATTEMPTS) {
            throw new BadRequestException(GENERIC_INVALID_REGISTRATION_CODE);
        }

        token.setAttempts(token.getAttempts() + 1);
        if (!hashCode(token.getCodeSalt(), req.getCode()).equals(token.getCodeHash())) {
            throw new BadRequestException(GENERIC_INVALID_REGISTRATION_CODE);
        }

        if (token.getPhone() != null && userRepository.existsByPhone(token.getPhone())
                || token.getEmail() != null && userRepository.existsByEmail(token.getEmail())) {
            throw new BadRequestException("Account already registered");
        }

        User u = new User();
        u.setPhone(token.getPhone());
        u.setEmail(token.getEmail());
        u.setFirstName(token.getFirstName());
        u.setLastName(token.getLastName());
        u.setDateOfBirth(token.getDateOfBirth());
        u.setPasswordHash(token.getPasswordHash());
        u.getRoles().add(UserRole.USER);
        u.setEnabled(true);

        u = userRepository.save(u);
        token.setUsedAt(now);

        AuthResult result = issueTokens(u);
        auditLogger.userRegistered(u.getId());
        return result;
    }

    @Transactional
    public AuthResult login(LoginRequest req) {
        User u = findByIdentifier(req.getIdentifier());

        if (u == null) {
            auditLogger.loginFailed();
            throw new BadRequestException("Invalid credentials");
        }

        if (!u.isEnabled()) {
            auditLogger.loginFailed();
            throw new BadRequestException("Invalid credentials");
        }

        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
            auditLogger.loginFailed();
            throw new BadRequestException("Invalid credentials");
        }

        AuthResult result = issueTokens(u);
        auditLogger.loginSucceeded(u.getId());
        return result;
    }

    @Transactional
    public AuthResult refresh(String refreshToken) {
        RefreshToken storedToken = requireActiveRefreshToken(refreshToken);
        User u = storedToken.getUser();

        if (!u.isEnabled()) {
            throw new BadRequestException("User disabled");
        }

        storedToken.setRevokedAt(OffsetDateTime.now());
        return issueTokens(u);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hashToken(refreshToken))
                .ifPresent(token -> token.setRevokedAt(OffsetDateTime.now()));
    }

    public ResponseCookie buildAccessCookie(String token) {
        return baseCookie(ACCESS_COOKIE, token, jwtService.getAccessTtlSeconds());
    }

    public ResponseCookie buildRefreshCookie(String token) {
        return baseCookie(REFRESH_COOKIE, token, jwtService.getRefreshTtlSeconds());
    }

    public ResponseCookie clearAccessCookie() {
        return baseCookie(ACCESS_COOKIE, "", 0);
    }

    public ResponseCookie clearRefreshCookie() {
        return baseCookie(REFRESH_COOKIE, "", 0);
    }

    private ResponseCookie baseCookie(String name, String value, int maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieProps.isSecure())
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(cookieProps.getSameSite())
                .build();
    }

    private AuthResult issueTokens(User u) {
        Set<UserRole> roles = effectiveRoles(u);
        String access = jwtService.generateAccessToken(u.getId(), roleNames(roles));
        String refresh = jwtService.generateRefreshToken(u.getId());

        RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(u);
        storedToken.setTokenHash(hashToken(refresh));
        storedToken.setExpiresAt(jwtService.getExpiration(refresh));
        refreshTokenRepository.save(storedToken);

        return new AuthResult(
                u,
                buildAccessCookie(access),
                buildRefreshCookie(refresh)
        );
    }

    private RefreshToken requireActiveRefreshToken(String token) {
        try {
            if (!jwtService.isRefreshToken(token)) {
                throw new BadRequestException("Invalid refresh token");
            }

            long userId = jwtService.getUserId(token);
            RefreshToken storedToken = refreshTokenRepository
                    .findByTokenHashAndRevokedAtIsNull(hashToken(token))
                    .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

            if (!storedToken.getUser().getId().equals(userId)
                    || !storedToken.getExpiresAt().isAfter(OffsetDateTime.now())) {
                throw new BadRequestException("Invalid refresh token");
            }

            return storedToken;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadRequestException("Invalid refresh token");
        }
    }

    private String hashToken(String token) {
        return sha256(token);
    }

    private String hashCode(String salt, String code) {
        return sha256(salt + ":" + code);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private String generateCode() {
        return "%06d".formatted(secureRandom.nextInt(1_000_000));
    }

    private String generateSalt() {
        byte[] bytes = new byte[18];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private User findByIdentifier(String identifier) {
        String value = identifier.trim();
        if (value.contains("@")) {
            return userRepository.findByEmail(normalizeEmail(value)).orElse(null);
        }

        try {
            return userRepository.findByPhone(normalizePhone(value)).orElse(null);
        } catch (BadRequestException ex) {
            return null;
        }
    }

    private String normalizeOptionalPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        return normalizePhone(phone);
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

        // check format
        if (!cleaned.matches("^\\+[1-9]\\d{9,14}$")) {
            throw new BadRequestException("Invalid phone format");
        }

        return cleaned;
    }

    private String normalizeEmail(String email) {
        if (email == null) return null;
        String e = email.trim().toLowerCase(Locale.ROOT);
        return e.isBlank() ? null : e;
    }

    private String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private RegistrationContact registrationContact(String email, String phone) {
        if (email == null && phone == null) {
            throw new BadRequestException("Phone or email is required");
        }
        return email != null
                ? new RegistrationContact("EMAIL", email)
                : new RegistrationContact("PHONE", phone);
    }

    private record RegistrationContact(String type, String value) {
    }

    public record AuthResult(User user, ResponseCookie accessCookie, ResponseCookie refreshCookie) {
        public UserDto userDto() {
            return new UserDto(user.getId(), user.getPhone(), user.getEmail(),
                    user.getFirstName(), user.getLastName(), user.getDateOfBirth(), effectiveRoles(user));
        }
    }

    public UserDto me(long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return new UserDto(u.getId(), u.getPhone(), u.getEmail(),
                u.getFirstName(), u.getLastName(), u.getDateOfBirth(), effectiveRoles(u));
    }

    @Transactional
    public UserDto updateProfile(long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setFirstName(normalizeName(request.getFirstName()));
        user.setLastName(normalizeName(request.getLastName()));
        user.setDateOfBirth(request.getDateOfBirth());
        return new UserDto(user.getId(), user.getPhone(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getDateOfBirth(), effectiveRoles(user));
    }

    private static Set<UserRole> effectiveRoles(User user) {
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            return user.getRoles();
        }
        return Set.of(UserRole.USER);
    }

    private static Set<String> roleNames(Set<UserRole> roles) {
        return roles.stream()
                .sorted(Comparator.comparing(Enum::name))
                .map(Enum::name)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
