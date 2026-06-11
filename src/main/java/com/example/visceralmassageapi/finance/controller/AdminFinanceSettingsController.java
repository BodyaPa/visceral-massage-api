package com.example.visceralmassageapi.finance.controller;

import com.example.visceralmassageapi.finance.dto.FinanceSettingsRequest;
import com.example.visceralmassageapi.finance.dto.FinanceSettingsResponse;
import com.example.visceralmassageapi.finance.service.FinanceSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/finance/settings")
@RequiredArgsConstructor
public class AdminFinanceSettingsController {

    private final FinanceSettingsService settingsService;

    @GetMapping
    public FinanceSettingsResponse get() {
        return settingsService.get();
    }

    @PutMapping
    public FinanceSettingsResponse update(
            Authentication authentication,
            @Valid @RequestBody FinanceSettingsRequest request
    ) {
        return settingsService.update(currentUserId(authentication), request);
    }

    private long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new IllegalArgumentException("Not authenticated");
        }

        return userId;
    }
}
