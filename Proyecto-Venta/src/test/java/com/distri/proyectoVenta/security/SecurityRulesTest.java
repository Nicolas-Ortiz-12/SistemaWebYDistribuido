package com.distri.proyectoVenta.security;

import com.distri.proyectoVenta.apis.controller.ProveedorController;
import com.distri.proyectoVenta.apis.controller.RolController;
import com.distri.proyectoVenta.apis.controller.UsuarioController;
import com.distri.proyectoVenta.apis.controller.VentaController;
import com.distri.proyectoVenta.apis.service.ProveedorService;
import com.distri.proyectoVenta.apis.service.RolService;
import com.distri.proyectoVenta.apis.service.UsuarioService;
import com.distri.proyectoVenta.apis.service.VentaService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UsuarioController.class, RolController.class, VentaController.class, ProveedorController.class})
@Import({SecurityConfig.class, JwtService.class, RestAuthEntryPoint.class, RestAccessDeniedHandler.class})
@TestPropertySource(properties = {
        "security.jwt.secret=" + SecurityRulesTest.SECRET,
        "security.jwt.issuer=proyectodistri",
        "security.jwt.expiration-minutes=60",
        "security.jwt.refresh-expiration-days=7",
        "eureka.client.enabled=false"
})
class SecurityRulesTest {

    static final String SECRET = "test-secret-0123456789-0123456789-0123456789";

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;

    @MockitoBean UsuarioService usuarioService;
    @MockitoBean RolService rolService;
    @MockitoBean VentaService ventaService;
    @MockitoBean ProveedorService proveedorService;

    private String bearer(String... roles) {
        return "Bearer " + jwt.generateAccessToken("pepe", List.of(roles));
    }

    @Test
    void sinToken_es401() throws Exception {
        mvc.perform(get("/ventas/0/10")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenFirmadoConOtroSecreto_es401() throws Exception {
        String forjado = Jwts.builder()
                .setIssuer("proyectodistri").setSubject("hacker")
                .claim("typ", "access").claim("roles", List.of("ADMIN"))
                .setExpiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor("cambia-esto-por-un-secret-muy-largo-y-aleatorio".getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
        mvc.perform(get("/usuarios/0/10").header("Authorization", "Bearer " + forjado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenNoSirveComoAccessToken() throws Exception {
        String refresh = jwt.generateRefreshToken("pepe");
        mvc.perform(get("/ventas/0/10").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSinTipo_comoLosEmitidosAntesDelCambio_es401() throws Exception {
        String viejo = Jwts.builder()
                .setIssuer("proyectodistri").setSubject("pepe").claim("roles", List.of("ADMIN"))
                .setExpiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
        mvc.perform(get("/ventas/0/10").header("Authorization", "Bearer " + viejo))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuariosYRoles_soloAdmin() throws Exception {
        when(usuarioService.list(any(), anyInt(), anyInt())).thenReturn(Page.empty());
        when(rolService.list(any(), anyInt(), anyInt())).thenReturn(Page.empty());

        mvc.perform(get("/usuarios/0/10").header("Authorization", bearer("VENDEDOR"))).andExpect(status().isForbidden());
        mvc.perform(get("/usuarios/0/10").header("Authorization", bearer("USER"))).andExpect(status().isForbidden());
        mvc.perform(post("/roles").header("Authorization", bearer("VENDEDOR"))
                .contentType("application/json").content("{\"nombre\":\"ADMIN\"}")).andExpect(status().isForbidden());

        mvc.perform(get("/usuarios/0/10").header("Authorization", bearer("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/roles/0/10").header("Authorization", bearer("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void anularVenta_soloAdmin() throws Exception {
        mvc.perform(delete("/ventas/1").header("Authorization", bearer("VENDEDOR"))).andExpect(status().isForbidden());
        mvc.perform(delete("/ventas/1").header("Authorization", bearer("ADMIN"))).andExpect(status().isNoContent());
    }

    @Test
    void proveedores_vendedorConsultaPeroNoModifica() throws Exception {
        when(proveedorService.list(any(), anyInt(), anyInt())).thenReturn(Page.empty());
        mvc.perform(get("/proveedores/0/10").header("Authorization", bearer("VENDEDOR"))).andExpect(status().isOk());
        mvc.perform(delete("/proveedores/1").header("Authorization", bearer("VENDEDOR"))).andExpect(status().isForbidden());
    }

    @Test
    void pageSizeExcesivo_es400ConMensaje() throws Exception {
        mvc.perform(get("/ventas/0/100000").header("Authorization", bearer("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size debe estar entre 1 y 200"));
    }

    @Test
    void fechaInvalida_es400() throws Exception {
        mvc.perform(get("/ventas/0/10/1/no-es-fecha").header("Authorization", bearer("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void errorDeNegocio_es409ConMensaje_noEs500() throws Exception {
        when(ventaService.crear(any())).thenThrow(new IllegalStateException("Stock insuficiente para producto id=1"));
        mvc.perform(post("/ventas").header("Authorization", bearer("VENDEDOR"))
                        .contentType("application/json").content("{\"items\":[{\"productoId\":1,\"cantidad\":99}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Stock insuficiente para producto id=1"));
    }
}
