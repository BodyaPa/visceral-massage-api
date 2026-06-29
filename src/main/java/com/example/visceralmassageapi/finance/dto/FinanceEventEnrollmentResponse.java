package com.example.visceralmassageapi.finance.dto;

import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FinanceEventEnrollmentResponse(
        long id,
        FixedEventEnrollmentStatus status,
        long userId,
        String clientName,
        String clientContact,
        long eventId,
        long serviceId,
        String serviceTitleUa,
        String serviceTitleEn,
        String externalPaymentUrl,
        Long membershipPurchaseId,
        boolean paidWithMembership,
        BigDecimal bookedPrice,
        boolean paymentConfirmed,
        OffsetDateTime paymentConfirmedAt,
        Long paymentConfirmedByUserId,
        long specialistId,
        String specialistName,
        Long officeId,
        String officeName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
