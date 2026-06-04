package com.example.visceralmassageapi.schedule.controller;

import com.example.visceralmassageapi.schedule.dto.PublicScheduleAvailabilityResponse;
import com.example.visceralmassageapi.schedule.service.SpecialistScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/availability")
    public List<PublicScheduleAvailabilityResponse> listPublicAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) Long specialistId
    ) {
        return specialistScheduleService.listPublicAvailability(from, to, officeId, specialistId);
    }
}
