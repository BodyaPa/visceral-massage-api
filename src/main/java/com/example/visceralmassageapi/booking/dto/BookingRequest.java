package com.example.visceralmassageapi.booking.dto;

import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotNull Long availabilityBlockId,
        @NotNull Long serviceId,
        boolean reminderOptIn
) {
}
