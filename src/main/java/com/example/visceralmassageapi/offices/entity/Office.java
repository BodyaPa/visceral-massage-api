package com.example.visceralmassageapi.offices.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "offices")
public class Office {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "location_details", columnDefinition = "TEXT")
    private String locationDetails;

    @Column(columnDefinition = "TEXT")
    private String directions;

    @Column(name = "google_maps_url", length = 2048)
    private String googleMapsUrl;

    @Column(name = "photo_media_id")
    private UUID photoMediaId;

    @Column(name = "video_media_id")
    private UUID videoMediaId;

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
