package com.example.visceralmassageapi.site.domain;

import com.example.visceralmassageapi.auth.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "site_settings")
public class SiteSettings {

    public static final short SINGLETON_ID = 1;

    @Id
    private Short id = SINGLETON_ID;

    @Column(name = "footer_body_ua", columnDefinition = "TEXT")
    private String footerBodyUa;

    @Column(name = "footer_body_en", columnDefinition = "TEXT")
    private String footerBodyEn;

    @Column(name = "home_intro_ua", columnDefinition = "TEXT")
    private String homeIntroUa;

    @Column(name = "home_intro_en", columnDefinition = "TEXT")
    private String homeIntroEn;

    @Column(name = "about_body_ua", columnDefinition = "TEXT")
    private String aboutBodyUa;

    @Column(name = "about_body_en", columnDefinition = "TEXT")
    private String aboutBodyEn;

    @Column(name = "contact_body_ua", columnDefinition = "TEXT")
    private String contactBodyUa;

    @Column(name = "contact_body_en", columnDefinition = "TEXT")
    private String contactBodyEn;

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
