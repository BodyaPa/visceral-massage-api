package com.example.visceralmassageapi.offices.controller;

import com.example.visceralmassageapi.offices.dto.OfficeRequest;
import com.example.visceralmassageapi.offices.dto.OfficeResponse;
import com.example.visceralmassageapi.offices.service.OfficeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/offices")
@RequiredArgsConstructor
public class AdminOfficeController {

    private final OfficeService officeService;

    @GetMapping
    public Page<OfficeResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable
    ) {
        return officeService.list(query, active, pageable);
    }

    @GetMapping("/{id}")
    public OfficeResponse get(@PathVariable long id) {
        return officeService.get(id);
    }

    @PostMapping
    public ResponseEntity<OfficeResponse> create(@Valid @RequestBody OfficeRequest request) {
        return ResponseEntity.ok(officeService.create(request));
    }

    @PutMapping("/{id}")
    public OfficeResponse update(@PathVariable long id, @Valid @RequestBody OfficeRequest request) {
        return officeService.update(id, request);
    }
}
