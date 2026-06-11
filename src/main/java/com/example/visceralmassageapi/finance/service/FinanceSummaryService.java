package com.example.visceralmassageapi.finance.service;

import com.example.visceralmassageapi.booking.domain.Booking;
import com.example.visceralmassageapi.booking.domain.BookingStatus;
import com.example.visceralmassageapi.booking.repository.BookingRepository;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.finance.domain.FinanceExpense;
import com.example.visceralmassageapi.finance.dto.FinanceSummaryResponse;
import com.example.visceralmassageapi.finance.repository.FinanceExpenseSpecifications;
import com.example.visceralmassageapi.finance.repository.SpecialistFinanceSettingsRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.visceralmassageapi.booking.repository.BookingSpecifications.financeFilter;

@Service
@RequiredArgsConstructor
public class FinanceSummaryService {

    private final BookingRepository bookingRepository;
    private final EntityManager entityManager;
    private final SpecialistFinanceSettingsRepository specialistFinanceSettingsRepository;
    private final FinanceSettingsService financeSettingsService;

    @Transactional(readOnly = true)
    public FinanceSummaryResponse summarize(
            Long officeId,
            OffsetDateTime from,
            OffsetDateTime to,
            LocalDate expenseFrom,
            LocalDate expenseTo
    ) {
        validateRanges(from, to, expenseFrom, expenseTo);

        long pendingCount = bookingRepository.count(financeFilter(
                BookingStatus.AWAITING_PAYMENT_CONFIRMATION, officeId, from, to
        ));
        long confirmedCount = bookingRepository.count(financeFilter(BookingStatus.CONFIRMED, officeId, from, to));
        List<Booking> confirmedBookings = bookingRepository.findAll(financeFilter(BookingStatus.CONFIRMED, officeId, from, to));
        BigDecimal income = confirmedBookings.stream()
                .map(Booking::getBookedPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal specialistEarnings = sumSpecialistEarnings(confirmedBookings);
        BigDecimal businessIncome = income.subtract(specialistEarnings);
        BigDecimal expenses = sumExpenses(officeId, expenseFrom, expenseTo);
        BigDecimal taxableIncome = businessIncome.subtract(expenses).max(BigDecimal.ZERO);
        BigDecimal quarterlyTaxPercent = financeSettingsService.quarterlyTaxPercent();
        BigDecimal estimatedTax = taxableIncome
                .multiply(quarterlyTaxPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return new FinanceSummaryResponse(
                pendingCount,
                confirmedCount,
                income,
                specialistEarnings,
                businessIncome,
                expenses,
                taxableIncome,
                quarterlyTaxPercent,
                estimatedTax,
                businessIncome.subtract(expenses)
        );
    }

    private BigDecimal sumExpenses(Long officeId, LocalDate from, LocalDate to) {
        var builder = entityManager.getCriteriaBuilder();
        var query = builder.createQuery(BigDecimal.class);
        var expense = query.from(FinanceExpense.class);
        var predicate = FinanceExpenseSpecifications.financeFilter(officeId, from, to)
                .toPredicate(expense, query, builder);

        query.select(builder.coalesce(builder.sum(expense.get("amount")), BigDecimal.ZERO));
        query.where(predicate);
        return entityManager.createQuery(query).getSingleResult();
    }

    private BigDecimal sumSpecialistEarnings(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return BigDecimal.ZERO;
        }

        var specialistIds = bookings.stream()
                .map(booking -> booking.getSpecialist().getId())
                .collect(Collectors.toSet());
        Map<Long, BigDecimal> sharePercents = specialistFinanceSettingsRepository
                .findBySpecialistUserIdIn(specialistIds)
                .stream()
                .collect(Collectors.toMap(
                        settings -> settings.getSpecialist().getId(),
                        settings -> settings.getSpecialistSharePercent()
                ));

        return bookings.stream()
                .map(booking -> FinanceShareCalculator.specialistShare(
                        booking.getBookedPrice(),
                        sharePercents.getOrDefault(booking.getSpecialist().getId(), BigDecimal.ZERO)
                ))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateRanges(OffsetDateTime from, OffsetDateTime to, LocalDate expenseFrom, LocalDate expenseTo) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new BadRequestException("Booking range is invalid");
        }
        if (expenseFrom != null && expenseTo != null && expenseTo.isBefore(expenseFrom)) {
            throw new BadRequestException("Expense date range is invalid");
        }
    }
}
