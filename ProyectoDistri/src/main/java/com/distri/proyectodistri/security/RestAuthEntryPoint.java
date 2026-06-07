package com.distri.proyectodistri.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthEntryPoint implements org.springframework.security.web.AuthenticationEntryPoint {
    @Override public void commence(HttpServletRequest req, HttpServletResponse res,
                                   org.springframework.security.core.AuthenticationException ex) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"Credenciales inválidas o ausentes\"}");
    }
}
