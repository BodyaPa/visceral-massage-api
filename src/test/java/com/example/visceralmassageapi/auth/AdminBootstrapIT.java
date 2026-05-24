package com.example.visceralmassageapi.auth;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.auth.service.AdminBootstrapRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class AdminBootstrapIT extends IntegrationTestBase {

    private static final String ADMIN_PHONE = "+380000000099";
    private static final String ADMIN_PASSWORD = "ConfiguredAdminPassword123!";

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AdminBootstrapRunner adminBootstrapRunner;

    @DynamicPropertySource
    static void adminBootstrapProperties(DynamicPropertyRegistry registry) {
        registry.add("app.admin.bootstrap.enabled", () -> "true");
        registry.add("app.admin.bootstrap.phone", () -> ADMIN_PHONE);
        registry.add("app.admin.bootstrap.email", () -> "ADMIN@EXAMPLE.COM");
        registry.add("app.admin.bootstrap.password", () -> ADMIN_PASSWORD);
    }

    @Test
    void enabledBootstrap_createsOneAdminWithHashedPassword() throws Exception {
        var admin = userRepository.findByPhone(ADMIN_PHONE).orElseThrow();

        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.getEmail()).isEqualTo("admin@example.com");
        assertThat(admin.getPasswordHash()).isNotEqualTo(ADMIN_PASSWORD);
        assertThat(passwordEncoder.matches(ADMIN_PASSWORD, admin.getPasswordHash())).isTrue();

        adminBootstrapRunner.run(new DefaultApplicationArguments(new String[0]));

        assertThat(userRepository.findAll())
                .filteredOn(user -> user.getRole() == UserRole.ADMIN)
                .hasSize(1);
    }
}
