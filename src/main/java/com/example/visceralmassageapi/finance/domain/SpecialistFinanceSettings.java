package com.example.visceralmassageapi.finance.domain;

import com.example.visceralmassageapi.auth.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "specialist_finance_settings")
public class SpecialistFinanceSettings {

    @Id
    @Column(name = "specialist_user_id")
    private Long specialistUserId;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "specialist_user_id")
    private User specialist;

    @Column(name = "specialist_share_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal specialistSharePercent = BigDecimal.ZERO;

    @ManyToOne
    @JoinColumn(name = "updated_by_user_id")
    private User updatedBy;

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
