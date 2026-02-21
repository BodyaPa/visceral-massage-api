package com.example.visceralmassageapi.articles.dto;

import jakarta.validation.constraints.Size;

public class ArticleUpdateRequest {

    @Size(max = 255)
    private String title;

    @Size(max = 255)
    private String content;

    public ArticleUpdateRequest() {}
    public ArticleUpdateRequest(String title, String content) {
        this.title = title; this.content = content;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
}
