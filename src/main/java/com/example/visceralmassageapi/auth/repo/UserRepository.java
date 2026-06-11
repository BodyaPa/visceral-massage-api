package com.example.visceralmassageapi.auth.repo;

import com.example.visceralmassageapi.auth.domain.User;
import com.example.visceralmassageapi.auth.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    boolean existsByPhone(String phone);

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("""
            SELECT COUNT(user) > 0
            FROM User user
            JOIN user.roles role
            WHERE role = :role
            """)
    boolean existsByAssignedRole(UserRole role);

    @Query("""
            SELECT user
            FROM User user
            WHERE user.enabled = true
            AND :role MEMBER OF user.roles
            ORDER BY user.lastName ASC, user.firstName ASC, user.id ASC
            """)
    List<User> findEnabledUsersByRole(UserRole role);

    @Query(value = """
            SELECT DISTINCT user
            FROM User user
            WHERE (:query IS NULL
                OR LOWER(COALESCE(user.phone, '')) LIKE :query
                OR LOWER(COALESCE(user.email, '')) LIKE :query
                OR LOWER(COALESCE(user.firstName, '')) LIKE :query
                OR LOWER(COALESCE(user.lastName, '')) LIKE :query)
            AND (:enabled IS NULL OR user.enabled = :enabled)
            AND (:role IS NULL OR :role MEMBER OF user.roles)
            """,
            countQuery = """
            SELECT COUNT(DISTINCT user)
            FROM User user
            WHERE (:query IS NULL
                OR LOWER(COALESCE(user.phone, '')) LIKE :query
                OR LOWER(COALESCE(user.email, '')) LIKE :query
                OR LOWER(COALESCE(user.firstName, '')) LIKE :query
                OR LOWER(COALESCE(user.lastName, '')) LIKE :query)
            AND (:enabled IS NULL OR user.enabled = :enabled)
            AND (:role IS NULL OR :role MEMBER OF user.roles)
            """)
    Page<User> searchUsers(String query, Boolean enabled, UserRole role, Pageable pageable);
}
