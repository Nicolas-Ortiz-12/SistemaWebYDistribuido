package com.distri.seguridad.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JwtServiceTest {

    static final String SECRET = "test-secret-0123456789-0123456789-0123456789";

    JwtService jwt;

    @BeforeEach
    void setUp() {
        jwt = jwtCon("proyectodistri");
    }

    public static JwtService jwtCon(String issuer) {
        JwtService s = new JwtService();
        ReflectionTestUtils.setField(s, "secret", SECRET);
        ReflectionTestUtils.setField(s, "issuer", issuer);
        ReflectionTestUtils.setField(s, "expMin", 60L);
        ReflectionTestUtils.setField(s, "refreshDays", 7L);
        return s;
    }

    @Test
    void accessToken_valido() {
        var claims = jwt.parseAccess(jwt.generateAccessToken("pepe", List.of("ADMIN"))).getBody();
        assertThat(claims.getSubject()).isEqualTo("pepe");
        assertThat(claims.get("typ")).isEqualTo("access");
    }

    @Test
    void refreshToken_noSirveComoAccess_yViceversa() {
        String refresh = jwt.generateRefreshToken("pepe");
        String access = jwt.generateAccessToken("pepe", List.of("ADMIN"));

        assertThatThrownBy(() -> jwt.parseAccess(refresh)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> jwt.parseRefresh(access)).isInstanceOf(JwtException.class);
        assertThat(jwt.parseRefresh(refresh).getBody().getSubject()).isEqualTo("pepe");
    }

    @Test
    void issuerDistinto_esRechazado() {
        String ajeno = jwtCon("otro-sistema").generateAccessToken("pepe", List.of("ADMIN"));
        assertThatThrownBy(() -> jwt.parseAccess(ajeno)).isInstanceOf(JwtException.class);
    }
}
