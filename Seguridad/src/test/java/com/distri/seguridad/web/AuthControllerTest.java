package com.distri.seguridad.web;

import com.distri.seguridad.domain.Role;
import com.distri.seguridad.domain.UserAccount;
import com.distri.seguridad.security.JwtService;
import com.distri.seguridad.security.JwtServiceTest;
import com.distri.seguridad.security.LoginAttemptService;
import com.distri.seguridad.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    AuthenticationManager authManager = mock(AuthenticationManager.class);
    UserService userService = mock(UserService.class);
    LoginAttemptService attempts = mock(LoginAttemptService.class);
    JwtService jwt;
    AuthController controller;

    @BeforeEach
    void setUp() {
        jwt = JwtServiceTest.jwtCon("proyectodistri");
        controller = new AuthController(authManager, userService, jwt, attempts);
        ReflectionTestUtils.setField(controller, "expirationMinutes", 60L);

        UserAccount u = new UserAccount();
        u.setUsername("pepe");
        Role r = new Role();
        r.setName("ADMIN");
        u.getRoles().add(r);
        when(userService.findActiveByUsername("pepe")).thenReturn(Optional.of(u));
        when(attempts.blockMinutes()).thenReturn(15L);
    }

    @Test
    void loginCorrecto_devuelveTokensYLimpiaIntentos() {
        var resp = controller.login(new AuthController.LoginReq("pepe", "ok"));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked") var body = (Map<String, Object>) resp.getBody();
        assertThat(jwt.parseAccess((String) body.get("access_token")).getBody().getSubject()).isEqualTo("pepe");
        assertThat(jwt.parseRefresh((String) body.get("refresh_token")).getBody().getSubject()).isEqualTo("pepe");
        verify(attempts).loginSucceeded("pepe");
    }

    @Test
    void loginIncorrecto_es401YCuentaElIntento() {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        var resp = controller.login(new AuthController.LoginReq("pepe", "mal"));

        assertThat(resp.getStatusCode().value()).isEqualTo(401);
        verify(attempts).loginFailed("pepe");
    }

    @Test
    void usuarioInexistente_mismaRespuestaQueClaveIncorrecta() {
        // Se autentica primero (con la protección de tiempo de Spring) en vez de cortar antes
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));
        when(userService.findActiveByUsername("nadie")).thenReturn(Optional.empty());

        var resp = controller.login(new AuthController.LoginReq("nadie", "x"));

        assertThat(resp.getStatusCode().value()).isEqualTo(401);
        assertThat(resp.getBody()).isEqualTo(Map.of("error", "invalid_credentials"));
        verify(authManager).authenticate(any());
    }

    @Test
    void usuarioBloqueado_es429SinIntentarAutenticar() {
        when(attempts.isBlocked("pepe")).thenReturn(true);

        var resp = controller.login(new AuthController.LoginReq("pepe", "x"));

        assertThat(resp.getStatusCode().value()).isEqualTo(429);
        verifyNoInteractions(authManager);
    }

    @Test
    void refresh_conAccessToken_es401_yConRefreshToken_ok() {
        String access = jwt.generateAccessToken("pepe", List.of("ADMIN"));
        String refresh = jwt.generateRefreshToken("pepe");

        assertThat(controller.refresh(Map.of("refresh_token", access)).getStatusCode().value()).isEqualTo(401);
        assertThat(controller.refresh(Map.of("refresh_token", refresh)).getStatusCode().value()).isEqualTo(200);
    }
}
