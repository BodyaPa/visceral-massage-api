package com.example.visceralmassageapi.finance.controller;

import com.example.visceralmassageapi.finance.dto.FinanceSummaryResponse;
import com.example.visceralmassageapi.finance.service.FinanceSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/admin/finance/summary")
@RequiredArgsConstructor
public class AdminFinanceSummaryController {

    private final FinanceSummaryService summaryService;

    @GetMapping
    public FinanceSummaryResponse get(
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expenseFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expenseTo
    ) {
        return summaryService.summarize(officeId, from, to, expenseFrom, expenseTo);
    }
}
