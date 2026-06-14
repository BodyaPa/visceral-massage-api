package com.example.visceralmassageapi.schedule.dto;

import java.time.OffsetDateTime;

public record PublicScheduleAvailabilityResponse(
        long id,
        long specialistId,
        String specialistName,
        Long officeId,
        String officeName,
        String officeAddress,
        String officeDirections,
        String officePhotoUrl,
        String officeVideoUrl,
        String officeGoogleMapsUrl,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt
) {
}
