package com.example.visceralmassageapi.news.dto;

import jakarta.validation.constraints.Size;

public class NewsUpdateRequest {

    @Size(max = 255)
    private String title;

    @Size(max = 255)
    private String content;

    public NewsUpdateRequest() {}
    public NewsUpdateRequest(String title, String content) {
        this.title = title; this.content = content;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
}
