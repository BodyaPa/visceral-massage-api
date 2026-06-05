package com.example.visceralmassageapi.schedule.dto;

import java.time.OffsetDateTime;

public record PublicScheduleUnavailableResponse(
        String id,
        String status,
        long specialistId,
        String specialistName,
        Long officeId,
        String officeName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt
) {
}
