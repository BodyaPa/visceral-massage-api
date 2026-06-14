package com.example.visceralmassageapi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactChangeRequest {
    @Email
    @Size(max = 254)
    private String email;

    @Size(max = 32)
    private String phone;
}
