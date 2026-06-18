package com.example.visceralmassageapi.finance.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FinanceShareCalculatorTest {

    @Test
    void specialistShareRoundsMoneyToTwoDecimals() {
        BigDecimal share = FinanceShareCalculator.specialistShare(
                BigDecimal.valueOf(999.99),
                BigDecimal.valueOf(33.33)
        );

        assertThat(share).isEqualByComparingTo("333.30");
    }

    @Test
    void specialistShareDefaultsMissingAmountOrPercentToZero() {
        assertThat(FinanceShareCalculator.specialistShare(null, BigDecimal.valueOf(25)))
                .isEqualByComparingTo("0.00");
        assertThat(FinanceShareCalculator.specialistShare(BigDecimal.valueOf(1200), null))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void businessShareSubtractsSpecialistShareAndKeepsMoneyScale() {
        BigDecimal specialistShare = FinanceShareCalculator.specialistShare(
                BigDecimal.valueOf(1200),
                BigDecimal.valueOf(25)
        );

        assertThat(FinanceShareCalculator.businessShare(BigDecimal.valueOf(1200), specialistShare))
                .isEqualByComparingTo("900.00");
    }

    @Test
    void businessShareDefaultsMissingAmountOrSpecialistShareToZero() {
        assertThat(FinanceShareCalculator.businessShare(null, BigDecimal.valueOf(300)))
                .isEqualByComparingTo("0.00");
        assertThat(FinanceShareCalculator.businessShare(BigDecimal.valueOf(1200), null))
                .isEqualByComparingTo("1200.00");
    }
}
