package com.example.visceralmassageapi.site.controller;

import com.example.visceralmassageapi.media.dto.MediaAssetResponse;
import com.example.visceralmassageapi.media.service.MediaService;
import com.example.visceralmassageapi.site.dto.SiteSettingsResponse;
import com.example.visceralmassageapi.site.service.SiteSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/site-settings")
@RequiredArgsConstructor
public class SiteSettingsController {

    private final SiteSettingsService settingsService;
    private final MediaService mediaService;

    @GetMapping
    public SiteSettingsResponse get() {
        return settingsService.get();
    }

    @GetMapping("/media")
    public List<MediaAssetResponse> listMedia() {
        return mediaService.findAllForSiteSettings();
    }

    @GetMapping("/media/{mediaId}/content")
    public ResponseEntity<Resource> mediaContent(@PathVariable UUID mediaId) {
        MediaService.MediaContent media = mediaService.loadSiteSettingsContent(mediaId);
        MediaAssetResponse asset = media.asset();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.contentType()))
                .contentLength(asset.sizeBytes())
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(asset.originalFilename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(media.content());
    }
}
