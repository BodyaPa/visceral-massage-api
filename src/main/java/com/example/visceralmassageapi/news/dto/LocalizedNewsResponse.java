package com.example.visceralmassageapi.news.dto;

public class LocalizedNewsResponse {
    private final Integer id;
    private final String title;
    private final String content;
    private final boolean translationAvailable;

    public LocalizedNewsResponse(Integer id, String title, String content, boolean translationAvailable) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.translationAvailable = translationAvailable;
    }

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isTranslationAvailable() { return translationAvailable; }
}
