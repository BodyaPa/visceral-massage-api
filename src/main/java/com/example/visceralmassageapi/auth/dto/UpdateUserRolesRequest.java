package com.example.visceralmassageapi.auth.dto;

import com.example.visceralmassageapi.auth.domain.UserRole;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateUserRolesRequest {
    @NotEmpty
    private Set<UserRole> roles;
}
