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
@RequestMapping("/api/admin/news")
public class AdminNewsController {

    private final NewsService service;

    public AdminNewsController(NewsService service) {
        this.service = service;
    }

    @GetMapping
    public Page<NewsResponse> list(Pageable pageable) {
        return service.findAllForAdmin(pageable);
    }

    @GetMapping("/{id}")
    public NewsResponse get(@PathVariable Integer id) {
        return service.findByIdForAdmin(id);
    }

    @PostMapping
    public ResponseEntity<NewsResponse> create(@Valid @RequestBody NewsCreateRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public NewsResponse put(@PathVariable Integer id, @Valid @RequestBody NewsUpdateRequest request) {
        return service.updatePut(id, request);
    }

    @PatchMapping("/{id}")
    public NewsResponse patch(@PathVariable Integer id, @Valid @RequestBody NewsUpdateRequest request) {
        return service.updatePatch(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
