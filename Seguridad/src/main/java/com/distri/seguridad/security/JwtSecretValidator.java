package com.distri.seguridad.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtSecretValidator {

    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${security.jwt.secret:}")
    private String secret;

    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.isBlank() || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT_SECRET must be set and contain at least 32 characters");
        }
    }
}
