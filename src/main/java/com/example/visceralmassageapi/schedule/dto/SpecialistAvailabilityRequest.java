package com.example.visceralmassageapi.schedule.dto;

import com.example.visceralmassageapi.schedule.domain.ScheduleBlockType;
import com.example.visceralmassageapi.schedule.domain.ScheduleBlockStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record SpecialistAvailabilityRequest(
        Long specialistId,
        Long officeId,
        @NotNull ScheduleBlockStatus status,
        ScheduleBlockType itemType,
        Long serviceId,
        @Min(1) Integer capacity,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt,
        @Size(max = 500) String notes
) {
}
