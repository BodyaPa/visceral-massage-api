package com.example.visceralmassageapi.finance.controller;

import com.example.visceralmassageapi.finance.dto.FinanceExpenseRequest;
import com.example.visceralmassageapi.finance.dto.FinanceExpenseResponse;
import com.example.visceralmassageapi.finance.service.FinanceExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/finance/expenses")
@RequiredArgsConstructor
public class AdminFinanceExpenseController {

    private final FinanceExpenseService expenseService;

    @GetMapping
    public Page<FinanceExpenseResponse> list(
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable
    ) {
        return expenseService.list(officeId, from, to, pageable);
    }

    @PostMapping
    public ResponseEntity<FinanceExpenseResponse> create(
            Authentication authentication,
            @Valid @RequestBody FinanceExpenseRequest request
    ) {
        return ResponseEntity.ok(expenseService.create(currentUserId(authentication), request));
    }

    private long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new IllegalArgumentException("Not authenticated");
        }
        return userId;
    }
}
