package com.example.visceralmassageapi.offices.service;

import com.example.visceralmassageapi.common.exception.NotFoundException;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.media.entity.MediaAsset;
import com.example.visceralmassageapi.media.exception.MediaAssetNotFoundException;
import com.example.visceralmassageapi.media.repository.MediaAssetRepository;
import com.example.visceralmassageapi.offices.dto.OfficeRequest;
import com.example.visceralmassageapi.offices.dto.OfficeResponse;
import com.example.visceralmassageapi.offices.entity.Office;
import com.example.visceralmassageapi.offices.repository.OfficeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfficeService {

    private final OfficeRepository officeRepository;
    private final MediaAssetRepository mediaAssetRepository;

    @Transactional(readOnly = true)
    public Page<OfficeResponse> list(String query, Boolean active, Pageable pageable) {
        return officeRepository.search(normalizeQuery(query), active, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<OfficeResponse> listPublic(Pageable pageable) {
        return officeRepository.search(null, true, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OfficeResponse get(long id) {
        return toResponse(requireOffice(id));
    }

    @Transactional
    public OfficeResponse create(OfficeRequest request) {
        Office office = new Office();
        apply(office, request);
        Office saved = officeRepository.save(office);
        linkOfficeMedia(saved);
        return toResponse(saved);
    }

    @Transactional
    public OfficeResponse update(long id, OfficeRequest request) {
        Office office = requireOffice(id);
        apply(office, request);
        linkOfficeMedia(office);
        return toResponse(office);
    }

    private Office requireOffice(long id) {
        return officeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Office not found"));
    }

    private void apply(Office office, OfficeRequest request) {
        office.setName(normalizeRequired(request.getName()));
        office.setAddress(normalizeRequired(request.getAddress()));
        office.setActive(request.isActive());
        office.setPhone(normalizeOptional(request.getPhone()));
        office.setEmail(normalizeEmail(request.getEmail()));
        office.setLocationDetails(normalizeOptional(request.getLocationDetails()));
        office.setDirections(normalizeOptionalLongText(request.getDirections()));
        office.setPhotoMediaId(request.getPhotoMediaId());
        office.setVideoMediaId(request.getVideoMediaId());
    }

    private String normalizeRequired(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalLongText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replace("\r\n", "\n").replace('\r', '\n');
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private OfficeResponse toResponse(Office office) {
        return new OfficeResponse(
                office.getId(),
                office.getName(),
                office.getAddress(),
                office.isActive(),
                office.getPhone(),
                office.getEmail(),
                office.getLocationDetails(),
                visibleDirections(office),
                office.getPhotoMediaId(),
                mediaUrl(office, office.getPhotoMediaId()),
                office.getVideoMediaId(),
                mediaUrl(office, office.getVideoMediaId()),
                office.getCreatedAt(),
                office.getUpdatedAt()
        );
    }

    private String visibleDirections(Office office) {
        return office.getDirections() == null ? office.getLocationDetails() : office.getDirections();
    }

    private void linkOfficeMedia(Office office) {
        linkMedia(office.getPhotoMediaId(), office.getId(), "image/");
        linkMedia(office.getVideoMediaId(), office.getId(), "video/");
    }

    private void linkMedia(UUID mediaId, Long officeId, String contentTypePrefix) {
        if (mediaId == null) {
            return;
        }
        MediaAsset asset = mediaAssetRepository.findById(mediaId)
                .orElseThrow(() -> new MediaAssetNotFoundException(mediaId));
        if (asset.getNewsId() != null) {
            throw new BadRequestException("Media asset is already linked to news");
        }
        if (asset.getOfficeId() != null && !asset.getOfficeId().equals(officeId)) {
            throw new BadRequestException("Media asset is already linked to another office");
        }
        if (!asset.getContentType().startsWith(contentTypePrefix)) {
            throw new BadRequestException("Office media type does not match the selected field");
        }
        asset.setOfficeId(officeId);
    }

    private String mediaUrl(Office office, UUID mediaId) {
        if (mediaId == null) {
            return null;
        }
        return "/api/offices/" + office.getId() + "/media/" + mediaId + "/content";
    }
}
