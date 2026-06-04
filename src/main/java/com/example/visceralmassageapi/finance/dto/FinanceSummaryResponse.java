package com.example.visceralmassageapi.finance.dto;

import java.math.BigDecimal;

public record FinanceSummaryResponse(
        long pendingCount,
        long confirmedCount,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal result
) {
}
