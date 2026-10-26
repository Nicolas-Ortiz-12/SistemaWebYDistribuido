package com.distri.seguridad.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

// security/JwtAuthFilter.java
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final RolesCache rolesCache;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            String token = h.substring(7);
            try {
                var jws = jwt.parseAccess(token);
                String username = jws.getBody().getSubject();

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Los roles salen de la BD (vía cache), no del token: un usuario deshabilitado deja de entrar
                    var authorities = Arrays.stream(rolesCache.rolesOf(username).split(","))
                            .filter(r -> !r.isBlank())
                            // IMPORTANTE: si usás hasRole('ADMIN'), hay que prefijar con "ROLE_"
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                            .toList();

                    var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
                // token inválido o usuario inexistente/deshabilitado → sin auth; el entry point responderá 401
            }
        }
        chain.doFilter(req, res);
    }
}
