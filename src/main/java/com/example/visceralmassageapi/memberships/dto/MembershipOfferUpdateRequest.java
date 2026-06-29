package com.example.visceralmassageapi.memberships.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

public record MembershipOfferUpdateRequest(
        @NotBlank @Size(max = 160) String titleUa,
        @Size(max = 160) String titleEn,
        @Size(max = 5000) String descriptionUa,
        @Size(max = 5000) String descriptionEn,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @Min(0) Integer visitsTotal,
        @Min(1) int validityDays,
        boolean active,
        Set<Long> eligibleServiceIds
) {
}
