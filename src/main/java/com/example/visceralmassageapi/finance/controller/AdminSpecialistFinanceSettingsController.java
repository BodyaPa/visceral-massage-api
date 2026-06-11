package com.example.visceralmassageapi.finance.controller;

import com.example.visceralmassageapi.finance.dto.SpecialistFinanceSettingsRequest;
import com.example.visceralmassageapi.finance.dto.SpecialistFinanceSettingsResponse;
import com.example.visceralmassageapi.finance.service.SpecialistFinanceSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/finance/specialists")
@RequiredArgsConstructor
public class AdminSpecialistFinanceSettingsController {

    private final SpecialistFinanceSettingsService settingsService;

    @GetMapping
    public List<SpecialistFinanceSettingsResponse> list() {
        return settingsService.list();
    }

    @GetMapping("/{specialistId}/settings")
    public SpecialistFinanceSettingsResponse get(@PathVariable long specialistId) {
        return settingsService.get(specialistId);
    }

    @PutMapping("/{specialistId}/settings")
    public SpecialistFinanceSettingsResponse update(
            Authentication authentication,
            @PathVariable long specialistId,
            @Valid @RequestBody SpecialistFinanceSettingsRequest request
    ) {
        return settingsService.update(specialistId, currentUserId(authentication), request);
    }

    private long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new IllegalArgumentException("Not authenticated");
        }

        return userId;
    }
}
