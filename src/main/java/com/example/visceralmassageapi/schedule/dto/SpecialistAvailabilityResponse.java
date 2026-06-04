package com.example.visceralmassageapi.schedule.dto;

import com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus;

import java.time.OffsetDateTime;

public record SpecialistAvailabilityResponse(
        long id,
        Long officeId,
        String officeName,
        ScheduleBlockStatus status,
        boolean booked,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
