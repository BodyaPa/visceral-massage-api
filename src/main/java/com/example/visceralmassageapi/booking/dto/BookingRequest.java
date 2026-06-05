package com.example.visceralmassageapi.booking.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record BookingRequest(
        @NotNull Long availabilityBlockId,
        @NotNull Long serviceId,
        OffsetDateTime startsAt,
        boolean reminderOptIn
) {
}
