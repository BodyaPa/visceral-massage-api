package com.example.visceralmassageapi.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record ManualBookingRequest(
        @NotBlank String clientIdentifier,
        @NotNull Long availabilityBlockId,
        @NotNull Long serviceId,
        OffsetDateTime startsAt,
        boolean reminderOptIn
) {
}
