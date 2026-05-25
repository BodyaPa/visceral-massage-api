package com.example.visceralmassageapi.news.entity;

import jakarta.persistence.*;

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
}
