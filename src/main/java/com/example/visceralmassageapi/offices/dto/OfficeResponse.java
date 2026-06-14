package com.example.visceralmassageapi.offices.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OfficeResponse {
    private Long id;
    private String name;
    private String address;
    private boolean active;
    private String locationDetails;
    private String directions;
    private String googleMapsUrl;
    private UUID photoMediaId;
    private String photoMediaUrl;
    private UUID videoMediaId;
    private String videoMediaUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
