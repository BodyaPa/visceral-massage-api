package com.example.visceralmassageapi.articles.controller;

import com.example.visceralmassageapi.articles.dto.ArticleCreateRequest;
import com.example.visceralmassageapi.articles.dto.ArticleResponse;
import com.example.visceralmassageapi.articles.dto.ArticleUpdateRequest;
import com.example.visceralmassageapi.articles.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService service;

    public ArticleController(ArticleService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ArticleResponse> list(Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    public ArticleResponse get(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<ArticleResponse> create(@Valid @RequestBody ArticleCreateRequest request) {
        var created = service.create(request);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ArticleResponse put(@PathVariable Integer id, @Valid @RequestBody ArticleUpdateRequest request) {
        return service.updatePut(id, request);
    }

    @PatchMapping("/{id}")
    public ArticleResponse patch(@PathVariable Integer id, @RequestBody ArticleUpdateRequest request) {
        return service.updatePatch(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
