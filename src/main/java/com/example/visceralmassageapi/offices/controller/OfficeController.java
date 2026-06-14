package com.example.visceralmassageapi.offices.controller;

import com.example.visceralmassageapi.offices.dto.OfficeResponse;
import com.example.visceralmassageapi.media.dto.MediaAssetResponse;
import com.example.visceralmassageapi.media.service.MediaService;
import com.example.visceralmassageapi.offices.service.OfficeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/offices")
@RequiredArgsConstructor
public class OfficeController {

    private final OfficeService officeService;
    private final MediaService mediaService;

    @GetMapping
    public Page<OfficeResponse> list(Pageable pageable) {
        return officeService.listPublic(pageable);
    }

    @GetMapping("/{id}/media/{mediaId}/content")
    public ResponseEntity<Resource> content(@PathVariable long id, @PathVariable UUID mediaId) {
        MediaService.MediaContent media = mediaService.loadOfficeContent(id, mediaId);
        MediaAssetResponse asset = media.asset();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.contentType()))
                .contentLength(asset.sizeBytes())
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(asset.originalFilename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(media.content());
    }
}
