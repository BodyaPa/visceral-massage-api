package com.example.visceralmassageapi.auth.api;

import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.dto.AdminUserDto;
import com.example.visceralmassageapi.auth.dto.UpdateUserRolesRequest;
import com.example.visceralmassageapi.auth.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public Page<AdminUserDto> listUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) UserRole role,
            Pageable pageable
    ) {
        return adminUserService.listUsers(query, enabled, role, pageable);
    }

    @GetMapping("/{id}")
    public AdminUserDto getUser(@PathVariable long id) {
        return adminUserService.getUser(id);
    }

    @PatchMapping("/{id}/roles")
    public AdminUserDto updateRoles(
            @PathVariable long id,
            @Valid @RequestBody UpdateUserRolesRequest request,
            Authentication authentication
    ) {
        long actorId = (long) authentication.getPrincipal();
        return adminUserService.updateRoles(id, actorId, request.getRoles());
    }
}
