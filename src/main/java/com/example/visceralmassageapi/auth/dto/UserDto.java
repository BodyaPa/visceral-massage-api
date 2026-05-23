package com.example.visceralmassageapi.auth.dto;

import com.example.visceralmassageapi.auth.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String phone;
    private String email;
    private UserRole role;
}