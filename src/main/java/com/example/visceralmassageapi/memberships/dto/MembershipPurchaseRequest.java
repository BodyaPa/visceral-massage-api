package com.example.visceralmassageapi.memberships.dto;

import jakarta.validation.constraints.NotNull;

public record MembershipPurchaseRequest(
        @NotNull Long offerId
) {
}
