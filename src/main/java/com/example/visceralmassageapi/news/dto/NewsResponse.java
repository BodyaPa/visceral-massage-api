package com.example.visceralmassageapi.news.dto;

public class NewsResponse {
    private Integer id;
    private String titleUa;
    private String contentUa;
    private String titleEn;
    private String contentEn;

    public NewsResponse() {}
    public NewsResponse(Integer id, String titleUa, String contentUa, String titleEn, String contentEn) {
        this.id = id;
        this.titleUa = titleUa;
        this.contentUa = contentUa;
        this.titleEn = titleEn;
        this.contentEn = contentEn;
    }

    public Integer getId() { return id; }
    public String getTitleUa() { return titleUa; }
    public String getContentUa() { return contentUa; }
    public String getTitleEn() { return titleEn; }
    public String getContentEn() { return contentEn; }
}
