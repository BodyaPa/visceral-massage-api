package com.example.visceralmassageapi.auth;

import com.example.visceralmassageapi.IntegrationTestBase;
import com.example.visceralmassageapi.auth.domain.UserRole;
import com.example.visceralmassageapi.auth.repo.UserRepository;
import com.example.visceralmassageapi.auth.service.OwnerBootstrapRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class OwnerBootstrapIT extends IntegrationTestBase {

    private static final String OWNER_PHONE = "+380000000099";
    private static final String OWNER_PASSWORD = "ConfiguredOwnerPassword123!";

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired OwnerBootstrapRunner ownerBootstrapRunner;

    @DynamicPropertySource
    static void ownerBootstrapProperties(DynamicPropertyRegistry registry) {
        registry.add("app.owner.bootstrap.enabled", () -> "true");
        registry.add("app.owner.bootstrap.phone", () -> OWNER_PHONE);
        registry.add("app.owner.bootstrap.email", () -> "OWNER@EXAMPLE.COM");
        registry.add("app.owner.bootstrap.password", () -> OWNER_PASSWORD);
    }

    @Test
    void enabledBootstrap_createsOneOwnerWithHashedPassword() throws Exception {
        var owner = userRepository.findByPhone(OWNER_PHONE).orElseThrow();

        assertThat(owner.getRoles()).containsExactlyInAnyOrder(
                UserRole.USER,
                UserRole.MASTER,
                UserRole.SPECIALIST,
                UserRole.FINANCE_MANAGER,
                UserRole.SMM
        );
        assertThat(owner.getEmail()).isEqualTo("owner@example.com");
        assertThat(owner.getPasswordHash()).isNotEqualTo(OWNER_PASSWORD);
        assertThat(passwordEncoder.matches(OWNER_PASSWORD, owner.getPasswordHash())).isTrue();

        ownerBootstrapRunner.run(new DefaultApplicationArguments(new String[0]));

        assertThat(userRepository.findAll())
                .filteredOn(user -> user.getRoles().contains(UserRole.MASTER))
                .hasSize(1);
    }
}
