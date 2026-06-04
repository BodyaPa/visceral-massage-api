package com.example.visceralmassageapi.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record FinanceExpenseResponse(
        long id,
        BigDecimal amount,
        String category,
        String description,
        LocalDate expenseDate,
        Long officeId,
        String officeName,
        long createdByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
