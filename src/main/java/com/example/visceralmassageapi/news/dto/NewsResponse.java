package com.example.visceralmassageapi.news.dto;

public class NewsResponse {
    private Integer id;
    private String title;
    private String content;

    public NewsResponse() {}
    public NewsResponse(Integer id, String title, String content) {
        this.id = id; this.title = title; this.content = content;
    }

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
}
