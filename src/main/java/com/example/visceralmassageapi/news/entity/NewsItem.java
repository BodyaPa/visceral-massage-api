package com.example.visceralmassageapi.news.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "news")
public class NewsItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "title_ua", length = 255)
    private String titleUa;

    @Column(name = "content_ua", columnDefinition = "TEXT")
    private String contentUa;

    @Column(name = "title_en", length = 255)
    private String titleEn;

    @Column(name = "content_en", columnDefinition = "TEXT")
    private String contentEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private NewsStatus status = NewsStatus.DRAFT;

    @Column(name = "cover_media_id")
    private UUID coverMediaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cover_display_mode", nullable = false, length = 16)
    private CoverDisplayMode coverDisplayMode = CoverDisplayMode.FILL;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    public NewsItem() {}

    public NewsItem(Integer id, String titleUa, String contentUa, String titleEn, String contentEn) {
        this.id = id;
        this.titleUa = titleUa;
        this.contentUa = contentUa;
        this.titleEn = titleEn;
        this.contentEn = contentEn;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitleUa() { return titleUa; }
    public void setTitleUa(String titleUa) { this.titleUa = titleUa; }

    public String getContentUa() { return contentUa; }
    public void setContentUa(String contentUa) { this.contentUa = contentUa; }

    public String getTitleEn() { return titleEn; }
    public void setTitleEn(String titleEn) { this.titleEn = titleEn; }

    public String getContentEn() { return contentEn; }
    public void setContentEn(String contentEn) { this.contentEn = contentEn; }

    public NewsStatus getStatus() { return status; }
    public void setStatus(NewsStatus status) { this.status = status; }

    public UUID getCoverMediaId() { return coverMediaId; }
    public void setCoverMediaId(UUID coverMediaId) { this.coverMediaId = coverMediaId; }

    public CoverDisplayMode getCoverDisplayMode() { return coverDisplayMode; }
    public void setCoverDisplayMode(CoverDisplayMode coverDisplayMode) { this.coverDisplayMode = coverDisplayMode; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }
}
