package com.example.visceralmassageapi.offices.service;

import com.example.visceralmassageapi.common.exception.NotFoundException;
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

@Service
@RequiredArgsConstructor
public class OfficeService {

    private final OfficeRepository officeRepository;

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
        return toResponse(officeRepository.save(office));
    }

    @Transactional
    public OfficeResponse update(long id, OfficeRequest request) {
        Office office = requireOffice(id);
        apply(office, request);
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
                office.getCreatedAt(),
                office.getUpdatedAt()
        );
    }
}
