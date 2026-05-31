package com.example.visceralmassageapi.offices.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class OfficeResponse {
    private Long id;
    private String name;
    private String address;
    private boolean active;
    private String phone;
    private String email;
    private String locationDetails;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
