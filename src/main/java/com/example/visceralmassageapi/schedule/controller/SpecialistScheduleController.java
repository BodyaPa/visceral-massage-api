package com.example.visceralmassageapi.schedule.controller;

import com.example.visceralmassageapi.booking.dto.SpecialistBookingResponse;
import com.example.visceralmassageapi.booking.dto.ManualBookingRequest;
import com.example.visceralmassageapi.booking.service.BookingService;
import com.example.visceralmassageapi.schedule.dto.SpecialistAvailabilityRequest;
import com.example.visceralmassageapi.schedule.dto.SpecialistAvailabilityResponse;
import com.example.visceralmassageapi.schedule.service.SpecialistScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/schedule")
@RequiredArgsConstructor
public class SpecialistScheduleController {

    private final SpecialistScheduleService specialistScheduleService;
    private final BookingService bookingService;

    @GetMapping("/availability")
    public List<SpecialistAvailabilityResponse> listAvailability(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return specialistScheduleService.listAvailability(currentUserId(authentication), from, to);
    }

    @GetMapping("/bookings")
    public List<SpecialistBookingResponse> listBookings(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return bookingService.listSpecialistBookings(currentUserId(authentication), from, to);
    }

    @PostMapping("/bookings")
    public ResponseEntity<SpecialistBookingResponse> createManualBooking(
            Authentication authentication,
            @Valid @RequestBody ManualBookingRequest request
    ) {
        return ResponseEntity.ok(bookingService.createManual(currentUserId(authentication), request));
    }

    @PostMapping("/availability")
    public ResponseEntity<SpecialistAvailabilityResponse> createAvailability(
            Authentication authentication,
            @Valid @RequestBody SpecialistAvailabilityRequest request
    ) {
        return ResponseEntity.ok(specialistScheduleService.createAvailability(currentUserId(authentication), request));
    }

    @DeleteMapping("/availability/{id}")
    public ResponseEntity<Void> deleteAvailability(Authentication authentication, @PathVariable long id) {
        specialistScheduleService.deleteAvailability(currentUserId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    private long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new IllegalArgumentException("Not authenticated");
        }

        return userId;
    }
}
