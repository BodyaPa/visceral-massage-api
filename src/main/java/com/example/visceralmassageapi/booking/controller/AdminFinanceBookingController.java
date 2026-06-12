package com.example.visceralmassageapi.booking.controller;

import com.example.visceralmassageapi.booking.domain.BookingStatus;
import com.example.visceralmassageapi.booking.dto.FinanceBookingResponse;
import com.example.visceralmassageapi.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/admin/finance/bookings")
@RequiredArgsConstructor
public class AdminFinanceBookingController {

    private final BookingService bookingService;

    @GetMapping
    public Page<FinanceBookingResponse> list(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Pageable pageable
    ) {
        return bookingService.listFinanceBookings(status, officeId, from, to, pageable);
    }

    @PostMapping("/{id}/confirm-payment")
    public ResponseEntity<FinanceBookingResponse> confirmPayment(Authentication authentication, @PathVariable long id) {
        return ResponseEntity.ok(bookingService.confirmPayment(id, currentUserId(authentication)));
    }

    @PostMapping("/{id}/specialist-payout/mark-paid")
    public ResponseEntity<FinanceBookingResponse> markSpecialistPayoutPaid(Authentication authentication, @PathVariable long id) {
        return ResponseEntity.ok(bookingService.markSpecialistPayoutPaid(id, currentUserId(authentication)));
    }

    private long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new IllegalArgumentException("Not authenticated");
        }

        return userId;
    }
}
