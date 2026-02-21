package com.example.visceralmassageapi.articles.service;

import com.example.visceralmassageapi.articles.dto.ArticleCreateRequest;
import com.example.visceralmassageapi.articles.dto.ArticleResponse;
import com.example.visceralmassageapi.articles.dto.ArticleUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ArticleService {
    Page<ArticleResponse> findAll(Pageable pageable);
    ArticleResponse findById(Integer id);
    ArticleResponse create(ArticleCreateRequest request);
    ArticleResponse updatePut(Integer id, ArticleUpdateRequest request);
    ArticleResponse updatePatch(Integer id, ArticleUpdateRequest request);
    void delete(Integer id);
}