package com.example.visceralmassageapi.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey key;
    private final String issuer;
    private final int accessTtlMinutes;
    private final int refreshTtlDays;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.accessTtlMinutes}") int accessTtlMinutes,
            @Value("${jwt.refreshTtlDays}") int refreshTtlDays
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessTtlMinutes = accessTtlMinutes;
        this.refreshTtlDays = refreshTtlDays;
    }

    public String generateAccessToken(long userId, Collection<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(Long.toString(userId))
                .claim("roles", roles)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtlMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(Long.toString(userId))
                .id(UUID.randomUUID().toString())
                .claim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTtlDays, ChronoUnit.DAYS)))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token);
    }

    public long getUserId(String token) {
        return Long.parseLong(parse(token).getPayload().getSubject());
    }

    public List<String> getRoles(String token) {
        Object value = parse(token).getPayload().get("roles");
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(Object::toString)
                    .toList();
        }
        return List.of();
    }

    public boolean isAccessToken(String token) {
        return ACCESS_TOKEN_TYPE.equals(parse(token).getPayload().get(TOKEN_TYPE_CLAIM));
    }

    public boolean isRefreshToken(String token) {
        return REFRESH_TOKEN_TYPE.equals(parse(token).getPayload().get(TOKEN_TYPE_CLAIM));
    }

    public OffsetDateTime getExpiration(String token) {
        return OffsetDateTime.ofInstant(parse(token).getPayload().getExpiration().toInstant(), ZoneOffset.UTC);
    }

    public int getAccessTtlSeconds() {
        return accessTtlMinutes * 60;
    }

    public int getRefreshTtlSeconds() {
        return refreshTtlDays * 24 * 60 * 60;
    }
}
