package com.example.visceralmassageapi.articles.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ArticleCreateRequest {
    @NotBlank @Size(max = 255)
    private String title;

    @NotBlank @Size(max = 255)
    private String content;

    public ArticleCreateRequest() {}
    public ArticleCreateRequest(String title, String content) {
        this.title = title; this.content = content;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
}

