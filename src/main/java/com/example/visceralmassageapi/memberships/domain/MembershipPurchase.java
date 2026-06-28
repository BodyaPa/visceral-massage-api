package com.example.visceralmassageapi.memberships.domain;

import com.example.visceralmassageapi.auth.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "membership_purchases")
public class MembershipPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private MembershipOffer offer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private MembershipPurchaseStatus status;

    @Column(name = "price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceSnapshot;

    @Column(name = "visits_total")
    private Integer visitsTotal;

    @Column(name = "visits_remaining")
    private Integer visitsRemaining;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @ManyToOne
    @JoinColumn(name = "confirmed_by_user_id")
    private User confirmedBy;

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
