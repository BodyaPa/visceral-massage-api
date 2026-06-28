package com.example.visceralmassageapi.memberships.dto;

import com.example.visceralmassageapi.memberships.domain.MembershipOfferKind;
import com.example.visceralmassageapi.memberships.domain.MembershipPurchaseStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MembershipPurchaseResponse(
        Long id,
        MembershipPurchaseStatus status,
        Long userId,
        Long offerId,
        String offerCode,
        MembershipOfferKind offerKind,
        String titleUa,
        String titleEn,
        BigDecimal priceSnapshot,
        Integer visitsTotal,
        Integer visitsRemaining,
        OffsetDateTime activatedAt,
        OffsetDateTime expiresAt,
        Long confirmedByUserId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
