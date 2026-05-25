package com.example.visceralmassageapi.news.controller;

import com.example.visceralmassageapi.news.dto.LocalizedNewsResponse;
import com.example.visceralmassageapi.news.dto.NewsLocale;
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
    public Page<LocalizedNewsResponse> list(
            @RequestParam(defaultValue = "ua") String lang,
            Pageable pageable
    ) {
        return service.findAll(NewsLocale.from(lang), pageable);
    }

    @GetMapping("/{id}")
    public LocalizedNewsResponse get(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "ua") String lang
    ) {
        return service.findById(id, NewsLocale.from(lang));
    }
}
