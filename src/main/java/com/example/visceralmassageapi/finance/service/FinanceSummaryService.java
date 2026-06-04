package com.example.visceralmassageapi.finance.service;

import com.example.visceralmassageapi.booking.domain.Booking;
import com.example.visceralmassageapi.booking.domain.BookingStatus;
import com.example.visceralmassageapi.booking.repository.BookingRepository;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.finance.domain.FinanceExpense;
import com.example.visceralmassageapi.finance.dto.FinanceSummaryResponse;
import com.example.visceralmassageapi.finance.repository.FinanceExpenseSpecifications;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static com.example.visceralmassageapi.booking.repository.BookingSpecifications.financeFilter;

@Service
@RequiredArgsConstructor
public class FinanceSummaryService {

    private final BookingRepository bookingRepository;
    private final EntityManager entityManager;

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
        BigDecimal income = sumConfirmedIncome(officeId, from, to);
        BigDecimal expenses = sumExpenses(officeId, expenseFrom, expenseTo);

        return new FinanceSummaryResponse(pendingCount, confirmedCount, income, expenses, income.subtract(expenses));
    }

    private BigDecimal sumConfirmedIncome(Long officeId, OffsetDateTime from, OffsetDateTime to) {
        var builder = entityManager.getCriteriaBuilder();
        var query = builder.createQuery(BigDecimal.class);
        var booking = query.from(Booking.class);
        var predicate = financeFilter(BookingStatus.CONFIRMED, officeId, from, to)
                .toPredicate(booking, query, builder);

        query.select(builder.coalesce(builder.sum(booking.get("bookedPrice")), BigDecimal.ZERO));
        query.where(predicate);
        return entityManager.createQuery(query).getSingleResult();
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

    private void validateRanges(OffsetDateTime from, OffsetDateTime to, LocalDate expenseFrom, LocalDate expenseTo) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new BadRequestException("Booking range is invalid");
        }
        if (expenseFrom != null && expenseTo != null && expenseTo.isBefore(expenseFrom)) {
            throw new BadRequestException("Expense date range is invalid");
        }
    }
}
