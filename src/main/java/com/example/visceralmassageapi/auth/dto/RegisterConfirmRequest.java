package com.example.visceralmassageapi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterConfirmRequest {

    @Email
    @Size(max = 254)
    private String email;

    @Size(max = 32)
    private String phone;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Registration code must contain 6 digits")
    private String code;
}
