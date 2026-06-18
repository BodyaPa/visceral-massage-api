package com.example.visceralmassageapi.site.controller;

import com.example.visceralmassageapi.site.dto.SiteSettingsRequest;
import com.example.visceralmassageapi.site.dto.SiteSettingsResponse;
import com.example.visceralmassageapi.site.service.SiteSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/site-settings")
@RequiredArgsConstructor
public class AdminSiteSettingsController {

    private final SiteSettingsService settingsService;

    @GetMapping
    public SiteSettingsResponse get() {
        return settingsService.get();
    }

    @PutMapping
    public SiteSettingsResponse update(Authentication authentication, @Valid @RequestBody SiteSettingsRequest request) {
        return settingsService.update(currentUserId(authentication), request);
    }

    private long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Number id)) {
            throw new IllegalArgumentException("Not authenticated");
        }
        return id.longValue();
    }
}
