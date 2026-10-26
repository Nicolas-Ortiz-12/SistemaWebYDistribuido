package com.distri.apigateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtGatewayFilterTest {

    static final String SECRET = "test-secret-0123456789-0123456789-0123456789";

    JwtGatewayFilter filter;

    @BeforeEach
    void setUp() {
        JwtService jwt = new JwtService();
        ReflectionTestUtils.setField(jwt, "secret", SECRET);
        ReflectionTestUtils.setField(jwt, "issuer", "proyectodistri");
        filter = new JwtGatewayFilter(jwt);
    }

    /** Devuelve el status; 0 significa que el request siguió al backend. */
    private int run(String uri, String token) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", uri);
        if (token != null) req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);
        return chain.getRequest() != null ? 0 : res.getStatus();
    }

    private static String token(String typ) {
        var b = Jwts.builder().setIssuer("proyectodistri").setSubject("pepe")
                .setExpiration(Date.from(Instant.now().plusSeconds(600)));
        if (typ != null) b.claim("typ", typ);
        return b.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256).compact();
    }

    @Test
    void loginEsPublico() throws Exception {
        assertThat(run("/auth/login", null)).isZero();
    }

    @Test
    void sinToken_es401() throws Exception {
        assertThat(run("/ventas/0/10", null)).isEqualTo(401);
    }

    @Test
    void respuestaDeError_enUtf8() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/ventas/0/10");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        assertThat(res.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(res.getContentAsString()).contains("inválidas");
    }

    @Test
    void accessToken_pasa_refreshOTokenSinTipo_no() throws Exception {
        assertThat(run("/ventas/0/10", token("access"))).isZero();
        assertThat(run("/ventas/0/10", token("refresh"))).isEqualTo(401);
        assertThat(run("/ventas/0/10", token(null))).isEqualTo(401);
    }

    @Test
    void pathTraversalDesdeLaListaBlanca_esRechazado() throws Exception {
        assertThat(run("/swagger-ui/../ventas/0/10", null)).isEqualTo(400);
        assertThat(run("/swagger-ui/%2e%2e/ventas/0/10", null)).isEqualTo(400);
        assertThat(run("/swagger-ui/..%2Fventas", null)).isEqualTo(400);
        assertThat(run("/auth/login/..;/ventas", null)).isEqualTo(400);
        assertThat(run("//ventas/0/10", null)).isEqualTo(400);
    }

    @Test
    void prefijoDeListaBlanca_soloPorSegmentoCompleto() throws Exception {
        assertThat(run("/swagger-ui/index.html", null)).isZero();
        assertThat(run("/swagger-uiX", null)).isEqualTo(401);
    }

    @Test
    void busquedasNormalesSiguenFuncionando() throws Exception {
        assertThat(run("/productos/0/10/martillo%20grande", token("access"))).isZero();
        assertThat(run("/ventas/0/50/3/2026-10-01T00%3A00%3A00-03%3A00", token("access"))).isZero();
    }
}
