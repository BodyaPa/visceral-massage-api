package com.example.visceralmassageapi.mapper;

import com.example.visceralmassageapi.dto.ArticleCreateRequest;
import com.example.visceralmassageapi.dto.ArticleResponse;
import com.example.visceralmassageapi.entity.Article;

public class ArticleMapper {
    public static Article toEntity(ArticleCreateRequest req) {
        return new Article(null, req.getTitle(), req.getContent());
    }

    public static ArticleResponse toResponse(Article entity) {
        return new ArticleResponse(entity.getId(), entity.getTitle(), entity.getContent());
    }
}
