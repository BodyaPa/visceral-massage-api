package com.example.visceralmassageapi.finance.controller;

import com.example.visceralmassageapi.finance.dto.FinanceEventEnrollmentResponse;
import com.example.visceralmassageapi.finance.service.FinanceEventEnrollmentService;
import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/admin/finance/event-enrollments")
@RequiredArgsConstructor
public class AdminFinanceEventEnrollmentController {

    private final FinanceEventEnrollmentService enrollmentService;

    @GetMapping
    public Page<FinanceEventEnrollmentResponse> list(
            @RequestParam(required = false) FixedEventEnrollmentStatus status,
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Pageable pageable
    ) {
        return enrollmentService.list(status, officeId, from, to, pageable);
    }

    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<FinanceEventEnrollmentResponse> confirmPayment(Authentication authentication, @PathVariable long id) {
        return ResponseEntity.ok(enrollmentService.confirmPayment(id, currentUserId(authentication)));
    }

    private long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new IllegalArgumentException("Not authenticated");
        }
        return userId;
    }
}
