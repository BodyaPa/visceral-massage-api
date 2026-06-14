package com.example.visceralmassageapi.schedule.dto;

import java.time.OffsetDateTime;

public record PublicScheduleAvailabilityResponse(
        long id,
        long specialistId,
        String specialistName,
        Long officeId,
        String officeName,
        String officeAddress,
        String officeLocationDetails,
        String officeDescription,
        String officeDirections,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt
) {
}
