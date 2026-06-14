package com.example.visceralmassageapi.offices.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class OfficeRequest {
    @NotBlank
    @Size(max = 120)
    private String name;

    @NotBlank
    @Size(max = 255)
    private String address;

    private boolean active = true;

    @Size(max = 2000)
    private String locationDetails;

    @Size(max = 4000)
    private String directions;

    @Size(max = 2048)
    @Pattern(regexp = "^https?://.+$", message = "must be an http(s) URL")
    private String googleMapsUrl;

    private UUID photoMediaId;

    private UUID videoMediaId;
}
