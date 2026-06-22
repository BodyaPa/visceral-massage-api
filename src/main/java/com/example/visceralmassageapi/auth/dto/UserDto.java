package com.example.visceralmassageapi.auth.dto;

import com.example.visceralmassageapi.auth.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String phone;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private UUID avatarMediaId;
    private String avatarMediaUrl;
    private Set<UserRole> roles;
}
