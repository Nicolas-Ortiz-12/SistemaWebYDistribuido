package com.distri.seguridad.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

// security/RestAuthEntryPoint.java (401)
@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
<<<<<<< HEAD
        response.setContentType("application/json;charset=UTF-8");
=======
>>>>>>> d97f1f3ffd27942626cc5cf3ba1628e04ecdc0fb
        response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"Credenciales inválidas o ausentes\"}");
    }
}
