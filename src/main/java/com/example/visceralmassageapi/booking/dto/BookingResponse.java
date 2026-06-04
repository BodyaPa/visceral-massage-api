package com.example.visceralmassageapi.booking.dto;

import com.example.visceralmassageapi.booking.domain.BookingStatus;

import java.time.OffsetDateTime;

public record BookingResponse(
        long id,
        BookingStatus status,
        long serviceId,
        String serviceTitleUa,
        long specialistId,
        String specialistName,
        Long officeId,
        String officeName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        boolean reminderOptIn,
        String externalPaymentUrl
) {
}
