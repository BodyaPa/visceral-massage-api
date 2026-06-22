package com.example.visceralmassageapi.auth.api;

import com.example.visceralmassageapi.auth.dto.ContactChangeConfirmRequest;
import com.example.visceralmassageapi.auth.dto.ContactChangeRequest;
import com.example.visceralmassageapi.auth.dto.LoginRequest;
import com.example.visceralmassageapi.auth.dto.PasswordChangeRequest;
import com.example.visceralmassageapi.auth.dto.PasswordRecoveryConfirmRequest;
import com.example.visceralmassageapi.auth.dto.PasswordRecoveryRequest;
import com.example.visceralmassageapi.auth.dto.ProfileUpdateRequest;
import com.example.visceralmassageapi.auth.dto.RegisterConfirmRequest;
import com.example.visceralmassageapi.auth.dto.RegisterRequest;
import com.example.visceralmassageapi.auth.dto.UserDto;
import com.example.visceralmassageapi.auth.service.AuthService;
import com.example.visceralmassageapi.auth.service.PasswordRecoveryService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordRecoveryService passwordRecoveryService;
    private final CookieCsrfTokenRepository csrfTokenRepository;

    @PostMapping("/register")
    public ResponseEntity<Void> requestRegistration(@Valid @RequestBody RegisterRequest req) {
        authService.requestRegistration(req);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register/confirm")
    public ResponseEntity<UserDto> confirmRegistration(@Valid @RequestBody RegisterConfirmRequest req) {
        var result = authService.confirmRegistration(req);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.accessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString())
                .body(result.userDto());
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@Valid @RequestBody LoginRequest req) {
        var result = authService.login(req);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.accessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString())
                .body(result.userDto());
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request) {
        String refreshToken = readCookie(request, AuthService.REFRESH_COOKIE);
        var result = authService.refresh(refreshToken);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, result.accessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(readOptionalCookie(request, AuthService.REFRESH_COOKIE));

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authService.clearAccessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authService.clearRefreshCookie().toString())
                .build();
    }

    @PostMapping("/password-recovery/request")
    public ResponseEntity<Void> requestPasswordRecovery(@Valid @RequestBody PasswordRecoveryRequest request) {
        passwordRecoveryService.request(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-recovery/confirm")
    public ResponseEntity<Void> confirmPasswordRecovery(@Valid @RequestBody PasswordRecoveryConfirmRequest request) {
        passwordRecoveryService.confirm(request);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authService.clearAccessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authService.clearRefreshCookie().toString())
                .build();
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(token, request, response);
        return token;
    }

    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        return authService.me(currentUserId(authentication));
    }

    @PutMapping("/me")
    public UserDto updateMe(Authentication authentication, @Valid @RequestBody ProfileUpdateRequest request) {
        return authService.updateProfile(currentUserId(authentication), request);
    }

    @PostMapping(path = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserDto updateAvatar(Authentication authentication, @RequestPart("file") MultipartFile file) {
        return authService.updateAvatar(currentUserId(authentication), file);
    }

    @PostMapping("/me/contact-change/request")
    public ResponseEntity<Void> requestContactChange(
            Authentication authentication,
            @Valid @RequestBody ContactChangeRequest request
    ) {
        authService.requestContactChange(currentUserId(authentication), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/contact-change/confirm")
    public UserDto confirmContactChange(
            Authentication authentication,
            @Valid @RequestBody ContactChangeConfirmRequest request
    ) {
        return authService.confirmContactChange(currentUserId(authentication), request);
    }

    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        authService.changePassword(currentUserId(authentication), request);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authService.clearAccessCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authService.clearRefreshCookie().toString())
                .build();
    }

    private long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("Not authenticated");
        }
        return (long) authentication.getPrincipal();
    }

    private String readCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) throw new IllegalArgumentException("No cookies");
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        throw new IllegalArgumentException("Missing cookie: " + name);
    }

    private String readOptionalCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
