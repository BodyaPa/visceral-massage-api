package com.example.visceralmassageapi.schedule.dto;

import com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record SpecialistAvailabilityRequest(
        Long officeId,
        @NotNull ScheduleBlockStatus status,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt,
        @Size(max = 500) String notes
) {
}
