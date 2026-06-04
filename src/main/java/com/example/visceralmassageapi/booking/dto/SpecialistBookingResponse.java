package com.example.visceralmassageapi.booking.dto;

import com.example.visceralmassageapi.booking.domain.BookingStatus;

import java.time.OffsetDateTime;

public record SpecialistBookingResponse(
        long id,
        BookingStatus status,
        long clientId,
        String clientName,
        String clientContact,
        long serviceId,
        String serviceTitleUa,
        Long officeId,
        String officeName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        boolean reminderOptIn
) {
}
