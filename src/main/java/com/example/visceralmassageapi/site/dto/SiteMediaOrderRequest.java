package com.example.visceralmassageapi.site.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SiteMediaOrderRequest(
        @NotNull List<UUID> mediaIds
) {
}
