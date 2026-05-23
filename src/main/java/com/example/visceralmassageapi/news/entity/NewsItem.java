package com.example.visceralmassageapi.news.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "news")
public class NewsItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 255)
    private String title;

    @Column(length = 255)
    private String content;

    public NewsItem() {}

    public NewsItem(Integer id, String title, String content) {
        this.id = id; this.title = title; this.content = content;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
