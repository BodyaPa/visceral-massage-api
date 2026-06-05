package com.example.visceralmassageapi.schedule.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SpecialistFixedEventResponse(
        long id,
        long serviceId,
        String serviceTitle,
        Long officeId,
        String officeName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        int capacity,
        int enrolledCount,
        BigDecimal price,
        String note,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
