package com.example.visceralmassageapi.auth.service;

import com.example.visceralmassageapi.auth.domain.RefreshToken;
import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.dto.LoginRequest;
import com.example.visceralmassageapi.auth.dto.RegisterRequest;
import com.example.visceralmassageapi.auth.dto.UserDto;
import com.example.visceralmassageapi.auth.repo.RefreshTokenRepository;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.common.audit.AuditLogger;
import com.example.visceralmassageapi.common.config.CookieProps;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.common.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CookieProps cookieProps;
    private final AuditLogger auditLogger;

    @Transactional
    public AuthResult register(RegisterRequest req) {
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

        User u = new User();
        u.setPhone(phone);
        u.setEmail(email);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setRole(UserRole.USER);
        u.setEnabled(true);

        u = userRepository.save(u);

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
        String access = jwtService.generateAccessToken(u.getId(), u.getRole().name());
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
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
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

    public record AuthResult(User user, ResponseCookie accessCookie, ResponseCookie refreshCookie) {
        public UserDto userDto() {
            return new UserDto(user.getId(), user.getPhone(), user.getEmail(),
                    user.getFirstName(), user.getLastName(), user.getRole());
        }
    }

    public UserDto me(long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return new UserDto(u.getId(), u.getPhone(), u.getEmail(),
                u.getFirstName(), u.getLastName(), u.getRole());
    }
}
