package com.example.visceralmassageapi.finance.controller;

import com.example.visceralmassageapi.finance.dto.SpecialistFinanceOverviewResponse;
import com.example.visceralmassageapi.finance.service.SpecialistFinanceOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/specialist/finance/overview")
@RequiredArgsConstructor
public class SpecialistFinanceOverviewController {

    private final SpecialistFinanceOverviewService overviewService;

    @GetMapping
    public SpecialistFinanceOverviewResponse get(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return overviewService.getOverview(currentUserId(authentication), from, to);
    }

    private long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new IllegalArgumentException("Not authenticated");
        }

        return userId;
    }
}
