package com.example.visceralmassageapi.schedule.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record DayPlanCopyResponse(
        long specialistId,
        LocalDate sourceDate,
        List<LocalDate> targetDates,
        int copiedAvailabilityCount,
        int copiedEventCount,
        List<Conflict> conflicts
) {
    public record Conflict(
            LocalDate targetDate,
            String itemType,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String reason
    ) {
    }
}
