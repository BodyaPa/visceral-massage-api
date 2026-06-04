package com.example.visceralmassageapi.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ManualBookingRequest(
        @NotBlank String clientIdentifier,
        @NotNull Long availabilityBlockId,
        @NotNull Long serviceId,
        boolean reminderOptIn
) {
}
