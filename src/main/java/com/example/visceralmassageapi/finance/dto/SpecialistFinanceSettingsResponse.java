package com.example.visceralmassageapi.finance.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SpecialistFinanceSettingsResponse(
        long specialistId,
        String specialistName,
        BigDecimal specialistSharePercent,
        Long updatedByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
