package com.distri.seguridad.web;

import com.distri.seguridad.domain.Role;
import com.distri.seguridad.security.JwtService;
import com.distri.seguridad.security.LoginAttemptService;
import com.distri.seguridad.service.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authManager; // ← cambia a AuthenticationManager
    private final UserService userService;
    private final JwtService jwt;
<<<<<<< HEAD
    private final LoginAttemptService attempts;

    @Value("${security.jwt.expiration-minutes}")
    private long expirationMinutes;
=======
    @Value("${security.jwt.expiration-minutes:60}")
    private long expMin;
>>>>>>> d97f1f3ffd27942626cc5cf3ba1628e04ecdc0fb

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginReq req) {
        if (attempts.isBlocked(req.username())) {
            return ResponseEntity.status(429).body(Map.of("error", "too_many_attempts",
                    "message", "Demasiados intentos fallidos. Probá de nuevo en " + attempts.blockMinutes() + " minutos."));
        }

        // Se autentica antes de buscar el usuario: el AuthenticationManager tarda lo mismo
        // exista o no el usuario, así no se puede averiguar qué usuarios existen por el tiempo de respuesta.
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password())
            );
        } catch (AuthenticationException e) {
            attempts.loginFailed(req.username());
            return ResponseEntity.status(401).body(Map.of("error","invalid_credentials"));
        }
        attempts.loginSucceeded(req.username());

        var u = userService.findActiveByUsername(req.username()).orElseThrow();
        var roles = u.getRoles().stream().map(Role::getName).toList();
        String access = jwt.generateAccessToken(u.getUsername(), roles);
        String refresh = jwt.generateRefreshToken(u.getUsername());
        return ResponseEntity.ok(Map.of(
                "access_token", access,
                "refresh_token", refresh,
                "token_type", "Bearer",
<<<<<<< HEAD
                "expires_in_minutes", expirationMinutes
=======
                "expires_in_minutes", expMin
>>>>>>> d97f1f3ffd27942626cc5cf3ba1628e04ecdc0fb
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String,String> body) {
        String rt = body.getOrDefault("refresh_token", "");
        try {
            // Solo acepta refresh tokens: un access token no sirve para renovar
            var jws = jwt.parseRefresh(rt);
            String username = jws.getBody().getSubject();
            var u = userService.findActiveByUsername(username).orElse(null);
            if (u == null) return ResponseEntity.status(401).body(Map.of("error","invalid_refresh"));
            var roles = u.getRoles().stream().map(Role::getName).toList();
            String access = jwt.generateAccessToken(username, roles);
<<<<<<< HEAD
            return ResponseEntity.ok(Map.of("access_token", access, "token_type", "Bearer"));
        } catch (JwtException | IllegalArgumentException e) {
=======
            return ResponseEntity.ok(Map.of(
                    "access_token", access,
                    "token_type", "Bearer",
                    "expires_in_minutes", expMin
            ));
        } catch (Exception e) {
>>>>>>> d97f1f3ffd27942626cc5cf3ba1628e04ecdc0fb
            return ResponseEntity.status(401).body(Map.of("error","invalid_refresh"));
        }
    }

    @GetMapping("/me")
    public Map<String,Object> me(Authentication auth) {
        return Map.of("username", auth.getName(),
                "authorities", auth.getAuthorities());
    }

    // Topes: username es varchar(80) y BCrypt solo usa 72 bytes. Sin ellos, un usuario de miles
    // de caracteres llegaba igual a Redis como clave del contador de intentos.
    public record LoginReq(@NotBlank @Size(max = 80) String username, @NotBlank @Size(max = 128) String password) {}
}
