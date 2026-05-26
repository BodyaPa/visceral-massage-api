package com.example.visceralmassageapi.news.dto;

import com.example.visceralmassageapi.news.entity.CoverDisplayMode;
import com.example.visceralmassageapi.news.entity.NewsStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class NewsResponse {
    private Integer id;
    private String titleUa;
    private String contentUa;
    private String titleEn;
    private String contentEn;
    private NewsStatus status;
    private UUID coverMediaId;
    private CoverDisplayMode coverDisplayMode;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime publishedAt;

    public NewsResponse() {}
    public NewsResponse(Integer id, String titleUa, String contentUa, String titleEn, String contentEn,
                        NewsStatus status, UUID coverMediaId, CoverDisplayMode coverDisplayMode, OffsetDateTime createdAt,
                        OffsetDateTime updatedAt, OffsetDateTime publishedAt) {
        this.id = id;
        this.titleUa = titleUa;
        this.contentUa = contentUa;
        this.titleEn = titleEn;
        this.contentEn = contentEn;
        this.status = status;
        this.coverMediaId = coverMediaId;
        this.coverDisplayMode = coverDisplayMode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.publishedAt = publishedAt;
    }

    public Integer getId() { return id; }
    public String getTitleUa() { return titleUa; }
    public String getContentUa() { return contentUa; }
    public String getTitleEn() { return titleEn; }
    public String getContentEn() { return contentEn; }
    public NewsStatus getStatus() { return status; }
    public UUID getCoverMediaId() { return coverMediaId; }
    public CoverDisplayMode getCoverDisplayMode() { return coverDisplayMode; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
}
