package com.example.visceralmassageapi.schedule.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicFixedEventResponse(
        long id,
        long serviceId,
        String title,
        String description,
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
        OffsetDateTime endsAt,
        int capacity,
        int enrolledCount,
        int remainingPlaces,
        boolean full,
        boolean enrolled,
        BigDecimal price,
        String note
) {
}
