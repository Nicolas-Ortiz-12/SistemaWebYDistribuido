package com.distri.proyectodistri.apis.service;

import com.distri.proyectodistri.apis.entities.compra.Compra;
import com.distri.proyectodistri.apis.entities.compra.CompraDetalle;
import com.distri.proyectodistri.apis.entities.inventario.Producto;
import com.distri.proyectodistri.apis.mapper.CompraRequestMapper;
import com.distri.proyectodistri.apis.mapper.CompraResponseMapper;
import com.distri.proyectodistri.apis.repository.CompraRepository;
import com.distri.proyectodistri.apis.repository.ProductoRepository;
import com.distri.proyectodistri.apis.repository.ProveedorRepository;
import com.distri.proyectodistri.dto.CompraDetalleRequestDTO;
import com.distri.proyectodistri.dto.CompraRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompraServiceTest {

    @Mock CompraRepository compraRepository;
    @Mock ProveedorRepository proveedorRepository;
    @Mock ProductoRepository productoRepository;
    @Mock CompraRequestMapper compraRequestMapper;
    @Mock CompraResponseMapper compraResponseMapper;
    @Mock CacheManager cacheManager;
    @Mock Cache cache;

    @InjectMocks CompraService service;

    Producto clavo;

    @BeforeEach
    void setUp() {
        clavo = new Producto();
        clavo.setId(7L);
        clavo.setCosto(new BigDecimal("100"));
        clavo.setStockMinimo(new BigDecimal("10"));
        when(productoRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(clavo));
        when(compraRequestMapper.toEntity(any())).thenAnswer(inv -> new Compra());
        when(compraRepository.save(any(Compra.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cacheManager.getCache("sd")).thenReturn(cache);
    }

    @Test
    void crear_sumaStock_actualizaCosto_eInvalidaCacheDelProducto() {
        service.crear(request(item(7L, 5.0, 120.0, 10.0)));

        assertThat(clavo.getStockMinimo()).isEqualByComparingTo("15");
        assertThat(clavo.getCosto()).isEqualByComparingTo("120");
        verify(cache).evict("api_producto_7");
    }

    @Test
    void crear_rechazaUnComprobanteYaCargadoParaElMismoProveedor() {
        CompraRequestDTO req = request(item(7L, 1.0, 100.0, 10.0));
        req.setProveedorId(5L);
        req.setNumero("001-001-0000456");
        when(proveedorRepository.findById(5L)).thenReturn(Optional.of(new com.distri.proyectodistri.apis.entities.cliente.Proveedor()));
        when(compraRepository.existsByProveedor_IdAndNumero(5L, "001-001-0000456")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(req))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("001-001-0000456");
        verify(compraRepository, never()).save(any());
    }

    @Test
    void eliminar_restaElStockQueHabiaIngresado() {
        // Antes sumaba: borrar una compra de 4 dejaba el stock en 14 en vez de 6
        when(compraRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(compraCon(clavo, "4")));

        service.eliminar(1L);

        assertThat(clavo.getStockMinimo()).isEqualByComparingTo("6");
    }

    @Test
    void eliminar_siLoCompradoYaSeVendio_noDejaStockNegativo() {
        clavo.setStockMinimo(new BigDecimal("2"));
        Compra compra = compraCon(clavo, "4");
        when(compraRepository.findByIdAndActivoTrue(1L)).thenReturn(Optional.of(compra));

        assertThatThrownBy(() -> service.eliminar(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("negativo");
        verify(compraRepository, never()).save(any());
    }

    @Test
    void actualizar_mismoProducto_flushAntesDeInsertar() {
        // compra de 4 (stock 10 → sin vender) editada a 6 del mismo producto
        Compra compra = compraCon(clavo, "4");
        when(compraRepository.findById(1L)).thenReturn(Optional.of(compra));

        service.actualizar(1L, request(item(7L, 6.0, 100.0, 10.0)));

        assertThat(clavo.getStockMinimo()).isEqualByComparingTo("12"); // 10 - 4 + 6
        assertThat(compra.getDetalles()).hasSize(1);
        InOrder order = inOrder(compraRepository);
        order.verify(compraRepository).flush();
        order.verify(compraRepository).save(compra);
    }

    @Test
    void rechazaTasaIvaInvalida() {
        assertThatThrownBy(() -> service.crear(request(item(7L, 1.0, 100.0, 12.5))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tasa de IVA");
    }

    private static Compra compraCon(Producto p, String cantidad) {
        Compra c = new Compra();
        CompraDetalle d = new CompraDetalle();
        d.setCompra(c);
        d.setProducto(p);
        d.setCantidad(new BigDecimal(cantidad));
        c.getDetalles().add(d);
        return c;
    }

    private static CompraDetalleRequestDTO item(Long productoId, Double cantidad, Double costo, Double tasaIva) {
        CompraDetalleRequestDTO i = new CompraDetalleRequestDTO();
        i.setProductoId(productoId);
        i.setCantidad(cantidad);
        i.setCostoUnitario(costo);
        i.setTasaIva(tasaIva);
        return i;
    }

    private static CompraRequestDTO request(CompraDetalleRequestDTO... items) {
        CompraRequestDTO r = new CompraRequestDTO();
        r.setDetalles(new ArrayList<>(List.of(items)));
        return r;
    }
}
