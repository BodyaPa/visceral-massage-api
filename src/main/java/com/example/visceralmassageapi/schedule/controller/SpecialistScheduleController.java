package com.example.visceralmassageapi.schedule.controller;

import com.example.visceralmassageapi.booking.dto.SpecialistBookingResponse;
import com.example.visceralmassageapi.booking.dto.ManualBookingRequest;
import com.example.visceralmassageapi.booking.domain.BookingStatus;
import com.example.visceralmassageapi.booking.service.BookingService;
import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus;
import com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus;
import com.example.visceralmassageapi.schedule.dto.SpecialistAvailabilityRequest;
import com.example.visceralmassageapi.schedule.dto.SpecialistAvailabilityResponse;
import com.example.visceralmassageapi.schedule.dto.DayPlanCopyRequest;
import com.example.visceralmassageapi.schedule.dto.DayPlanCopyResponse;
import com.example.visceralmassageapi.schedule.dto.SpecialistFixedEventEnrollmentResponse;
import com.example.visceralmassageapi.schedule.dto.SpecialistFixedEventRequest;
import com.example.visceralmassageapi.schedule.dto.SpecialistFixedEventResponse;
import com.example.visceralmassageapi.schedule.service.FixedEventService;
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
    private final FixedEventService fixedEventService;

    @GetMapping("/availability")
    public List<SpecialistAvailabilityResponse> listAvailability(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) Long specialistId,
            @RequestParam(required = false) ScheduleBlockStatus status,
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) Long serviceId
    ) {
        return specialistScheduleService.listAvailability(currentUserId(authentication), from, to, specialistId, status, officeId, serviceId);
    }

    @GetMapping("/bookings")
    public List<SpecialistBookingResponse> listBookings(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) Long specialistId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) Long serviceId
    ) {
        return bookingService.listSpecialistBookings(currentUserId(authentication), from, to, specialistId, status, officeId, serviceId);
    }

    @GetMapping("/events")
    public List<SpecialistFixedEventResponse> listEvents(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) Long specialistId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) Long serviceId
    ) {
        return fixedEventService.listOwn(currentUserId(authentication), from, to, specialistId, active, officeId, serviceId);
    }

    @GetMapping("/events/enrollments")
    public List<SpecialistFixedEventEnrollmentResponse> listEventEnrollments(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) Long specialistId,
            @RequestParam(required = false) Boolean eventActive,
            @RequestParam(required = false) FixedEventEnrollmentStatus status,
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) Long serviceId
    ) {
        return fixedEventService.listOwnEnrollments(currentUserId(authentication), from, to, specialistId, eventActive, status, officeId, serviceId);
    }

    @PostMapping("/events")
    public ResponseEntity<SpecialistFixedEventResponse> createEvent(
            Authentication authentication,
            @Valid @RequestBody SpecialistFixedEventRequest request
    ) {
        return ResponseEntity.ok(fixedEventService.createOwn(currentUserId(authentication), request));
    }

    @PutMapping("/events/{id}")
    public SpecialistFixedEventResponse updateEvent(
            Authentication authentication,
            @PathVariable long id,
            @Valid @RequestBody SpecialistFixedEventRequest request
    ) {
        return fixedEventService.updateOwn(currentUserId(authentication), id, request);
    }

    @PostMapping("/bookings")
    public ResponseEntity<SpecialistBookingResponse> createManualBooking(
            Authentication authentication,
            @Valid @RequestBody ManualBookingRequest request
    ) {
        return ResponseEntity.ok(bookingService.createManual(currentUserId(authentication), request));
    }

    @PostMapping("/day-copy")
    public ResponseEntity<DayPlanCopyResponse> copyDayPlan(
            Authentication authentication,
            @Valid @RequestBody DayPlanCopyRequest request
    ) {
        return ResponseEntity.ok(specialistScheduleService.copyDayPlan(currentUserId(authentication), request));
    }

    @PostMapping("/availability")
    public ResponseEntity<SpecialistAvailabilityResponse> createAvailability(
            Authentication authentication,
            @Valid @RequestBody SpecialistAvailabilityRequest request
    ) {
        return ResponseEntity.ok(specialistScheduleService.createAvailability(currentUserId(authentication), request));
    }

    @PutMapping("/availability/{id}")
    public SpecialistAvailabilityResponse updateAvailability(
            Authentication authentication,
            @PathVariable long id,
            @Valid @RequestBody SpecialistAvailabilityRequest request
    ) {
        return specialistScheduleService.updateAvailability(currentUserId(authentication), id, request);
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
