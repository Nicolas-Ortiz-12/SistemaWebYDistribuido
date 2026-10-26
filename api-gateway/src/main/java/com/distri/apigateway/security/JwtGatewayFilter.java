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
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class JwtGatewayFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    private static final Set<String> WHITELIST_EXACT = Set.of(
            "/Seguridad/auth/login",
            "/auth/login",
            "/Seguridad/auth/refresh",
            "/auth/refresh"
    );

    private static final Set<String> WHITELIST_PREFIXES = Set.of(
            "/v3/api-docs", "/swagger-ui", "/actuator/health"
    );

    // El gateway no tiene el firewall de Spring Security: una URI como "/swagger-ui/../ventas"
    // pasaría la lista blanca y se enrutaría a /ventas. Se rechazan las URIs no canónicas.
    private static final Pattern NON_CANONICAL = Pattern.compile("(^|/)\\.\\.?(/|$)|//|;|\\\\|%2e|%2f|%5c|%3b");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean preflight = HttpMethod.OPTIONS.matches(request.getMethod())
                && request.getHeader("Origin") != null
                && request.getHeader("Access-Control-Request-Method") != null;
        return preflight;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (NON_CANONICAL.matcher(path.toLowerCase(Locale.ROOT)).find()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"error\":\"bad_request\",\"message\":\"Ruta inválida\"}");
            return;
        }
        if (isPublic(path)) {
            chain.doFilter(req, res);
            return;
        }

        String h = req.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) {
            unauthorized(res, "Credenciales inválidas o ausentes");
            return;
        }

        String token = h.substring(7);
        try {
            jwt.parse(token);
        } catch (JwtException | IllegalArgumentException ex) {
            unauthorized(res, "Token inválido");
            return;
        }
        chain.doFilter(req, res);
    }

    static boolean isPublic(String path) {
        if (WHITELIST_EXACT.contains(path)) return true;
        // Coincidencia por segmento completo: "/swagger-ui" o "/swagger-ui/...", no "/swagger-uiX"
        return WHITELIST_PREFIXES.stream().anyMatch(p -> path.equals(p) || path.startsWith(p + "/"));
    }

    private void unauthorized(HttpServletResponse res, String msg) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"" + msg + "\"}");
    }
}
