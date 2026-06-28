package com.example.visceralmassageapi.site.controller;

import com.example.visceralmassageapi.media.dto.MediaAssetResponse;
import com.example.visceralmassageapi.media.service.MediaService;
import com.example.visceralmassageapi.site.dto.SiteMediaOrderRequest;
import com.example.visceralmassageapi.site.dto.SiteSettingsRequest;
import com.example.visceralmassageapi.site.dto.SiteSettingsResponse;
import com.example.visceralmassageapi.site.service.SiteSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/site-settings")
@RequiredArgsConstructor
public class AdminSiteSettingsController {

    private final SiteSettingsService settingsService;
    private final MediaService mediaService;

    @GetMapping
    public SiteSettingsResponse get() {
        return settingsService.get();
    }

    @PutMapping
    public SiteSettingsResponse update(Authentication authentication, @Valid @RequestBody SiteSettingsRequest request) {
        return settingsService.update(currentUserId(authentication), request);
    }

    @GetMapping("/media")
    public List<MediaAssetResponse> listMedia() {
        return mediaService.findAllForSiteSettings();
    }

    @PostMapping(path = "/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaAssetResponse> uploadMedia(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        long userId = currentUserId(authentication);
        MediaAssetResponse uploaded = mediaService.upload(file, userId);
        MediaAssetResponse linked = mediaService.linkToSiteSettings(uploaded.id());
        return ResponseEntity.created(URI.create("/api/admin/site-settings/media/" + linked.id())).body(linked);
    }

    @GetMapping("/media/{mediaId}/content")
    public ResponseEntity<Resource> mediaContent(@PathVariable UUID mediaId) {
        MediaService.MediaContent media = mediaService.loadSiteSettingsContent(mediaId);
        return mediaResponse(media, CacheControl.noStore());
    }

    @PutMapping("/media/{mediaId}")
    public MediaAssetResponse linkMedia(@PathVariable UUID mediaId) {
        return mediaService.linkToSiteSettings(mediaId);
    }

    @DeleteMapping("/media/{mediaId}")
    public MediaAssetResponse unlinkMedia(@PathVariable UUID mediaId) {
        return mediaService.unlinkFromSiteSettings(mediaId);
    }

    @PutMapping("/media/order")
    public List<MediaAssetResponse> reorderMedia(@Valid @RequestBody SiteMediaOrderRequest request) {
        return mediaService.reorderSiteSettingsMedia(request.mediaIds());
    }

    private long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Number id)) {
            throw new IllegalArgumentException("Not authenticated");
        }
        return id.longValue();
    }

    private ResponseEntity<Resource> mediaResponse(MediaService.MediaContent media, CacheControl cacheControl) {
        MediaAssetResponse asset = media.asset();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.contentType()))
                .contentLength(asset.sizeBytes())
                .cacheControl(cacheControl)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(asset.originalFilename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(media.content());
    }
}
