package com.distri.apigateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.JwtException;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtGatewayFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    // Rutas públicas (no requieren token)
    private static final Set<String> WHITELIST_PREFIXES = Set.of(
            "/auth/", "/v3/api-docs", "/swagger-ui", "/actuator/health"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String p = request.getRequestURI();
        // Permite /auth/**, swagger/health y preflight CORS
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || WHITELIST_PREFIXES.stream().anyMatch(p::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) {
            unauthorized(res, "Credenciales inválidas o ausentes");
            return;
        }

        String token = h.substring(7);
        try {
            jwt.parse(token); // ← valida firma/exp/estructura
            chain.doFilter(req, res); // OK, continúa el ruteo del Gateway
        } catch (JwtException ex) {
            unauthorized(res, "Token inválido");
        }
    }

    private void unauthorized(HttpServletResponse res, String msg) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"" + msg + "\"}");
    }
}
