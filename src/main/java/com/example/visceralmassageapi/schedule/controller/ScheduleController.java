package com.example.visceralmassageapi.schedule.controller;

import com.example.visceralmassageapi.schedule.dto.PublicScheduleAvailabilityResponse;
import com.example.visceralmassageapi.schedule.dto.FixedEventEnrollmentRequest;
import com.example.visceralmassageapi.schedule.dto.PublicFixedEventResponse;
import com.example.visceralmassageapi.schedule.dto.PublicScheduleUnavailableResponse;
import com.example.visceralmassageapi.schedule.service.FixedEventService;
import com.example.visceralmassageapi.schedule.service.SpecialistScheduleService;
import com.example.visceralmassageapi.services.dto.ServiceLocale;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final SpecialistScheduleService specialistScheduleService;
    private final FixedEventService fixedEventService;

    @GetMapping("/availability")
    public List<PublicScheduleAvailabilityResponse> listPublicAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) Long specialistId,
            @RequestParam(required = false) Long serviceId
    ) {
        return specialistScheduleService.listPublicAvailability(from, to, officeId, specialistId, serviceId);
    }

    @GetMapping("/unavailable")
    public List<PublicScheduleUnavailableResponse> listPublicUnavailable(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) Long specialistId
    ) {
        return specialistScheduleService.listPublicUnavailable(from, to, officeId, specialistId);
    }

    @GetMapping("/events")
    public List<PublicFixedEventResponse> listPublicEvents(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) Long specialistId,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(defaultValue = "ua") String lang
    ) {
        return fixedEventService.listPublic(
                from,
                to,
                officeId,
                specialistId,
                serviceId,
                currentUserIdOrNull(authentication),
                ServiceLocale.from(lang)
        );
    }

    @GetMapping("/events/my")
    public List<PublicFixedEventResponse> listMyEvents(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "ua") String lang
    ) {
        return fixedEventService.listMyEnrollments(currentUserId(authentication), from, to, ServiceLocale.from(lang));
    }

    @PostMapping("/events/{id}/enroll")
    public ResponseEntity<PublicFixedEventResponse> enroll(
            Authentication authentication,
            @PathVariable long id,
            @RequestParam(defaultValue = "ua") String lang,
            @RequestBody FixedEventEnrollmentRequest request
    ) {
        return ResponseEntity.ok(fixedEventService.enroll(id, currentUserId(authentication), request, ServiceLocale.from(lang)));
    }

    @PostMapping("/events/{id}/cancel")
    public ResponseEntity<PublicFixedEventResponse> cancelEnrollment(
            Authentication authentication,
            @PathVariable long id,
            @RequestParam(defaultValue = "ua") String lang
    ) {
        return ResponseEntity.ok(fixedEventService.cancelEnrollment(id, currentUserId(authentication), ServiceLocale.from(lang)));
    }

    private Long currentUserIdOrNull(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            return null;
        }
        return userId;
    }

    private long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new IllegalArgumentException("Not authenticated");
        }
        return userId;
    }
}
