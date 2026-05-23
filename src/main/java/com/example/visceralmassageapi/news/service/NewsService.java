package com.example.visceralmassageapi.news.service;

import com.example.visceralmassageapi.news.dto.NewsCreateRequest;
import com.example.visceralmassageapi.news.dto.NewsResponse;
import com.example.visceralmassageapi.news.dto.NewsUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NewsService {
    Page<NewsResponse> findAll(Pageable pageable);
    NewsResponse findById(Integer id);
    NewsResponse create(NewsCreateRequest request);
    NewsResponse updatePut(Integer id, NewsUpdateRequest request);
    NewsResponse updatePatch(Integer id, NewsUpdateRequest request);
    void delete(Integer id);
}
