package com.example.visceralmassageapi.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({CookieProps.class, OwnerBootstrapProps.class, CorsProps.class, MediaProps.class})
public class PropsConfig {}
