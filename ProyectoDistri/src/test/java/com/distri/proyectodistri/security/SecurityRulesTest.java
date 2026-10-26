package com.distri.proyectodistri.security;

import com.distri.proyectodistri.apis.controller.CompraController;
import com.distri.proyectodistri.apis.controller.ProductoController;
import com.distri.proyectodistri.apis.controller.UsuarioController;
import com.distri.proyectodistri.apis.service.CompraService;
import com.distri.proyectodistri.apis.service.ProductoService;
import com.distri.proyectodistri.apis.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Los controllers duplicados de este micro (productos, usuarios, ...) no pasan por el gateway,
// pero se llega a ellos entrando directo al Tomcat (/Compra/...). Antes cualquier token servía.
@WebMvcTest(controllers = {ProductoController.class, UsuarioController.class, CompraController.class})
@Import({SecurityConfig.class, JwtService.class, RestAuthEntryPoint.class, RestAccessDeniedHandler.class})
@TestPropertySource(properties = {
        "security.jwt.secret=test-secret-0123456789-0123456789-0123456789",
        "security.jwt.issuer=proyectodistri",
        "security.jwt.expiration-minutes=60",
        "security.jwt.refresh-expiration-days=7",
        "eureka.client.enabled=false"
})
class SecurityRulesTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;

    @MockitoBean ProductoService productoService;
    @MockitoBean UsuarioService usuarioService;
    @MockitoBean CompraService compraService;

    private String bearer(String... roles) {
        return "Bearer " + jwt.generateAccessToken("pepe", List.of(roles));
    }

    @Test
    void productos_requierenAdminOVendedor() throws Exception {
        when(productoService.list(any(), any(), anyInt(), anyInt())).thenReturn(Page.empty());
        mvc.perform(get("/productos/0/10").header("Authorization", bearer("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/productos/0/10").header("Authorization", bearer("VENDEDOR"))).andExpect(status().isOk());
    }

    @Test
    void usuarios_soloAdmin() throws Exception {
        when(usuarioService.list(any(), anyInt(), anyInt())).thenReturn(Page.empty());
        mvc.perform(get("/usuarios/0/10").header("Authorization", bearer("VENDEDOR"))).andExpect(status().isForbidden());
        mvc.perform(get("/usuarios/0/10").header("Authorization", bearer("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void refreshToken_noSirve() throws Exception {
        mvc.perform(get("/compras/0/10").header("Authorization", "Bearer " + jwt.generateRefreshToken("pepe")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anularCompra_soloAdmin() throws Exception {
        mvc.perform(delete("/compras/1").header("Authorization", bearer("VENDEDOR"))).andExpect(status().isForbidden());
        mvc.perform(delete("/compras/1").header("Authorization", bearer("ADMIN"))).andExpect(status().isNoContent());
    }
}
