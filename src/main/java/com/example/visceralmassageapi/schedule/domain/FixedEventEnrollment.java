package com.example.visceralmassageapi.schedule.domain;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.memberships.domain.MembershipPurchase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "fixed_event_enrollments")
public class FixedEventEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private FixedEvent event;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FixedEventEnrollmentStatus status;

    @Column(name = "reminder_opt_in", nullable = false)
    private boolean reminderOptIn;

    @Column(name = "reminder_sent_at")
    private OffsetDateTime reminderSentAt;

    @ManyToOne
    @JoinColumn(name = "membership_purchase_id")
    private MembershipPurchase membershipPurchase;

    @Column(name = "payment_confirmed_at")
    private OffsetDateTime paymentConfirmedAt;

    @ManyToOne
    @JoinColumn(name = "payment_confirmed_by_user_id")
    private User paymentConfirmedBy;

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
