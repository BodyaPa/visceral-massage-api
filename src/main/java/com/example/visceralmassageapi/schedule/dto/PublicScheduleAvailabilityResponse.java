package com.example.visceralmassageapi.schedule.dto;

import java.time.OffsetDateTime;

public record PublicScheduleAvailabilityResponse(
        long id,
        long specialistId,
        String specialistName,
        Long officeId,
        String officeName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt
) {
}
