package com.example.visceralmassageapi.news.mapper;

import com.example.visceralmassageapi.news.dto.NewsCreateRequest;
import com.example.visceralmassageapi.news.dto.NewsResponse;
import com.example.visceralmassageapi.news.entity.NewsItem;

public class NewsMapper {
    public static NewsItem toEntity(NewsCreateRequest req) {
        return new NewsItem(null, req.getTitle(), req.getContent());
    }

    public static NewsResponse toResponse(NewsItem entity) {
        return new NewsResponse(entity.getId(), entity.getTitle(), entity.getContent());
    }
}
