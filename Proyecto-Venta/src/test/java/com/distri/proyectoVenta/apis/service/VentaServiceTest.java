package com.distri.proyectoVenta.apis.service;

import com.distri.proyectoVenta.apis.entities.inventario.Producto;
import com.distri.proyectoVenta.apis.entities.venta.Venta;
import com.distri.proyectoVenta.apis.entities.venta.VentaDetalle;
import com.distri.proyectoVenta.apis.mapper.VentaRequestMapper;
import com.distri.proyectoVenta.apis.mapper.VentaResponseMapper;
import com.distri.proyectoVenta.apis.repository.ClienteRepository;
import com.distri.proyectoVenta.apis.repository.ProductoRepository;
import com.distri.proyectoVenta.apis.repository.UsuarioRepository;
import com.distri.proyectoVenta.apis.repository.VentaRepository;
import com.distri.proyectoVenta.dto.VentaDetalleRequestDTO;
import com.distri.proyectoVenta.dto.VentaRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VentaServiceTest {

    @Mock VentaRepository ventaRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ProductoRepository productoRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock VentaRequestMapper ventaRequestMapper;
    @Mock VentaResponseMapper ventaResponseMapper;
    @Mock CacheManager cacheManager;
    @Mock Cache cache;

    @InjectMocks VentaService service;

    Producto martillo;

    @BeforeEach
    void setUp() {
        martillo = producto(1L, "1000", "10");
        when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(martillo));
        when(ventaRequestMapper.toEntity(any())).thenAnswer(inv -> new Venta());
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache("sd")).thenReturn(cache);
    }

    @Test
    void crear_usaPrecioDelProducto_descuentaStock_eInvalidaCacheDelProducto() {
        service.crear(request(item(1L, 2.0, 10.0)));

        Venta v = savedVenta();
        assertThat(v.getSubtotal()).isEqualByComparingTo("2000");
        assertThat(v.getIva()).isEqualByComparingTo("200");
        assertThat(v.getTotal()).isEqualByComparingTo("2200");
        assertThat(martillo.getStockMinimo()).isEqualByComparingTo("8");
        verify(cache).evict("api_producto_1");
    }

    @Test
    void crear_aceptaTasasVigentes() {
        service.crear(request(item(1L, 1.0, 5.0)));
        assertThat(savedVenta().getIva()).isEqualByComparingTo("50");
    }

    @Test
    void crear_rechazaTasaIvaNegativa_sinTocarStock() {
        assertThatThrownBy(() -> service.crear(request(item(1L, 2.0, -100.0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tasa de IVA");
        assertThat(martillo.getStockMinimo()).isEqualByComparingTo("10");
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void crear_rechazaStockInsuficiente() {
        assertThatThrownBy(() -> service.crear(request(item(1L, 11.0, 10.0))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void crear_rechazaProductoRepetido() {
        assertThatThrownBy(() -> service.crear(request(item(1L, 1.0, 10.0), item(1L, 1.0, 10.0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("repetido");
    }

    @Test
    void crear_rechazaVentaSinItems() {
        assertThatThrownBy(() -> service.crear(request()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actualizar_devuelveStockViejo_borraDetallesAntesDeInsertar_yAplicaNuevos() {
        // Venta existente: 3 martillos vendidos (stock quedó en 7)
        martillo.setStockMinimo(new BigDecimal("7"));
        Venta existente = new Venta();
        existente.setId(50L);
        existente.getDetalles().add(detalle(existente, martillo, "3"));
        when(ventaRepository.findById(50L)).thenReturn(Optional.of(existente));

        // Se edita para vender 5 del MISMO producto (antes rompía el unique venta_id+producto_id)
        service.actualizar(50L, request(item(1L, 5.0, 10.0)));

        assertThat(martillo.getStockMinimo()).isEqualByComparingTo("5"); // 7 + 3 - 5
        assertThat(existente.getDetalles()).hasSize(1);
        assertThat(existente.getDetalles().get(0).getCantidad()).isEqualByComparingTo("5");
        assertThat(existente.getTotal()).isEqualByComparingTo("5500");
        InOrder order = inOrder(ventaRepository);
        order.verify(ventaRepository).flush();
        order.verify(ventaRepository).save(existente);
    }

    @Test
    void actualizar_rechazaCantidadNegativa() {
        Venta existente = new Venta();
        when(ventaRepository.findById(50L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.actualizar(50L, request(item(1L, -5.0, 10.0))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actualizar_sinItems_es400YNoNullPointer() {
        VentaRequestDTO req = new VentaRequestDTO();
        req.setItems(null);
        assertThatThrownBy(() -> service.actualizar(50L, req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void numeroDeFactura_siguePrefijoYRellenaConCeros() {
        assertThat(VentaService.formatoNumero(24)).isEqualTo("001-001-0000024");
        assertThat(VentaService.formatoNumero(1234567)).isEqualTo("001-001-1234567");
    }

    @Test
    void proximoNumero_usaElSiguienteIdDeLaSecuencia() {
        when(ventaRepository.proximoId()).thenReturn(24L);
        assertThat(service.proximoNumero()).isEqualTo("001-001-0000024");
    }

    @Test
    void fechaConOffsetLocal_seGuardaEnUtc() {
        // 20/10 a las 21:30 en Paraguay (-03:00) es el 21/10 a las 00:30 UTC
        assertThat(VentaService.enUtc(OffsetDateTime.parse("2026-10-20T21:30:00-03:00")))
                .isEqualTo(LocalDateTime.of(2026, 10, 21, 0, 30));
        assertThat(VentaService.enUtc(OffsetDateTime.parse("2026-10-20T10:00:00Z")))
                .isEqualTo(LocalDateTime.of(2026, 10, 20, 10, 0));
    }

    @Test
    void eliminar_devuelveStock() {
        martillo.setStockMinimo(new BigDecimal("7"));
        Venta existente = new Venta();
        existente.getDetalles().add(detalle(existente, martillo, "3"));
        when(ventaRepository.findByIdAndActivoTrue(50L)).thenReturn(Optional.of(existente));

        service.eliminar(50L);

        assertThat(martillo.getStockMinimo()).isEqualByComparingTo("10");
        assertThat(existente.isActivo()).isFalse();
        verify(cache).evict("api_producto_1");
    }

    @Test
    void listar_conSoloFechaDesde_filtraConHastaAbierto() {
        when(ventaRepository.findByFechaVentaBetween(any(), any(), any())).thenReturn(Page.empty());
        OffsetDateTime desde = OffsetDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC);

        service.listar(PageRequest.of(0, 10), null, desde, null);

        ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(ventaRepository).findByFechaVentaBetween(eq(desde.toLocalDateTime()), hasta.capture(), any());
        assertThat(hasta.getValue().getYear()).isEqualTo(9999);
    }

    // ---------- helpers ----------

    private Venta savedVenta() {
        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(captor.capture());
        return captor.getValue();
    }

    private static Producto producto(Long id, String precio, String stock) {
        Producto p = new Producto();
        p.setId(id);
        p.setPrecio(new BigDecimal(precio));
        p.setStockMinimo(new BigDecimal(stock));
        return p;
    }

    private static VentaDetalle detalle(Venta v, Producto p, String cantidad) {
        VentaDetalle d = new VentaDetalle();
        d.setVenta(v);
        d.setProducto(p);
        d.setCantidad(new BigDecimal(cantidad));
        return d;
    }

    private static VentaDetalleRequestDTO item(Long productoId, Double cantidad, Double tasaIva) {
        VentaDetalleRequestDTO i = new VentaDetalleRequestDTO();
        i.setProductoId(productoId);
        i.setCantidad(cantidad);
        i.setTasaIva(tasaIva);
        return i;
    }

    private static VentaRequestDTO request(VentaDetalleRequestDTO... items) {
        VentaRequestDTO r = new VentaRequestDTO();
        r.setItems(new java.util.ArrayList<>(List.of(items)));
        return r;
    }
}
