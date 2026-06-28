package com.example.visceralmassageapi.finance.service;

import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.common.audit.AuditLogger;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.finance.domain.FinanceExpense;
import com.example.visceralmassageapi.finance.dto.FinanceExpenseRequest;
import com.example.visceralmassageapi.finance.dto.FinanceExpenseResponse;
import com.example.visceralmassageapi.finance.repository.FinanceExpenseRepository;
import com.example.visceralmassageapi.offices.entity.Office;
import com.example.visceralmassageapi.offices.repository.OfficeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.example.visceralmassageapi.finance.repository.FinanceExpenseSpecifications.financeFilter;

@Service
@RequiredArgsConstructor
public class FinanceExpenseService {

    private final FinanceExpenseRepository expenseRepository;
    private final OfficeRepository officeRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    public Page<FinanceExpenseResponse> list(Long officeId, LocalDate from, LocalDate to, Pageable pageable) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException("Expense date range is invalid");
        }

        return expenseRepository.findAll(financeFilter(officeId, from, to), pageable).map(this::toResponse);
    }

    @Transactional
    public FinanceExpenseResponse create(long actorId, FinanceExpenseRequest request) {
        FinanceExpense expense = new FinanceExpense();
        expense.setAmount(request.amount());
        expense.setCategory(normalize(request.category()));
        expense.setDescription(normalize(request.description()));
        expense.setExpenseDate(request.expenseDate());
        expense.setOffice(resolveOffice(request.officeId()));
        expense.setCreatedBy(userRepository.findById(actorId)
                .orElseThrow(() -> new NotFoundException("User not found")));

        FinanceExpense saved = expenseRepository.save(expense);
        auditLogger.financeExpenseCreated(saved.getId(), actorId);
        return toResponse(saved);
    }

    private Office resolveOffice(Long officeId) {
        if (officeId == null) {
            return null;
        }
        return officeRepository.findById(officeId)
                .orElseThrow(() -> new NotFoundException("Office not found"));
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private FinanceExpenseResponse toResponse(FinanceExpense expense) {
        Office office = expense.getOffice();
        return new FinanceExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getCategory(),
                expense.getDescription(),
                expense.getExpenseDate(),
                office == null ? null : office.getId(),
                office == null ? null : office.getName(),
                expense.getCreatedBy().getId(),
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }
}
