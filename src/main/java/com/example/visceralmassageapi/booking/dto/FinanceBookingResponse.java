package com.example.visceralmassageapi.booking.dto;

import com.example.visceralmassageapi.booking.domain.BookingStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FinanceBookingResponse(
        long id,
        BookingStatus status,
        long userId,
        String clientName,
        String clientContact,
        long serviceId,
        String serviceTitleUa,
        BigDecimal bookedPrice,
        long specialistId,
        String specialistName,
        Long officeId,
        String officeName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        boolean reminderOptIn,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
