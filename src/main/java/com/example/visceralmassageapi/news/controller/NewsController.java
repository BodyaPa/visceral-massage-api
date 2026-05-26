package com.example.visceralmassageapi.news.controller;

import com.example.visceralmassageapi.news.dto.LocalizedNewsResponse;
import com.example.visceralmassageapi.news.dto.NewsLocale;
import com.example.visceralmassageapi.news.service.NewsService;
import com.example.visceralmassageapi.media.service.MediaService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService service;
    private final MediaService mediaService;

    public NewsController(NewsService service, MediaService mediaService) {
        this.service = service;
        this.mediaService = mediaService;
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

    @GetMapping("/{id}/media/{mediaId}/content")
    public ResponseEntity<Resource> content(@PathVariable Integer id, @PathVariable UUID mediaId) {
        service.requirePublished(id);
        MediaService.MediaContent media = mediaService.loadPublishedContent(id, mediaId);
        var asset = media.asset();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.contentType()))
                .contentLength(asset.sizeBytes())
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(asset.originalFilename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(media.content());
    }
}
