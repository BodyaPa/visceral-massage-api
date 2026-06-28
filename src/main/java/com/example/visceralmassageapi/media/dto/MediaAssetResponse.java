package com.example.visceralmassageapi.media.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MediaAssetResponse(
        UUID id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        Integer newsId,
        Long officeId,
        Short siteSettingsId,
        Integer siteSliderSortOrder,
        OffsetDateTime createdAt
) {
}
