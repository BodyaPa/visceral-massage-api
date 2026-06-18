package com.example.visceralmassageapi.site.dto;

import jakarta.validation.constraints.Size;

public record SiteSettingsRequest(
        @Size(max = 2000) String footerBodyUa,
        @Size(max = 2000) String footerBodyEn,
        @Size(max = 4000) String homeIntroUa,
        @Size(max = 4000) String homeIntroEn,
        @Size(max = 4000) String aboutBodyUa,
        @Size(max = 4000) String aboutBodyEn,
        @Size(max = 4000) String contactBodyUa,
        @Size(max = 4000) String contactBodyEn
) {
}
