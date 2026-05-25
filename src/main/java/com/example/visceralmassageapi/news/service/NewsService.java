package com.example.visceralmassageapi.news.service;

import com.example.visceralmassageapi.news.dto.NewsCreateRequest;
import com.example.visceralmassageapi.news.dto.LocalizedNewsResponse;
import com.example.visceralmassageapi.news.dto.NewsLocale;
import com.example.visceralmassageapi.news.dto.NewsResponse;
import com.example.visceralmassageapi.news.dto.NewsUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NewsService {
    Page<LocalizedNewsResponse> findAll(NewsLocale locale, Pageable pageable);
    LocalizedNewsResponse findById(Integer id, NewsLocale locale);
    Page<NewsResponse> findAllForAdmin(Pageable pageable);
    NewsResponse findByIdForAdmin(Integer id);
    NewsResponse create(NewsCreateRequest request);
    NewsResponse updatePut(Integer id, NewsUpdateRequest request);
    NewsResponse updatePatch(Integer id, NewsUpdateRequest request);
    void delete(Integer id);
}
