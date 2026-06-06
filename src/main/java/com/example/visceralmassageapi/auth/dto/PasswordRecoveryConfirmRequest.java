package com.example.visceralmassageapi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordRecoveryConfirmRequest {

    @Email
    @Size(max = 254)
    private String email;

    @Size(max = 32)
    private String phone;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Recovery code must contain 6 digits")
    private String code;

    @NotBlank
    @Size(min = 12, max = 128)
    @Pattern(regexp = ".*\\p{Lu}.*", message = "Password must contain an uppercase letter")
    @Pattern(regexp = ".*\\p{Ll}.*", message = "Password must contain a lowercase letter")
    @Pattern(regexp = ".*\\d.*", message = "Password must contain a number")
    @Pattern(regexp = ".*[^\\p{L}\\d\\s].*", message = "Password must contain a special character")
    private String newPassword;
}
