package com.example.visceralmassageapi.finance.dto;

import java.math.BigDecimal;

public record SpecialistFinanceOverviewResponse(
        long completedCount,
        long pendingCount,
        long workedMinutes,
        BigDecimal grossIncome,
        BigDecimal specialistEarnings,
        BigDecimal pendingGrossIncome,
        BigDecimal pendingSpecialistEarnings,
        BigDecimal specialistSharePercent
) {
}
