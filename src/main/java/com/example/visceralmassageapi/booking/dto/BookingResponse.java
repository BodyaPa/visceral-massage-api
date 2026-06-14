package com.example.visceralmassageapi.booking.dto;

import com.example.visceralmassageapi.booking.domain.BookingStatus;

import java.time.OffsetDateTime;

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
        String officePhotoUrl,
        String officeVideoUrl,
        String officeGoogleMapsUrl,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        boolean reminderOptIn,
        String externalPaymentUrl
) {
}
