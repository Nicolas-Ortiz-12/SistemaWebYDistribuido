package com.distri.proyectoVenta.security;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class RestAccessDeniedHandler implements org.springframework.security.web.access.AccessDeniedHandler {
    @Override public void handle(HttpServletRequest req, HttpServletResponse res,
                                 org.springframework.security.access.AccessDeniedException ex) throws IOException {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"forbidden\",\"message\":\"No tiene permisos para este recurso\"}");
    }
}