package com.example.visceralmassageapi.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "app.cookies")
public class CookieProps {
    private boolean secure = false;
    private String sameSite = "Lax";
}