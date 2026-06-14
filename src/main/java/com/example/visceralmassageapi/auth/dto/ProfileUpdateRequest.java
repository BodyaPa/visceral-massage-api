package com.example.visceralmassageapi.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProfileUpdateRequest {
    @NotBlank
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[\\p{L}][\\p{L}'’ -]*$", message = "First name contains unsupported characters")
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[\\p{L}][\\p{L}'’ -]*$", message = "Last name contains unsupported characters")
    private String lastName;

    @Past
    private LocalDate dateOfBirth;
}
