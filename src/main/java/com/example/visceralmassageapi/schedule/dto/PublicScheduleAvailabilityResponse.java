package com.example.visceralmassageapi.schedule.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicScheduleAvailabilityResponse(
        long id,
        long specialistId,
        String specialistName,
        Long officeId,
        String officeName,
        String officeAddress,
        String officeDirections,
        String officeGoogleMapsUrl,
        UUID officePhotoMediaId,
        String officePhotoMediaUrl,
        UUID officeVideoMediaId,
        String officeVideoMediaUrl,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt
) {
}
