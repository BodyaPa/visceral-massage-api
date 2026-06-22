package com.example.visceralmassageapi.schedule.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus;

public record PublicFixedEventResponse(
        long id,
        long serviceId,
        String title,
        String description,
        long specialistId,
        String specialistName,
        UUID specialistAvatarMediaId,
        String specialistAvatarMediaUrl,
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
        FixedEventEnrollmentStatus enrollmentStatus,
        BigDecimal price,
        String note
) {
}
