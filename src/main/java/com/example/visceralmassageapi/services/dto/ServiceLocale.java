package com.example.visceralmassageapi.services.dto;

import com.example.visceralmassageapi.common.exception.BadRequestException;

public enum ServiceLocale {
    UA,
    EN;

    public static ServiceLocale from(String locale) {
        if ("ua".equalsIgnoreCase(locale)) {
            return UA;
        }
        if ("en".equalsIgnoreCase(locale)) {
            return EN;
        }
        throw new BadRequestException("Unsupported service locale");
    }
}
