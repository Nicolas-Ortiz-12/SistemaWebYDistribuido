package com.distri.seguridad.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
// security/JwtService.java
@Service
@RequiredArgsConstructor
public class JwtService {
    // Claim que distingue el access token del refresh token: uno no puede usarse como el otro
    public static final String TYPE_CLAIM = "typ";
    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";

    @Value("${security.jwt.secret}") private String secret;
    @Value("${security.jwt.issuer}") private String issuer;
    @Value("${security.jwt.expiration-minutes}") private long expMin;
    @Value("${security.jwt.refresh-expiration-days}") private long refreshDays;

    private Key key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String subject, Collection<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(subject)
                .claim(TYPE_CLAIM, ACCESS)
                .claim("roles", roles)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expMin, ChronoUnit.MINUTES)))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String subject) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(subject)
                .claim(TYPE_CLAIM, REFRESH)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(refreshDays, ChronoUnit.DAYS)))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

<<<<<<< HEAD
    /** Valida firma, expiración, issuer y que sea un access token. */
    public Jws<Claims> parseAccess(String token) {
        return parse(token, ACCESS);
    }

    /** Valida firma, expiración, issuer y que sea un refresh token. */
    public Jws<Claims> parseRefresh(String token) {
        return parse(token, REFRESH);
    }

    private Jws<Claims> parse(String token, String expectedType) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(key())
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token);
        if (!expectedType.equals(jws.getBody().get(TYPE_CLAIM, String.class))) {
            throw new JwtException("Tipo de token inválido");
        }
        return jws;
=======
    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .requireIssuer(issuer)
                .setAllowedClockSkewSeconds(60)
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token);
>>>>>>> d97f1f3ffd27942626cc5cf3ba1628e04ecdc0fb
    }
}
