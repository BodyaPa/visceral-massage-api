package com.example.visceralmassageapi.site.controller;

import com.example.visceralmassageapi.site.dto.SiteSettingsResponse;
import com.example.visceralmassageapi.site.service.SiteSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/site-settings")
@RequiredArgsConstructor
public class SiteSettingsController {

    private final SiteSettingsService settingsService;

    @GetMapping
    public SiteSettingsResponse get() {
        return settingsService.get();
    }
}
