package com.example.visceralmassageapi.booking.dto;

import com.example.visceralmassageapi.booking.domain.BookingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingResponse(
        long id,
        BookingStatus status,
        long serviceId,
        String serviceTitleUa,
        String serviceTitleEn,
        long specialistId,
        String specialistName,
        Long officeId,
        String officeName,
        String officeAddress,
        String officeDirections,
        UUID officePhotoMediaId,
        String officePhotoMediaUrl,
        UUID officeVideoMediaId,
        String officeVideoMediaUrl,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        boolean reminderOptIn,
        String externalPaymentUrl
) {
}
