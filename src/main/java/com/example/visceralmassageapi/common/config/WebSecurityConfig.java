package com.example.visceralmassageapi.common.config;

import com.example.visceralmassageapi.common.security.AuditAccessDeniedHandler;
import com.example.visceralmassageapi.common.security.JwtAuthenticationFilter;
import com.example.visceralmassageapi.common.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
public class WebSecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                            AuditAccessDeniedHandler auditAccessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
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

                        // but ME must require login
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()

                        // public content
                        .requestMatchers(HttpMethod.GET, "/api/news/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/pages/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/services/**").permitAll()

                        // admin-only
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // everything else in /api requires login (calendar, booking, etc.)
                        .requestMatchers("/api/**").authenticated()

                        // non-api (if any)
                        .anyRequest().permitAll()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Profile("dev")
    CorsConfigurationSource corsConfigurationSource() {
        var allowedOrigins = List.of(
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://localhost:5173"
        );

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
