package com.example.visceralmassageapi.memberships.dto;

import com.example.visceralmassageapi.memberships.domain.MembershipOfferKind;

import java.math.BigDecimal;

public record MembershipOfferResponse(
        Long id,
        String code,
        MembershipOfferKind kind,
        String titleUa,
        String titleEn,
        String descriptionUa,
        String descriptionEn,
        BigDecimal price,
        Integer visitsTotal,
        int validityDays,
        boolean active
) {
}
