package com.example.visceralmassageapi.schedule.domain;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.offices.entity.Office;
import com.example.visceralmassageapi.services.entity.ServiceOffering;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "fixed_events")
public class FixedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering service;

    @ManyToOne(optional = false)
    @JoinColumn(name = "specialist_user_id", nullable = false)
    private User specialist;

    @ManyToOne
    @JoinColumn(name = "office_id")
    private Office office;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime endsAt;

    @Column(nullable = false)
    private Integer capacity;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false)
    private boolean active = true;

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
