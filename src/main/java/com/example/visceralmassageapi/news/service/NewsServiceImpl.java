package com.example.visceralmassageapi.news.service;

import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.news.dto.LocalizedNewsResponse;
import com.example.visceralmassageapi.news.dto.NewsCreateRequest;
import com.example.visceralmassageapi.news.dto.NewsLocale;
import com.example.visceralmassageapi.news.dto.NewsResponse;
import com.example.visceralmassageapi.news.dto.NewsUpdateRequest;
import com.example.visceralmassageapi.news.entity.NewsItem;
import com.example.visceralmassageapi.news.exception.NewsNotFoundException;
import com.example.visceralmassageapi.news.mapper.NewsMapper;
import com.example.visceralmassageapi.news.repository.NewsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NewsServiceImpl implements NewsService {

    private final NewsRepository repo;

    public NewsServiceImpl(NewsRepository repo) {
        this.repo = repo;
    }

    @Override
    public Page<LocalizedNewsResponse> findAll(NewsLocale locale, Pageable pageable) {
        return repo.findAll(pageable).map(item -> NewsMapper.toLocalizedResponse(item, locale));
    }

    @Override
    public LocalizedNewsResponse findById(Integer id, NewsLocale locale) {
        var entity = repo.findById(id).orElseThrow(() -> new NewsNotFoundException(id));
        return NewsMapper.toLocalizedResponse(entity, locale);
    }

    @Override
    public Page<NewsResponse> findAllForAdmin(Pageable pageable) {
        return repo.findAll(pageable).map(NewsMapper::toResponse);
    }

    @Override
    public NewsResponse findByIdForAdmin(Integer id) {
        var entity = repo.findById(id).orElseThrow(() -> new NewsNotFoundException(id));
        return NewsMapper.toResponse(entity);
    }

    @Override
    public NewsResponse create(NewsCreateRequest request) {
        var entity = NewsMapper.toEntity(request);
        validateTranslationState(entity);
        var saved = repo.save(entity);
        return NewsMapper.toResponse(saved);
    }

    @Override
    public NewsResponse updatePut(Integer id, NewsUpdateRequest request) {
        var entity = repo.findById(id).orElseThrow(() -> new NewsNotFoundException(id));
        entity.setTitleUa(request.getTitleUa());
        entity.setContentUa(request.getContentUa());
        entity.setTitleEn(request.getTitleEn());
        entity.setContentEn(request.getContentEn());
        validateTranslationState(entity);
        return NewsMapper.toResponse(repo.save(entity));
    }

    @Override
    public NewsResponse updatePatch(Integer id, NewsUpdateRequest request) {
        var entity = repo.findById(id).orElseThrow(() -> new NewsNotFoundException(id));
        if (request.getTitleUa() != null) entity.setTitleUa(request.getTitleUa());
        if (request.getContentUa() != null) entity.setContentUa(request.getContentUa());
        if (request.getTitleEn() != null) entity.setTitleEn(request.getTitleEn());
        if (request.getContentEn() != null) entity.setContentEn(request.getContentEn());
        validateTranslationState(entity);
        return NewsMapper.toResponse(repo.save(entity));
    }

    @Override
    public void delete(Integer id) {
        if (!repo.existsById(id)) throw new NewsNotFoundException(id);
        repo.deleteById(id);
    }

    private void validateTranslationState(NewsItem item) {
        validateLocalePair(item.getTitleUa(), item.getContentUa(), "ua");
        validateLocalePair(item.getTitleEn(), item.getContentEn(), "en");

        if (!hasCompleteTranslation(item.getTitleUa(), item.getContentUa())
                && !hasCompleteTranslation(item.getTitleEn(), item.getContentEn())) {
            throw new BadRequestException("At least one complete news translation is required");
        }
    }

    private void validateLocalePair(String title, String content, String locale) {
        if (StringUtils.hasText(title) != StringUtils.hasText(content)) {
            throw new BadRequestException("Both title and content are required for locale " + locale);
        }
    }

    private boolean hasCompleteTranslation(String title, String content) {
        return StringUtils.hasText(title) && StringUtils.hasText(content);
    }
}
