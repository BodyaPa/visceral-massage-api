package com.example.visceralmassageapi.schedule.dto;

import com.example.visceralmassageapi.schedule.domain.FixedEventEnrollmentStatus;

import java.time.OffsetDateTime;

public record SpecialistFixedEventEnrollmentResponse(
        long id,
        long eventId,
        String eventTitle,
        OffsetDateTime eventStartsAt,
        OffsetDateTime eventEndsAt,
        long clientId,
        String clientName,
        String clientContact,
        FixedEventEnrollmentStatus status,
        boolean reminderOptIn,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
