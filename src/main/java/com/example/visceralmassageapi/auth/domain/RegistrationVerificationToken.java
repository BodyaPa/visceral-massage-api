package com.example.visceralmassageapi.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "registration_verification_tokens")
public class RegistrationVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "contact_type", nullable = false, length = 16)
    private String contactType;

    @Column(name = "contact_value", nullable = false, length = 255)
    private String contactValue;

    @Column(name = "code_hash", nullable = false, length = 64, unique = true)
    private String codeHash;

    @Column(name = "code_salt", nullable = false, length = 64)
    private String codeSalt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}
