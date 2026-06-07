package com.distri.seguridad.web;

import com.distri.seguridad.domain.Role;
import com.distri.seguridad.security.JwtService;
import com.distri.seguridad.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authManager; // ← cambia a AuthenticationManager
    private final UserService userService;
    private final JwtService jwt;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginReq req) {
        var userOpt = userService.findActiveByUsername(req.username());
        if (userOpt.isEmpty())
            return ResponseEntity.status(401).body(Map.of("error","invalid_credentials"));

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password())
        );

        var u = userOpt.get();
        var roles = u.getRoles().stream().map(Role::getName).toList();
        String access = jwt.generateAccessToken(u.getUsername(), roles);
        String refresh = jwt.generateRefreshToken(u.getUsername());
        return ResponseEntity.ok(Map.of(
                "access_token", access,
                "refresh_token", refresh,
                "token_type", "Bearer",
                "expires_in_minutes", 60
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String,String> body) {
        String rt = body.getOrDefault("refresh_token", "");
        try {
            var jws = jwt.parse(rt);
            String username = jws.getBody().getSubject();
            var u = userService.findActiveByUsername(username).orElseThrow();
            var roles = u.getRoles().stream().map(Role::getName).toList();
            String access = jwt.generateAccessToken(username, roles);
            return ResponseEntity.ok(Map.of("access_token", access, "token_type", "Bearer"));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error","invalid_refresh"));
        }
    }

    @GetMapping("/me")
    public Map<String,Object> me(Authentication auth) {
        return Map.of("username", auth.getName(),
                "authorities", auth.getAuthorities());
    }

    public record LoginReq(@NotBlank String username, @NotBlank String password) {}
}
