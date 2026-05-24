package com.example.visceralmassageapi.news.controller;

import com.example.visceralmassageapi.news.dto.NewsResponse;
import com.example.visceralmassageapi.news.service.NewsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
}
