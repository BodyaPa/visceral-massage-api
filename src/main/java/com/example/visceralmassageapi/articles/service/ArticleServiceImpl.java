package com.example.visceralmassageapi.articles.service;

import com.example.visceralmassageapi.articles.dto.ArticleCreateRequest;
import com.example.visceralmassageapi.articles.dto.ArticleResponse;
import com.example.visceralmassageapi.articles.dto.ArticleUpdateRequest;
import com.example.visceralmassageapi.articles.exception.ArticleNotFoundException;
import com.example.visceralmassageapi.articles.mapper.ArticleMapper;
import com.example.visceralmassageapi.articles.repository.ArticleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository repo;

    public ArticleServiceImpl(ArticleRepository repo) {
        this.repo = repo;
    }

    @Override
    public Page<ArticleResponse> findAll(Pageable pageable) {
        return repo.findAll(pageable).map(ArticleMapper::toResponse);
    }

    @Override
    public ArticleResponse findById(Integer id) {
        var entity = repo.findById(id).orElseThrow(() -> new ArticleNotFoundException(id));
        return ArticleMapper.toResponse(entity);
    }

    @Override
    public ArticleResponse create(ArticleCreateRequest request) {
        var entity = ArticleMapper.toEntity(request);
        var saved = repo.save(entity);
        return ArticleMapper.toResponse(saved);
    }

    @Override
    public ArticleResponse updatePut(Integer id, ArticleUpdateRequest request) {
        var entity = repo.findById(id).orElseThrow(() -> new ArticleNotFoundException(id));
        entity.setTitle(request.getTitle());
        entity.setContent(request.getContent());
        return ArticleMapper.toResponse(repo.save(entity));
    }

    @Override
    public ArticleResponse updatePatch(Integer id, ArticleUpdateRequest request) {
        var entity = repo.findById(id).orElseThrow(() -> new ArticleNotFoundException(id));
        if (request.getTitle() != null) entity.setTitle(request.getTitle());
        if (request.getContent() != null) entity.setContent(request.getContent());
        return ArticleMapper.toResponse(repo.save(entity));
    }

    @Override
    public void delete(Integer id) {
        if (!repo.existsById(id)) throw new ArticleNotFoundException(id);
        repo.deleteById(id);
    }
}
