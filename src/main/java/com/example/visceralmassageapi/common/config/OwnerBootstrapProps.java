package com.example.visceralmassageapi.common.config;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.owner.bootstrap")
public class OwnerBootstrapProps {
    private boolean enabled = false;
    private String phone;
    @Email
    private String email;
    private String password;
}
