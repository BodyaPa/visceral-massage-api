package com.example.visceralmassageapi.booking.controller;

import com.example.visceralmassageapi.booking.dto.BookingRequest;
import com.example.visceralmassageapi.booking.dto.BookingResponse;
import com.example.visceralmassageapi.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("/my")
    public Page<BookingResponse> listMyBookings(Authentication authentication, Pageable pageable) {
        return bookingService.listUserBookings(currentUserId(authentication), pageable);
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(Authentication authentication, @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.ok(bookingService.create(currentUserId(authentication), request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(Authentication authentication, @PathVariable long id) {
        return ResponseEntity.ok(bookingService.cancel(currentUserId(authentication), id));
    }

    private long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new IllegalArgumentException("Not authenticated");
        }

        return userId;
    }
}
