package com.example.visceralmassageapi.site.dto;

import java.time.OffsetDateTime;

public record SiteSettingsResponse(
        String footerBodyUa,
        String footerBodyEn,
        String homeIntroUa,
        String homeIntroEn,
        String aboutBodyUa,
        String aboutBodyEn,
        String contactBodyUa,
        String contactBodyEn,
        Long updatedByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
