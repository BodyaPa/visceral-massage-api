package com.example.visceralmassageapi.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeRequest {
    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 12, max = 128)
    @Pattern(regexp = ".*\\p{Lu}.*", message = "Password must contain an uppercase letter")
    @Pattern(regexp = ".*\\p{Ll}.*", message = "Password must contain a lowercase letter")
    @Pattern(regexp = ".*\\d.*", message = "Password must contain a number")
    @Pattern(regexp = ".*[^\\p{L}\\d\\s].*", message = "Password must contain a special character")
    private String newPassword;
}
