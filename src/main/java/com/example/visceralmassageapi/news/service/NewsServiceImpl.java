package com.example.visceralmassageapi.news.service;

import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.news.dto.LocalizedNewsResponse;
import com.example.visceralmassageapi.news.dto.NewsCreateRequest;
import com.example.visceralmassageapi.news.dto.NewsLocale;
import com.example.visceralmassageapi.news.dto.NewsResponse;
import com.example.visceralmassageapi.news.dto.NewsUpdateRequest;
import com.example.visceralmassageapi.news.entity.NewsItem;
import com.example.visceralmassageapi.news.entity.CoverDisplayMode;
import com.example.visceralmassageapi.news.entity.NewsStatus;
import com.example.visceralmassageapi.news.exception.NewsNotFoundException;
import com.example.visceralmassageapi.news.mapper.NewsMapper;
import com.example.visceralmassageapi.news.repository.NewsRepository;
import com.example.visceralmassageapi.media.service.MediaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class NewsServiceImpl implements NewsService {

    private final NewsRepository repo;
    private final MediaService mediaService;

    public NewsServiceImpl(NewsRepository repo, MediaService mediaService) {
        this.repo = repo;
        this.mediaService = mediaService;
    }

    @Override
    public Page<LocalizedNewsResponse> findAll(NewsLocale locale, Pageable pageable) {
        return repo.findPublishedForLocale(NewsStatus.PUBLISHED, locale.name().toLowerCase(), pageable)
                .map(item -> NewsMapper.toLocalizedResponse(item, locale));
    }

    @Override
    public LocalizedNewsResponse findById(Integer id, NewsLocale locale) {
        var entity = repo.findPublishedForLocale(id, NewsStatus.PUBLISHED, locale.name().toLowerCase())
                .orElseThrow(() -> new NewsNotFoundException(id));
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
        var entity = request == null ? new NewsItem() : NewsMapper.toEntity(request);
        OffsetDateTime now = OffsetDateTime.now();
        entity.setStatus(NewsStatus.DRAFT);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        var saved = repo.save(entity);
        return NewsMapper.toResponse(saved);
    }

    @Override
    public NewsResponse updatePut(Integer id, NewsUpdateRequest request) {
        var entity = repo.findById(id).orElseThrow(() -> new NewsNotFoundException(id));
        requireEditable(entity);
        entity.setTitleUa(request.getTitleUa());
        entity.setContentUa(request.getContentUa());
        entity.setTitleEn(request.getTitleEn());
        entity.setContentEn(request.getContentEn());
        validateCurrentPublicationState(entity);
        entity.setUpdatedAt(OffsetDateTime.now());
        return NewsMapper.toResponse(repo.save(entity));
    }

    @Override
    public NewsResponse updatePatch(Integer id, NewsUpdateRequest request) {
        var entity = repo.findById(id).orElseThrow(() -> new NewsNotFoundException(id));
        requireEditable(entity);
        if (request.getTitleUa() != null) entity.setTitleUa(request.getTitleUa());
        if (request.getContentUa() != null) entity.setContentUa(request.getContentUa());
        if (request.getTitleEn() != null) entity.setTitleEn(request.getTitleEn());
        if (request.getContentEn() != null) entity.setContentEn(request.getContentEn());
        validateCurrentPublicationState(entity);
        entity.setUpdatedAt(OffsetDateTime.now());
        return NewsMapper.toResponse(repo.save(entity));
    }

    @Override
    public NewsResponse publish(Integer id) {
        var entity = requireNews(id);
        requireEditable(entity);
        validatePublishable(entity);
        OffsetDateTime now = OffsetDateTime.now();
        entity.setStatus(NewsStatus.PUBLISHED);
        entity.setPublishedAt(now);
        entity.setUpdatedAt(now);
        return NewsMapper.toResponse(repo.save(entity));
    }

    @Override
    public NewsResponse unpublish(Integer id) {
        var entity = requireNews(id);
        if (entity.getStatus() != NewsStatus.PUBLISHED) {
            throw new BadRequestException("Only published news can be moved back to draft");
        }
        entity.setStatus(NewsStatus.DRAFT);
        entity.setUpdatedAt(OffsetDateTime.now());
        return NewsMapper.toResponse(repo.save(entity));
    }

    @Override
    public NewsResponse archive(Integer id) {
        var entity = requireNews(id);
        if (entity.getStatus() == NewsStatus.ARCHIVED) {
            throw new BadRequestException("News item is already archived");
        }
        entity.setStatus(NewsStatus.ARCHIVED);
        entity.setUpdatedAt(OffsetDateTime.now());
        return NewsMapper.toResponse(repo.save(entity));
    }

    @Override
    public NewsResponse restore(Integer id) {
        var entity = requireNews(id);
        if (entity.getStatus() != NewsStatus.ARCHIVED) {
            throw new BadRequestException("Only archived news can be restored");
        }
        entity.setStatus(NewsStatus.DRAFT);
        entity.setUpdatedAt(OffsetDateTime.now());
        return NewsMapper.toResponse(repo.save(entity));
    }

    @Override
    public NewsResponse setCover(Integer id, UUID mediaId) {
        var entity = requireNews(id);
        requireEditable(entity);
        var asset = mediaService.findById(mediaId);
        if (!id.equals(asset.newsId()) || !asset.contentType().startsWith("image/")) {
            throw new BadRequestException("Cover must be an image linked to this news item");
        }
        entity.setCoverMediaId(mediaId);
        entity.setUpdatedAt(OffsetDateTime.now());
        return NewsMapper.toResponse(repo.save(entity));
    }

    @Override
    public NewsResponse setCoverDisplayMode(Integer id, CoverDisplayMode displayMode) {
        var entity = requireNews(id);
        requireEditable(entity);
        entity.setCoverDisplayMode(displayMode);
        entity.setUpdatedAt(OffsetDateTime.now());
        return NewsMapper.toResponse(repo.save(entity));
    }

    @Override
    public NewsResponse clearCover(Integer id) {
        var entity = requireNews(id);
        requireEditable(entity);
        entity.setCoverMediaId(null);
        entity.setUpdatedAt(OffsetDateTime.now());
        return NewsMapper.toResponse(repo.save(entity));
    }

    @Override
    public void clearCoverIfMatches(Integer id, UUID mediaId) {
        var entity = requireNews(id);
        if (mediaId.equals(entity.getCoverMediaId())) {
            entity.setCoverMediaId(null);
            entity.setUpdatedAt(OffsetDateTime.now());
            repo.save(entity);
        }
    }

    @Override
    public void requirePublished(Integer id) {
        var entity = requireNews(id);
        if (entity.getStatus() != NewsStatus.PUBLISHED) {
            throw new NewsNotFoundException(id);
        }
    }

    @Override
    public void delete(Integer id) {
        var entity = requireNews(id);
        if (entity.getStatus() != NewsStatus.DRAFT || entity.getPublishedAt() != null) {
            throw new BadRequestException("Only never-published drafts can be deleted");
        }
        repo.deleteById(id);
    }

    private NewsItem requireNews(Integer id) {
        return repo.findById(id).orElseThrow(() -> new NewsNotFoundException(id));
    }

    private void requireEditable(NewsItem item) {
        if (item.getStatus() == NewsStatus.ARCHIVED) {
            throw new BadRequestException("Archived news must be restored before editing");
        }
    }

    private void validateCurrentPublicationState(NewsItem item) {
        if (item.getStatus() == NewsStatus.PUBLISHED) {
            validatePublishable(item);
        }
    }

    private void validatePublishable(NewsItem item) {
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
