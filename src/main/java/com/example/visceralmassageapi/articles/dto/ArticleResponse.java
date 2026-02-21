package com.example.visceralmassageapi.articles.dto;

public class ArticleResponse {
    private Integer id;
    private String title;
    private String content;

    public ArticleResponse() {}
    public ArticleResponse(Integer id, String title, String content) {
        this.id = id; this.title = title; this.content = content;
    }

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
}