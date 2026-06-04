package com.example.visceralmassageapi.offices.controller;

import com.example.visceralmassageapi.offices.dto.OfficeResponse;
import com.example.visceralmassageapi.offices.service.OfficeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/offices")
@RequiredArgsConstructor
public class OfficeController {

    private final OfficeService officeService;

    @GetMapping
    public Page<OfficeResponse> list(Pageable pageable) {
        return officeService.listPublic(pageable);
    }
}
