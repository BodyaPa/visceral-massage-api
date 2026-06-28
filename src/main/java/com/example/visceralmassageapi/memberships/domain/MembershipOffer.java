package com.example.visceralmassageapi.memberships.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "membership_offers")
public class MembershipOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MembershipOfferKind kind;

    @Column(name = "title_ua", nullable = false, length = 160)
    private String titleUa;

    @Column(name = "title_en", nullable = false, length = 160)
    private String titleEn;

    @Column(name = "description_ua", columnDefinition = "TEXT")
    private String descriptionUa;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "visits_total")
    private Integer visitsTotal;

    @Column(name = "validity_days", nullable = false)
    private int validityDays;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 100;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        var now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
