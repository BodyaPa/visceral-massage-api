package com.example.visceralmassageapi.auth.api;

import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.media.dto.MediaAssetResponse;
import com.example.visceralmassageapi.media.service.MediaService;
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
import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAvatarController {

    private final UserRepository userRepository;
    private final MediaService mediaService;

    @GetMapping("/{id}/avatar/{mediaId}/content")
    public ResponseEntity<Resource> avatarContent(@PathVariable long id, @PathVariable UUID mediaId) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!mediaId.equals(user.getAvatarMediaId())) {
            throw new NotFoundException("Avatar not found");
        }
        MediaService.MediaContent media = mediaService.loadContent(mediaId);
        MediaAssetResponse asset = media.asset();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.contentType()))
                .contentLength(asset.sizeBytes())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(asset.originalFilename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(media.content());
    }
}
