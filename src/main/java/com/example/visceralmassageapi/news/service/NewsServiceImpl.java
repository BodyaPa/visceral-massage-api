package com.example.visceralmassageapi.news.service;

import com.example.visceralmassageapi.news.dto.NewsCreateRequest;
import com.example.visceralmassageapi.news.dto.NewsResponse;
import com.example.visceralmassageapi.news.dto.NewsUpdateRequest;
import com.example.visceralmassageapi.news.exception.NewsNotFoundException;
import com.example.visceralmassageapi.news.mapper.NewsMapper;
import com.example.visceralmassageapi.news.repository.NewsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class NewsServiceImpl implements NewsService {

    private final NewsRepository repo;

    public NewsServiceImpl(NewsRepository repo) {
        this.repo = repo;
    }

    @Override
    public Page<NewsResponse> findAll(Pageable pageable) {
        return repo.findAll(pageable).map(NewsMapper::toResponse);
    }

    @Override
    public NewsResponse findById(Integer id) {
        var entity = repo.findById(id).orElseThrow(() -> new NewsNotFoundException(id));
        return NewsMapper.toResponse(entity);
    }

    @Override
    public NewsResponse create(NewsCreateRequest request) {
        var entity = NewsMapper.toEntity(request);
        var saved = repo.save(entity);
        return NewsMapper.toResponse(saved);
    }

    @Override
    public NewsResponse updatePut(Integer id, NewsUpdateRequest request) {
        var entity = repo.findById(id).orElseThrow(() -> new NewsNotFoundException(id));
        entity.setTitle(request.getTitle());
        entity.setContent(request.getContent());
        return NewsMapper.toResponse(repo.save(entity));
    }

    @Override
    public NewsResponse updatePatch(Integer id, NewsUpdateRequest request) {
        var entity = repo.findById(id).orElseThrow(() -> new NewsNotFoundException(id));
        if (request.getTitle() != null) entity.setTitle(request.getTitle());
        if (request.getContent() != null) entity.setContent(request.getContent());
        return NewsMapper.toResponse(repo.save(entity));
    }

    @Override
    public void delete(Integer id) {
        if (!repo.existsById(id)) throw new NewsNotFoundException(id);
        repo.deleteById(id);
    }
}
