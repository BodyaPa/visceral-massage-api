package com.example.visceralmassageapi.news.dto;

import com.example.visceralmassageapi.common.exception.BadRequestException;

public enum NewsLocale {
    UA,
    EN;

    public static NewsLocale from(String locale) {
        if ("ua".equalsIgnoreCase(locale)) {
            return UA;
        }
        if ("en".equalsIgnoreCase(locale)) {
            return EN;
        }
        throw new BadRequestException("Unsupported news locale");
    }
}
