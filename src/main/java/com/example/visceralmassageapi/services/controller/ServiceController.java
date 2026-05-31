package com.example.visceralmassageapi.services.controller;

import com.example.visceralmassageapi.services.dto.PublicServiceResponse;
import com.example.visceralmassageapi.services.dto.ServiceLocale;
import com.example.visceralmassageapi.services.service.ServiceOfferingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceOfferingService serviceOfferingService;

    @GetMapping
    public Page<PublicServiceResponse> list(
            @RequestParam(defaultValue = "ua") String lang,
            Pageable pageable
    ) {
        return serviceOfferingService.listPublic(ServiceLocale.from(lang), pageable);
    }
}
