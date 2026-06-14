package com.example.visceralmassageapi.schedule.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
        String officePhotoUrl,
        String officeVideoUrl,
        String officeGoogleMapsUrl,
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
