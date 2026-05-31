package com.example.visceralmassageapi.services.controller;

import com.example.visceralmassageapi.services.dto.AdminServiceResponse;
import com.example.visceralmassageapi.services.dto.ServiceRequest;
import com.example.visceralmassageapi.services.service.ServiceOfferingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/services")
@RequiredArgsConstructor
public class AdminServiceController {

    private final ServiceOfferingService serviceOfferingService;

    @GetMapping
    public Page<AdminServiceResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            Pageable pageable
    ) {
        return serviceOfferingService.listAdmin(query, active, pageable);
    }

    @GetMapping("/{id}")
    public AdminServiceResponse get(@PathVariable long id) {
        return serviceOfferingService.getAdmin(id);
    }

    @PostMapping
    public ResponseEntity<AdminServiceResponse> create(@Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.ok(serviceOfferingService.create(request));
    }

    @PutMapping("/{id}")
    public AdminServiceResponse update(@PathVariable long id, @Valid @RequestBody ServiceRequest request) {
        return serviceOfferingService.update(id, request);
    }
}
