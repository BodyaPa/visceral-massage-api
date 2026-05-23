package com.example.visceralmassageapi.common.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public class ApiErrorResponse {
    private OffsetDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    private List<FieldErrorItem> fieldErrors;

    @Getter
    @Builder
    public static class FieldErrorItem {
        private String field;
        private String message;
    }
}