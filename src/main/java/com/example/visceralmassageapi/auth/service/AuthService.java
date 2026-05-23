package com.example.visceralmassageapi.auth.service;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.dto.LoginRequest;
import com.example.visceralmassageapi.auth.dto.RegisterRequest;
import com.example.visceralmassageapi.auth.dto.UserDto;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.common.config.CookieProps;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.visceralmassageapi.common.exception.NotFoundException;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CookieProps cookieProps;

    public AuthResult register(RegisterRequest req) {
        String phone = normalizePhone(req.getPhone());
        String email = normalizeEmail(req.getEmail());

        if (userRepository.existsByPhone(phone)) {
            throw new BadRequestException("Phone already exists");
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already exists");
        }

        User u = new User();
        u.setPhone(phone);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setRole(UserRole.USER);
        u.setEnabled(true);

        u = userRepository.save(u);

        return issueTokens(u);
    }

    public AuthResult login(LoginRequest req) {
        String phone = normalizePhone(req.getPhone());

        User u = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        if (!u.isEnabled()) {
            throw new BadRequestException("User disabled");
        }

        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw new BadRequestException("Invalid credentials");
        }

        return issueTokens(u);
    }

    public AuthResult refresh(String refreshToken) {
        long userId = jwtService.getUserId(refreshToken);

        User u = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (!u.isEnabled()) {
            throw new BadRequestException("User disabled");
        }

        return issueTokens(u);
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

        return new AuthResult(
                u,
                buildAccessCookie(access),
                buildRefreshCookie(refresh)
        );
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("Phone is required");
        }

        String cleaned = phone
                .trim()
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");

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

    public record AuthResult(User user, ResponseCookie accessCookie, ResponseCookie refreshCookie) {
        public UserDto userDto() {
            return new UserDto(user.getId(), user.getPhone(), user.getEmail(), user.getRole());
        }
    }

    public UserDto me(long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return new UserDto(u.getId(), u.getPhone(), u.getEmail(), u.getRole());
    }
}