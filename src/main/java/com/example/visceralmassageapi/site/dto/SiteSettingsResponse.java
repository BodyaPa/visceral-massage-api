package com.example.visceralmassageapi.site.dto;

import java.time.OffsetDateTime;

public record SiteSettingsResponse(
        String footerBodyUa,
        String footerBodyEn,
        String homeIntroUa,
        String homeIntroEn,
        String homeBodyUa,
        String homeBodyEn,
        String aboutBodyUa,
        String aboutBodyEn,
        String contactBodyUa,
        String contactBodyEn,
        String heroMediaUrls,
        Long updatedByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
