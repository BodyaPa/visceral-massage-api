package com.example.visceralmassageapi.common.config;

import com.example.visceralmassageapi.common.security.AuditAccessDeniedHandler;
import com.example.visceralmassageapi.common.security.JwtAuthenticationFilter;
import com.example.visceralmassageapi.common.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
public class WebSecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                            AuditAccessDeniedHandler auditAccessDeniedHandler,
                                            CookieCsrfTokenRepository csrfTokenRepository) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.accessDeniedHandler(auditAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // DEV preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // auth open only for POST endpoints
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/password-recovery/request").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/password-recovery/confirm").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()

                        // but ME must require login
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()

                        // public content
                        .requestMatchers(HttpMethod.GET, "/api/news/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pages/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/services/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/offices/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/schedule/**").permitAll()

                        // management APIs
                        .requestMatchers("/api/admin/news", "/api/admin/news/**").hasRole("SMM")
                        .requestMatchers("/api/admin/media", "/api/admin/media/**").hasRole("SMM")
                        .requestMatchers("/api/admin/schedule", "/api/admin/schedule/**").hasRole("SPECIALIST")
                        .requestMatchers("/api/admin/finance", "/api/admin/finance/**").hasRole("FINANCE_MANAGER")
                        .requestMatchers("/api/admin/**").hasRole("MASTER")

                        // everything else in /api requires login (calendar, booking, etc.)
                        .requestMatchers("/api/**").authenticated()

                        // non-api (if any)
                        .anyRequest().permitAll()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository(CookieProps cookieProps) {
        CookieCsrfTokenRepository csrfTokenRepository = new CookieCsrfTokenRepository();
        csrfTokenRepository.setCookiePath("/");
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie
                .secure(cookieProps.isSecure())
                .sameSite(cookieProps.getSameSite()));
        return csrfTokenRepository;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProps corsProps) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProps.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
