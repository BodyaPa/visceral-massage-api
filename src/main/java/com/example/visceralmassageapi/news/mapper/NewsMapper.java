package com.example.visceralmassageapi.news.mapper;

import com.example.visceralmassageapi.news.dto.NewsCreateRequest;
import com.example.visceralmassageapi.news.dto.LocalizedNewsResponse;
import com.example.visceralmassageapi.news.dto.NewsLocale;
import com.example.visceralmassageapi.news.dto.NewsResponse;
import com.example.visceralmassageapi.news.entity.NewsItem;

public class NewsMapper {
    public static NewsItem toEntity(NewsCreateRequest req) {
        return new NewsItem(
                null,
                req.getTitleUa(),
                req.getContentUa(),
                req.getTitleEn(),
                req.getContentEn()
        );
    }

    public static NewsResponse toResponse(NewsItem entity) {
        return new NewsResponse(
                entity.getId(),
                entity.getTitleUa(),
                entity.getContentUa(),
                entity.getTitleEn(),
                entity.getContentEn(),
                entity.getStatus(),
                entity.getCoverMediaId(),
                entity.getCoverDisplayMode(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getPublishedAt()
        );
    }

    public static LocalizedNewsResponse toLocalizedResponse(NewsItem entity, NewsLocale locale) {
        String title = locale == NewsLocale.UA ? entity.getTitleUa() : entity.getTitleEn();
        String content = locale == NewsLocale.UA ? entity.getContentUa() : entity.getContentEn();
        String coverImageUrl = entity.getCoverMediaId() == null
                ? null
                : "/api/news/" + entity.getId() + "/media/" + entity.getCoverMediaId() + "/content";
        return new LocalizedNewsResponse(
                entity.getId(),
                title,
                content,
                coverImageUrl,
                coverImageUrl == null ? null : title,
                entity.getCoverDisplayMode()
        );
    }
}
