package com.example.visceralmassageapi.media.service;

import com.example.visceralmassageapi.common.config.MediaProps;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.common.exception.PayloadTooLargeException;
import com.example.visceralmassageapi.media.dto.MediaAssetResponse;
import com.example.visceralmassageapi.media.entity.MediaAsset;
import com.example.visceralmassageapi.media.exception.MediaAssetNotFoundException;
import com.example.visceralmassageapi.media.repository.MediaAssetRepository;
import com.example.visceralmassageapi.media.storage.MediaFileStorage;
import com.example.visceralmassageapi.news.exception.NewsNotFoundException;
import com.example.visceralmassageapi.news.repository.NewsRepository;
import com.example.visceralmassageapi.offices.repository.OfficeRepository;
import com.example.visceralmassageapi.site.domain.SiteSettings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MediaServiceImpl implements MediaService {

    private static final Map<String, String> FILE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "video/mp4", ".mp4",
            "video/webm", ".webm"
    );

    private final MediaAssetRepository repository;
    private final MediaFileStorage fileStorage;
    private final MediaProps properties;
    private final NewsRepository newsRepository;
    private final OfficeRepository officeRepository;

    public MediaServiceImpl(MediaAssetRepository repository, MediaFileStorage fileStorage, MediaProps properties,
                            NewsRepository newsRepository, OfficeRepository officeRepository) {
        this.repository = repository;
        this.fileStorage = fileStorage;
        this.properties = properties;
        this.newsRepository = newsRepository;
        this.officeRepository = officeRepository;
    }

    @Override
    public Page<MediaAssetResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public MediaAssetResponse findById(UUID id) {
        return toResponse(requireAsset(id));
    }

    @Override
    @Transactional
    public MediaAssetResponse upload(MultipartFile file, long uploadedBy) {
        String contentType = validateUpload(file);
        UUID id = UUID.randomUUID();
        String storageKey = id + FILE_EXTENSIONS.get(contentType);

        try (InputStream content = file.getInputStream()) {
            fileStorage.store(storageKey, content);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read media file", ex);
        }

        MediaAsset asset = new MediaAsset();
        asset.setId(id);
        asset.setStorageKey(storageKey);
        asset.setOriginalFilename(normalizeFilename(file.getOriginalFilename()));
        asset.setContentType(contentType);
        asset.setSizeBytes(file.getSize());
        asset.setUploadedBy(uploadedBy);
        asset.setCreatedAt(OffsetDateTime.now());

        try {
            return toResponse(repository.save(asset));
        } catch (RuntimeException ex) {
            fileStorage.delete(storageKey);
            throw ex;
        }
    }

    @Override
    @Transactional
    public MediaAssetResponse uploadSiteSettingsContentMedia(MultipartFile file, long uploadedBy) {
        MediaAssetResponse uploaded = upload(file, uploadedBy);
        MediaAsset asset = requireAsset(uploaded.id());
        asset.setSiteSettingsId(SiteSettings.SINGLETON_ID);
        asset.setSiteSliderSortOrder(null);
        return toResponse(repository.save(asset));
    }

    @Override
    public MediaContent loadContent(UUID id) {
        MediaAsset asset = requireAsset(id);
        return new MediaContent(toResponse(asset), fileStorage.load(asset.getStorageKey()));
    }

    @Override
    public List<MediaAssetResponse> findAllForNews(Integer newsId) {
        requireNews(newsId);
        return repository.findAllByNewsIdOrderByCreatedAtAsc(newsId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public MediaAssetResponse linkToNews(UUID id, Integer newsId) {
        requireNews(newsId);
        MediaAsset asset = requireAsset(id);
        if (asset.getNewsId() != null && !asset.getNewsId().equals(newsId)) {
            throw new BadRequestException("Media asset is already linked to another news item");
        }
        asset.setNewsId(newsId);
        return toResponse(repository.save(asset));
    }

    @Override
    @Transactional
    public MediaAssetResponse unlinkFromNews(UUID id, Integer newsId) {
        MediaAsset asset = requireAsset(id);
        if (!newsId.equals(asset.getNewsId())) {
            throw new BadRequestException("Media asset is not linked to this news item");
        }
        asset.setNewsId(null);
        return toResponse(repository.save(asset));
    }

    @Override
    public List<MediaAssetResponse> findAllForSiteSettings() {
        return repository.findAllBySiteSettingsIdAndSiteSliderSortOrderIsNotNullOrderBySiteSliderSortOrderAscCreatedAtAsc(SiteSettings.SINGLETON_ID)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MediaAssetResponse linkToSiteSettings(UUID id) {
        MediaAsset asset = requireAsset(id);
        if (asset.getNewsId() != null || asset.getOfficeId() != null) {
            throw new BadRequestException("Media asset is already linked to another content area");
        }
        if (asset.getSiteSettingsId() != null && !asset.getSiteSettingsId().equals(SiteSettings.SINGLETON_ID)) {
            throw new BadRequestException("Media asset is already linked to another site settings area");
        }
        if (asset.getSiteSettingsId() == null) {
            asset.setSiteSettingsId(SiteSettings.SINGLETON_ID);
            asset.setSiteSliderSortOrder(repository.maxSiteSliderSortOrder(SiteSettings.SINGLETON_ID) + 1);
        }
        return toResponse(repository.save(asset));
    }

    @Override
    @Transactional
    public MediaAssetResponse unlinkFromSiteSettings(UUID id) {
        MediaAsset asset = repository.findByIdAndSiteSettingsId(id, SiteSettings.SINGLETON_ID)
                .orElseThrow(() -> new MediaAssetNotFoundException(id));
        asset.setSiteSettingsId(null);
        asset.setSiteSliderSortOrder(null);
        return toResponse(repository.save(asset));
    }

    @Override
    @Transactional
    public List<MediaAssetResponse> reorderSiteSettingsMedia(List<UUID> mediaIds) {
        List<MediaAsset> current = repository.findAllBySiteSettingsIdAndSiteSliderSortOrderIsNotNullOrderBySiteSliderSortOrderAscCreatedAtAsc(SiteSettings.SINGLETON_ID);
        if (current.size() != mediaIds.size()) {
            throw new BadRequestException("Media order must include every linked site settings asset");
        }

        Map<UUID, MediaAsset> byId = current.stream().collect(java.util.stream.Collectors.toMap(MediaAsset::getId, asset -> asset));
        for (int index = 0; index < mediaIds.size(); index++) {
            MediaAsset asset = byId.get(mediaIds.get(index));
            if (asset == null) {
                throw new BadRequestException("Media order contains unknown site settings asset");
            }
            asset.setSiteSliderSortOrder(index);
        }
        return repository.saveAll(current).stream()
                .sorted(java.util.Comparator.comparing(MediaAsset::getSiteSliderSortOrder))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public MediaContent loadPublishedContent(Integer newsId, UUID id) {
        MediaAsset asset = repository.findByIdAndNewsId(id, newsId)
                .orElseThrow(() -> new MediaAssetNotFoundException(id));
        return new MediaContent(toResponse(asset), fileStorage.load(asset.getStorageKey()));
    }

    @Override
    public MediaContent loadSiteSettingsContent(UUID id) {
        MediaAsset asset = repository.findByIdAndSiteSettingsId(id, SiteSettings.SINGLETON_ID)
                .orElseThrow(() -> new MediaAssetNotFoundException(id));
        return new MediaContent(toResponse(asset), fileStorage.load(asset.getStorageKey()));
    }

    @Override
    public MediaContent loadOfficeContent(Long officeId, UUID id) {
        var office = officeRepository.findById(officeId)
                .orElseThrow(() -> new MediaAssetNotFoundException(id));
        if (!id.equals(office.getPhotoMediaId()) && !id.equals(office.getVideoMediaId())) {
            throw new MediaAssetNotFoundException(id);
        }
        MediaAsset asset = requireAsset(id);
        return new MediaContent(toResponse(asset), fileStorage.load(asset.getStorageKey()));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        MediaAsset asset = requireAsset(id);
        if (asset.getNewsId() != null) {
            throw new BadRequestException("Linked media asset must be detached before deletion");
        }
        if (asset.getOfficeId() != null) {
            throw new BadRequestException("Office media asset must be detached before deletion");
        }
        if (asset.getSiteSettingsId() != null) {
            throw new BadRequestException("Site settings media asset must be detached before deletion");
        }
        fileStorage.delete(asset.getStorageKey());
        repository.delete(asset);
    }

    private String validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Media file is required");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new PayloadTooLargeException("Media file is too large");
        }

        String contentType = normalizedContentType(file.getContentType());
        if (!properties.getAllowedContentTypes().contains(contentType) || !FILE_EXTENSIONS.containsKey(contentType)) {
            throw new BadRequestException("Unsupported media content type");
        }

        try (InputStream content = file.getInputStream()) {
            if (!hasMatchingSignature(contentType, content.readNBytes(16))) {
                throw new BadRequestException("Media content does not match its declared type");
            }
        } catch (IOException ex) {
            throw new BadRequestException("Unable to read media file");
        }
        return contentType;
    }

    private String normalizedContentType(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Media content type is required");
        }
        try {
            MediaType type = MediaType.parseMediaType(value);
            return (type.getType() + "/" + type.getSubtype()).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid media content type");
        }
    }

    private boolean hasMatchingSignature(String contentType, byte[] bytes) {
        return switch (contentType) {
            case "image/jpeg" -> startsWith(bytes, 0xff, 0xd8, 0xff);
            case "image/png" -> startsWith(bytes, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a);
            case "image/webp" -> startsWithAscii(bytes, 0, "RIFF") && startsWithAscii(bytes, 8, "WEBP");
            case "video/mp4" -> startsWithAscii(bytes, 4, "ftyp");
            case "video/webm" -> startsWith(bytes, 0x1a, 0x45, 0xdf, 0xa3);
            default -> false;
        };
    }

    private boolean startsWith(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if ((bytes[index] & 0xff) != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean startsWithAscii(byte[] bytes, int offset, String prefix) {
        if (bytes.length < offset + prefix.length()) {
            return false;
        }
        for (int index = 0; index < prefix.length(); index++) {
            if (bytes[offset + index] != (byte) prefix.charAt(index)) {
                return false;
            }
        }
        return true;
    }

    private String normalizeFilename(String originalFilename) {
        String cleaned = originalFilename == null
                ? ""
                : originalFilename.replaceAll("\\p{Cntrl}", "").trim();
        if (cleaned.isBlank()) {
            return "upload";
        }
        try {
            cleaned = Path.of(cleaned).getFileName().toString();
        } catch (InvalidPathException ex) {
            return "upload";
        }
        return cleaned.length() <= 255 ? cleaned : cleaned.substring(cleaned.length() - 255);
    }

    private MediaAsset requireAsset(UUID id) {
        return repository.findById(id).orElseThrow(() -> new MediaAssetNotFoundException(id));
    }

    private void requireNews(Integer newsId) {
        if (!newsRepository.existsById(newsId)) {
            throw new NewsNotFoundException(newsId);
        }
    }

    private MediaAssetResponse toResponse(MediaAsset asset) {
        return new MediaAssetResponse(
                asset.getId(),
                asset.getOriginalFilename(),
                asset.getContentType(),
                asset.getSizeBytes(),
                asset.getNewsId(),
                asset.getOfficeId(),
                asset.getSiteSettingsId(),
                asset.getSiteSliderSortOrder(),
                asset.getCreatedAt()
        );
    }
}
