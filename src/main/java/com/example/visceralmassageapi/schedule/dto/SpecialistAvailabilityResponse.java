package com.example.visceralmassageapi.schedule.dto;

import com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus;

import java.time.OffsetDateTime;

public record SpecialistAvailabilityResponse(
        long id,
        long specialistId,
        String specialistName,
        Long officeId,
        String officeName,
        ScheduleBlockStatus status,
        com.example.visceralmassageapi.schedule.domain.ScheduleBlockType itemType,
        Long serviceId,
        String serviceTitle,
        Integer capacity,
        boolean booked,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
