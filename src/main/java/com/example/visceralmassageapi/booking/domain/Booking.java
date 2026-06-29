package com.example.visceralmassageapi.booking.domain;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.memberships.domain.MembershipPurchase;
import com.example.visceralmassageapi.offices.entity.Office;
import com.example.visceralmassageapi.schedule.domain.SpecialistAvailabilityBlock;
import com.example.visceralmassageapi.services.entity.ServiceOffering;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "specialist_user_id", nullable = false)
    private User specialist;

    @ManyToOne(optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering service;

    @ManyToOne
    @JoinColumn(name = "office_id")
    private Office office;

    @ManyToOne(optional = false)
    @JoinColumn(name = "availability_block_id", nullable = false)
    private SpecialistAvailabilityBlock availabilityBlock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private BookingStatus status;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(name = "booked_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal bookedPrice;

    @ManyToOne
    @JoinColumn(name = "membership_purchase_id")
    private MembershipPurchase membershipPurchase;

    @Enumerated(EnumType.STRING)
    @Column(name = "specialist_payout_status", nullable = false, length = 24)
    private SpecialistPayoutStatus specialistPayoutStatus = SpecialistPayoutStatus.PENDING;

    @Column(name = "specialist_payout_paid_at")
    private OffsetDateTime specialistPayoutPaidAt;

    @ManyToOne
    @JoinColumn(name = "specialist_payout_paid_by_user_id")
    private User specialistPayoutPaidBy;

    @Column(name = "reminder_opt_in", nullable = false)
    private boolean reminderOptIn;

    @Column(name = "reminder_sent_at")
    private OffsetDateTime reminderSentAt;

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
