package com.distri.seguridad.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthFilterTest {

    JwtService jwt = JwtServiceTest.jwtCon("proyectodistri");
    RolesCache rolesCache = mock(RolesCache.class);
    JwtAuthFilter filter = new JwtAuthFilter(jwt, rolesCache);

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    private void run(String token) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/auth/me");
        req.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
    }

    @Test
    void rolesSalenDeLaBd_noDelToken() throws Exception {
        when(rolesCache.rolesOf("pepe")).thenReturn("VENDEDOR");
        run(jwt.generateAccessToken("pepe", List.of("ADMIN")));

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_VENDEDOR");
    }

    @Test
    void usuarioDeshabilitado_quedaSinAutenticar() throws Exception {
        when(rolesCache.rolesOf("pepe")).thenThrow(new UsernameNotFoundException("x"));
        run(jwt.generateAccessToken("pepe", List.of("ADMIN")));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void refreshToken_quedaSinAutenticar() throws Exception {
        when(rolesCache.rolesOf("pepe")).thenReturn("ADMIN");
        run(jwt.generateRefreshToken("pepe"));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
