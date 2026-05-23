package com.example.visceralmassageapi.news.controller;

import com.example.visceralmassageapi.news.dto.NewsCreateRequest;
import com.example.visceralmassageapi.news.dto.NewsResponse;
import com.example.visceralmassageapi.news.dto.NewsUpdateRequest;
import com.example.visceralmassageapi.news.service.NewsService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService service;

    public NewsController(NewsService service) {
        this.service = service;
    }

    @GetMapping
    public Page<NewsResponse> list(Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    public NewsResponse get(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<NewsResponse> create(@Valid @RequestBody NewsCreateRequest request) {
        var created = service.create(request);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public NewsResponse put(@PathVariable Integer id, @Valid @RequestBody NewsUpdateRequest request) {
        return service.updatePut(id, request);
    }

    @PatchMapping("/{id}")
    public NewsResponse patch(@PathVariable Integer id, @RequestBody NewsUpdateRequest request) {
        return service.updatePatch(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
