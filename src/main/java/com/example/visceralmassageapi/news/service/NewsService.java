package com.example.visceralmassageapi.news.service;

import com.example.visceralmassageapi.news.dto.NewsCreateRequest;
import com.example.visceralmassageapi.news.dto.LocalizedNewsResponse;
import com.example.visceralmassageapi.news.dto.NewsLocale;
import com.example.visceralmassageapi.news.dto.NewsResponse;
import com.example.visceralmassageapi.news.dto.NewsUpdateRequest;
import com.example.visceralmassageapi.news.entity.CoverDisplayMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NewsService {
    Page<LocalizedNewsResponse> findAll(NewsLocale locale, Pageable pageable);
    LocalizedNewsResponse findById(Integer id, NewsLocale locale);
    Page<NewsResponse> findAllForAdmin(Pageable pageable);
    NewsResponse findByIdForAdmin(Integer id);
    NewsResponse create(NewsCreateRequest request);
    NewsResponse updatePut(Integer id, NewsUpdateRequest request);
    NewsResponse updatePatch(Integer id, NewsUpdateRequest request);
    NewsResponse publish(Integer id);
    NewsResponse unpublish(Integer id);
    NewsResponse archive(Integer id);
    NewsResponse restore(Integer id);
    NewsResponse setCover(Integer id, UUID mediaId);
    NewsResponse setCoverDisplayMode(Integer id, CoverDisplayMode displayMode);
    NewsResponse clearCover(Integer id);
    void clearCoverIfMatches(Integer id, UUID mediaId);
    void requirePublished(Integer id);
    void delete(Integer id);
}
