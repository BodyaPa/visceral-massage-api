package com.example.visceralmassageapi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterRequest {
    @NotBlank
    private String phone;

    @Email
    private String email; // optional

    @NotBlank
    private String password;
}