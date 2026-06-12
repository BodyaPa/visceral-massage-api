package com.example.visceralmassageapi.finance.dto;

import java.math.BigDecimal;

public record SpecialistFinanceOverviewResponse(
        long completedCount,
        long pendingCount,
        long payoutPendingCount,
        long payoutPaidCount,
        long workedMinutes,
        BigDecimal grossIncome,
        BigDecimal specialistEarnings,
        BigDecimal payoutPendingEarnings,
        BigDecimal payoutPaidEarnings,
        BigDecimal pendingGrossIncome,
        BigDecimal pendingSpecialistEarnings,
        BigDecimal specialistSharePercent
) {
}
