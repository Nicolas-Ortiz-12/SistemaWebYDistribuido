package com.distri.seguridad.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

// Limita los intentos de login fallidos por usuario. El contador vive en Redis para que
// lo compartan todas las instancias del cluster. Si Redis no responde, no bloquea el login.
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {
    private final StringRedisTemplate redis;

    @Value("${security.login.max-attempts:5}") private int maxAttempts;
    @Value("${security.login.block-minutes:15}") private long blockMinutes;

    public boolean isBlocked(String username) {
        try {
            String v = redis.opsForValue().get(key(username));
            return v != null && Long.parseLong(v) >= maxAttempts;
        } catch (RuntimeException e) {
            log.warn("No se pudo consultar intentos de login en Redis: {}", e.getMessage());
            return false;
        }
    }

    public void loginFailed(String username) {
        try {
            Long n = redis.opsForValue().increment(key(username));
            if (n != null && n == 1) redis.expire(key(username), Duration.ofMinutes(blockMinutes));
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar intento de login en Redis: {}", e.getMessage());
        }
    }

    public void loginSucceeded(String username) {
        try {
            redis.delete(key(username));
        } catch (RuntimeException e) {
            log.warn("No se pudo limpiar intentos de login en Redis: {}", e.getMessage());
        }
    }

    public long blockMinutes() { return blockMinutes; }

    private String key(String username) {
        return "seguridad:login-fail:" + username.toLowerCase(Locale.ROOT);
    }
}
