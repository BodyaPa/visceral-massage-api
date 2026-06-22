package com.example.visceralmassageapi.auth.service;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.dto.AdminUserDto;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.common.exception.BadRequestException;
import com.example.visceralmassageapi.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserDto> listUsers(String query, Boolean enabled, UserRole role, Pageable pageable) {
        String normalizedQuery = normalizeQuery(query);
        return userRepository.searchUsers(normalizedQuery, enabled, role, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public AdminUserDto getUser(long id) {
        return toDto(requireUser(id));
    }

    @Transactional
    public AdminUserDto updateRoles(long id, long actorId, Set<UserRole> requestedRoles) {
        User user = requireUser(id);
        Set<UserRole> roles = normalizeRoles(requestedRoles);

        if (id == actorId && user.getRoles().contains(UserRole.MASTER) && !roles.contains(UserRole.MASTER)) {
            throw new BadRequestException("Cannot remove MASTER from your own account");
        }

        user.getRoles().clear();
        user.getRoles().addAll(roles);

        return toDto(user);
    }

    private User requireUser(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        return "%" + query.trim().toLowerCase() + "%";
    }

    private Set<UserRole> normalizeRoles(Set<UserRole> requestedRoles) {
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            throw new BadRequestException("At least one role is required");
        }

        Set<UserRole> roles = new LinkedHashSet<>(requestedRoles);
        roles.add(UserRole.USER);
        return roles;
    }

    private AdminUserDto toDto(User user) {
        return new AdminUserDto(
                user.getId(),
                user.getPhone(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getDateOfBirth(),
                user.getAvatarMediaId(),
                AuthService.avatarUrl(user),
                user.isEnabled(),
                orderedRoles(user.getRoles()),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private Set<UserRole> orderedRoles(Set<UserRole> roles) {
        return roles.stream()
                .sorted(Comparator.comparing(Enum::name))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
