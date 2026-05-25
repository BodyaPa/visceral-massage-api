package com.example.visceralmassageapi.news.dto;

import jakarta.validation.constraints.Size;

public class NewsUpdateRequest {

    @Size(max = 255)
    private String titleUa;

    @Size(max = 100000)
    private String contentUa;

    @Size(max = 255)
    private String titleEn;

    @Size(max = 100000)
    private String contentEn;

    public NewsUpdateRequest() {}
    public NewsUpdateRequest(String titleUa, String contentUa, String titleEn, String contentEn) {
        this.titleUa = titleUa;
        this.contentUa = contentUa;
        this.titleEn = titleEn;
        this.contentEn = contentEn;
    }

    public String getTitleUa() { return titleUa; }
    public String getContentUa() { return contentUa; }
    public String getTitleEn() { return titleEn; }
    public String getContentEn() { return contentEn; }
}
