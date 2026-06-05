package com.example.visceralmassageapi.schedule.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record SpecialistFixedEventRequest(
        @NotNull Long serviceId,
        Long officeId,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt,
        @NotNull @Min(1) Integer capacity,
        @Size(max = 1000) String note,
        boolean active
) {
}
