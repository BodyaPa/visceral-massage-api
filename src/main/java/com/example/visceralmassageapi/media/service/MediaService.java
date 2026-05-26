package com.example.visceralmassageapi.media.service;

import com.example.visceralmassageapi.media.dto.MediaAssetResponse;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MediaService {

    Page<MediaAssetResponse> findAll(Pageable pageable);

    MediaAssetResponse findById(UUID id);

    MediaAssetResponse upload(MultipartFile file, long uploadedBy);

    MediaContent loadContent(UUID id);

    List<MediaAssetResponse> findAllForNews(Integer newsId);

    MediaAssetResponse linkToNews(UUID id, Integer newsId);

    MediaAssetResponse unlinkFromNews(UUID id, Integer newsId);

    MediaContent loadPublishedContent(Integer newsId, UUID id);

    void delete(UUID id);

    record MediaContent(MediaAssetResponse asset, Resource content) {
    }
}
