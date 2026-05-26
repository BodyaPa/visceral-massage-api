package com.example.visceralmassageapi.news.controller;

import com.example.visceralmassageapi.media.dto.MediaAssetResponse;
import com.example.visceralmassageapi.media.service.MediaService;
import com.example.visceralmassageapi.news.dto.NewsCreateRequest;
import com.example.visceralmassageapi.news.dto.NewsResponse;
import com.example.visceralmassageapi.news.dto.NewsUpdateRequest;
import com.example.visceralmassageapi.news.entity.CoverDisplayMode;
import com.example.visceralmassageapi.news.service.NewsService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/news")
public class AdminNewsController {

    private final NewsService service;
    private final MediaService mediaService;

    public AdminNewsController(NewsService service, MediaService mediaService) {
        this.service = service;
        this.mediaService = mediaService;
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
    public ResponseEntity<NewsResponse> create(@Valid @RequestBody(required = false) NewsCreateRequest request) {
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

    @PostMapping("/{id}/publish")
    public NewsResponse publish(@PathVariable Integer id) {
        return service.publish(id);
    }

    @PostMapping("/{id}/unpublish")
    public NewsResponse unpublish(@PathVariable Integer id) {
        return service.unpublish(id);
    }

    @PostMapping("/{id}/archive")
    public NewsResponse archive(@PathVariable Integer id) {
        return service.archive(id);
    }

    @PostMapping("/{id}/restore")
    public NewsResponse restore(@PathVariable Integer id) {
        return service.restore(id);
    }

    @GetMapping("/{id}/media")
    public List<MediaAssetResponse> listMedia(@PathVariable Integer id) {
        return mediaService.findAllForNews(id);
    }

    @PutMapping("/{id}/media/{mediaId}")
    public MediaAssetResponse linkMedia(@PathVariable Integer id, @PathVariable UUID mediaId) {
        return mediaService.linkToNews(mediaId, id);
    }

    @DeleteMapping("/{id}/media/{mediaId}")
    public MediaAssetResponse unlinkMedia(@PathVariable Integer id, @PathVariable UUID mediaId) {
        service.clearCoverIfMatches(id, mediaId);
        return mediaService.unlinkFromNews(mediaId, id);
    }

    @PutMapping("/{id}/cover/{mediaId}")
    public NewsResponse setCover(@PathVariable Integer id, @PathVariable UUID mediaId) {
        return service.setCover(id, mediaId);
    }

    @PutMapping("/{id}/cover/display-mode/{displayMode}")
    public NewsResponse setCoverDisplayMode(@PathVariable Integer id, @PathVariable CoverDisplayMode displayMode) {
        return service.setCoverDisplayMode(id, displayMode);
    }

    @DeleteMapping("/{id}/cover")
    public NewsResponse clearCover(@PathVariable Integer id) {
        return service.clearCover(id);
    }
}
