package com.example.visceralmassageapi.finance.dto;

import java.math.BigDecimal;

public record FinanceSummaryResponse(
        long pendingCount,
        long confirmedCount,
        BigDecimal income,
        BigDecimal specialistEarnings,
        BigDecimal businessIncome,
        BigDecimal expenses,
        BigDecimal taxableIncome,
        BigDecimal quarterlyTaxPercent,
        BigDecimal estimatedTax,
        BigDecimal result
) {
}
