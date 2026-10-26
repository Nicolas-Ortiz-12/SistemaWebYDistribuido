package com.distri.apigateway.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Service
public class JwtService {

    // Debe coincidir con el claim que emite el servicio de Seguridad
    private static final String TYPE_CLAIM = "typ";
    private static final String ACCESS = "access";

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.issuer}")
    private String issuer;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Valida firma, expiración, issuer y que sea un access token (un refresh token no sirve para llamar a la API). */
    public Jws<Claims> parse(String token) throws JwtException {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .requireIssuer(issuer)
                .setAllowedClockSkewSeconds(60)
                .build()
                .parseClaimsJws(token);
        if (!ACCESS.equals(jws.getBody().get(TYPE_CLAIM, String.class))) {
            throw new JwtException("Tipo de token inválido");
        }
        return jws;
    }
}
