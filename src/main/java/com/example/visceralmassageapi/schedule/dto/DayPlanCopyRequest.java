package com.example.visceralmassageapi.schedule.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record DayPlanCopyRequest(
        Long specialistId,
        @NotNull LocalDate sourceDate,
        @NotEmpty List<@NotNull LocalDate> targetDates,
        boolean includeAvailability,
        boolean includeFixedEvents
) {
}
