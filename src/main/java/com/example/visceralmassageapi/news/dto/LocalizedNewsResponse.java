package com.example.visceralmassageapi.news.dto;

import com.example.visceralmassageapi.news.entity.CoverDisplayMode;

public class LocalizedNewsResponse {
    private final Integer id;
    private final String title;
    private final String content;
    private final String coverImageUrl;
    private final String coverImageAlt;
    private final CoverDisplayMode coverDisplayMode;

    public LocalizedNewsResponse(Integer id, String title, String content, String coverImageUrl, String coverImageAlt,
                                 CoverDisplayMode coverDisplayMode) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.coverImageUrl = coverImageUrl;
        this.coverImageAlt = coverImageAlt;
        this.coverDisplayMode = coverDisplayMode;
    }

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public String getCoverImageAlt() { return coverImageAlt; }
    public CoverDisplayMode getCoverDisplayMode() { return coverDisplayMode; }
}
