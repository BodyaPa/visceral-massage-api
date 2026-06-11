package com.example.visceralmassageapi.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class FinanceShareCalculator {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private FinanceShareCalculator() {
    }

    public static BigDecimal specialistShare(BigDecimal amount, BigDecimal specialistSharePercent) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal percent = specialistSharePercent == null ? BigDecimal.ZERO : specialistSharePercent;
        return amount.multiply(percent).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal businessShare(BigDecimal amount, BigDecimal specialistShare) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal share = specialistShare == null ? BigDecimal.ZERO : specialistShare;
        return amount.subtract(share).setScale(2, RoundingMode.HALF_UP);
    }
}
