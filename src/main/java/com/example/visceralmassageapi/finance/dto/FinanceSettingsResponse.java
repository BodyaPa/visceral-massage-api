package com.example.visceralmassageapi.finance.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FinanceSettingsResponse(
        BigDecimal quarterlyTaxPercent,
        Long updatedByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
