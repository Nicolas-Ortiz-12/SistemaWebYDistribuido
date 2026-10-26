// src/main/java/com/distri/proyectodistri/apis/service/VentaService.java
package com.distri.proyectoVenta.apis.service;

import com.distri.proyectoVenta.apis.entities.cliente.Cliente;
import com.distri.proyectoVenta.apis.entities.inventario.Producto;
import com.distri.proyectoVenta.apis.entities.venta.Venta;
import com.distri.proyectoVenta.apis.entities.venta.VentaDetalle;
import com.distri.proyectoVenta.apis.mapper.VentaRequestMapper;
import com.distri.proyectoVenta.apis.mapper.VentaResponseMapper;
import com.distri.proyectoVenta.apis.repository.ClienteRepository;
import com.distri.proyectoVenta.apis.repository.ProductoRepository;
import com.distri.proyectoVenta.apis.repository.UsuarioRepository;
import com.distri.proyectoVenta.apis.repository.VentaRepository;
import com.distri.proyectoVenta.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class VentaService {

    // Tasas de IVA vigentes. El cliente elige una, pero no puede mandar cualquier valor
    // (una tasa negativa bajaba el total de la venta).
    static final List<BigDecimal> TASAS_IVA = List.of(new BigDecimal("0"), new BigDecimal("5"), new BigDecimal("10"));
    static final BigDecimal TASA_IVA_DEFAULT = new BigDecimal("10");

    // Mismas claves que usa ProductoService para cachear cada producto
    static final String CACHE = "sd";
    static final String CACHE_PRODUCTO_PREFIX = "api_producto_";

    private static final LocalDateTime FECHA_MIN = LocalDateTime.of(1900, 1, 1, 0, 0);
    private static final LocalDateTime FECHA_MAX = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final VentaRequestMapper ventaRequestMapper;
    private final VentaResponseMapper ventaResponseMapper;

    private final CacheManager redisCacheManager;

    // =======================
    // CREATE
    // =======================
    @Transactional
    @CachePut(value = "sd", key = "'api_venta_' + #result.id", unless = "#result == null")
    public VentaResponseDTO crear(VentaRequestDTO req) {
        log.info("Creando nueva venta para clienteId={}", req.getClienteId());
        validarItems(req.getItems());

        Venta venta = ventaRequestMapper.toEntity(req);

        if (req.getClienteId() != null) {
            venta.setCliente(buscarCliente(req.getClienteId()));
        }

        if (req.getFechaVenta() != null) {
            venta.setFechaVenta(enUtc(req.getFechaVenta()));
        }

        // Auditoría: quién registró la venta (si el usuario del token existe en la tabla usuario)
        usuarioActual().flatMap(usuarioRepository::findByUsuario).ifPresent(venta::setCreadoPor);

        Set<Long> tocados = aplicarItems(venta, req.getItems());

        venta = ventaRepository.save(venta);
        log.debug("Venta registrada exitosamente con id={}", venta.getId());
        evictProductosAfterCommit(tocados);
        return ventaResponseMapper.toDto(venta);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "sd", key = "'api_venta_' + #id")
    public VentaResponseDTO obtenerPorId(Long id) {
        log.debug("Buscando venta por id={}", id);
        Venta v = ventaRepository.findById(id)
                .orElseThrow(() ->{
                    log.error("Venta no encontrada: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Venta no encontrada: " + id);
                });
        log.debug("Venta encontrada: {}", v);
        return ventaResponseMapper.toDto(v);
    }

    // =======================
    // LIST (con filtros opcionales)
    // =======================
    @Transactional(readOnly = true)
    public Page<VentaResponseDTO> listar(Pageable pageable, Long clienteId, OffsetDateTime desde, OffsetDateTime hasta) {
        log.info("Listando ventas - clienteId={} desde={} hasta={}", clienteId, desde, hasta);
        Page<Venta> page;
        boolean filtraFecha = desde != null || hasta != null;
        // Si viene una sola fecha, el otro extremo queda abierto
        LocalDateTime d = desde != null ? enUtc(desde) : FECHA_MIN;
        LocalDateTime h = hasta != null ? enUtc(hasta) : FECHA_MAX;

        if (clienteId != null && filtraFecha) {
            page = ventaRepository.findByCliente_IdAndFechaVentaBetween(clienteId, d, h, pageable);
        } else if (clienteId != null) {
            page = ventaRepository.findByCliente_Id(clienteId, pageable);
        } else if (filtraFecha) {
            page = ventaRepository.findByFechaVentaBetween(d, h, pageable);
        } else {
            page = ventaRepository.findAll(pageable);
        }
        log.debug("Cantidad de ventas encontradas: {}", page.getTotalElements());
        return page.map(ventaResponseMapper::toDto);
    }

    // UPDATE (revertir stock, aplicar nuevos items)
    @Transactional
    @CachePut(value = "sd", key = "'api_venta_' + #result.id", unless = "#result == null")
    public VentaResponseDTO actualizar(Long id, VentaRequestDTO req) {
        log.info("Actualizando venta con id={}", id);
        validarItems(req.getItems());

        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Venta no encontrada: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Venta no encontrada: " + id);
                });

        // devolver stock de los detalles actuales
        Set<Long> tocados = new HashSet<>();
        log.debug("Devolviendo stock de venta previa id={}", id);
        for (VentaDetalle det : venta.getDetalles()) {
            Producto p = bloquearProducto(det.getProducto().getId());
            p.setStockMinimo(nz(p.getStockMinimo()).add(det.getCantidad()));
            tocados.add(p.getId());
        }
        venta.getDetalles().clear();
        // Borra los detalles viejos ANTES de insertar los nuevos: Hibernate ejecuta los INSERT antes que
        // los DELETE de huérfanos, y volver a cargar el mismo producto violaba el unique (venta_id, producto_id).
        ventaRepository.flush();

        // actualizar cabecera (cliente / fecha)
        if (req.getClienteId() != null) {
            venta.setCliente(buscarCliente(req.getClienteId()));
        }
        if (req.getFechaVenta() != null) {
            // misma conversión que al crear (DateMapper.asLocalDateTime): hora local del request
            venta.setFechaVenta(enUtc(req.getFechaVenta()));
        }

        tocados.addAll(aplicarItems(venta, req.getItems()));

        venta = ventaRepository.save(venta);
        log.debug("Venta actualizada correctamente id={}", venta.getId());
        evictProductosAfterCommit(tocados);
        return ventaResponseMapper.toDto(venta);
    }

    @Transactional
    @CacheEvict(value = "sd", key = "'api_venta_' + #id")
    public void eliminar(Long id) {
        log.debug("Eliminando (soft) venta id={}", id);
        // Trae SOLO ventas activas (si ya estaba inactiva => 404 / 400)
        Venta venta = ventaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> {
                    log.error("Venta no encontrada o inactiva: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Venta no encontrada: " + id);
                });
        // Restituir stock
        log.debug("Restituyendo stock de productos para venta id={}", id);
        Set<Long> tocados = new HashSet<>();
        for (VentaDetalle det : venta.getDetalles()) {
            Producto p = bloquearProducto(det.getProducto().getId());
            p.setStockMinimo(nz(p.getStockMinimo()).add(det.getCantidad()));
            det.setActivo(false);
            tocados.add(p.getId());
        }
        // Soft delete: marcar venta como inactiva
        venta.setActivo(false);
        // Persistir cambios (venta + detalles + productos)
        ventaRepository.save(venta);
        evictProductosAfterCommit(tocados);
        log.info("Venta marcada como inactiva id={}", id);
    }

    // =======================
    // helpers
    // =======================

    private void validarItems(List<VentaDetalleRequestDTO> items) {
        if (items == null || items.isEmpty()) {
            log.error("La venta no posee ítems");
            throw new IllegalArgumentException("La venta debe tener al menos un ítem");
        }
        Set<Long> vistos = new HashSet<>();
        for (VentaDetalleRequestDTO item : items) {
            if (item.getProductoId() == null) {
                log.error("Falta productoId en ítem");
                throw new IllegalArgumentException("Falta productoId en ítem");
            }
            if (!vistos.add(item.getProductoId())) {
                throw new IllegalArgumentException("Producto repetido en la venta: " + item.getProductoId() + " (sumar las cantidades en un solo ítem)");
            }
        }
    }

    /** Arma los detalles con el precio real del producto, descuenta stock y calcula los totales. */
    private Set<Long> aplicarItems(Venta venta, List<VentaDetalleRequestDTO> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;
        Set<Long> tocados = new HashSet<>();

        for (VentaDetalleRequestDTO item : items) {
            Long productoId = item.getProductoId();
            Producto producto = bloquearProducto(productoId);

            BigDecimal cantidad = toBD(item.getCantidad());
            if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("Cantidad debe ser mayor que cero para producto id={}", productoId);
                throw new IllegalArgumentException("Cantidad debe ser > 0 para producto id=" + productoId);
            }

            BigDecimal stock = nz(producto.getStockMinimo());
            if (stock.compareTo(cantidad) < 0) {
                log.error("Stock insuficiente para producto id={} (stock actual={})", productoId, stock);
                throw new IllegalStateException("Stock insuficiente para producto id=" + productoId);
            }

            BigDecimal precioUnit = nz(producto.getPrecio()); // precio real, no el del request
            BigDecimal tasaIvaPct = tasaIva(item.getTasaIva());

            BigDecimal subLinea = precioUnit.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            BigDecimal ivaLinea = subLinea.multiply(tasaIvaPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal totalLinea = subLinea.add(ivaLinea);

            VentaDetalle det = new VentaDetalle();
            det.setVenta(venta);
            det.setProducto(producto);
            det.setCantidad(cantidad);
            det.setPrecioUnitario(precioUnit);
            det.setTasaIva(tasaIvaPct);
            det.setSubtotalLinea(subLinea);
            det.setTotalLinea(totalLinea);

            venta.getDetalles().add(det);

            // descontar stock
            producto.setStockMinimo(stock.subtract(cantidad));
            tocados.add(productoId);

            subtotal = subtotal.add(subLinea);
            iva = iva.add(ivaLinea);
            log.debug("Detalle agregado productoId={} cantidad={} subtotal={}", productoId, cantidad, subLinea);
        }

        venta.setSubtotal(subtotal);
        venta.setIva(iva);
        venta.setTotal(subtotal.add(iva));
        return tocados;
    }

    static BigDecimal tasaIva(Number pedida) {
        if (pedida == null) return TASA_IVA_DEFAULT;
        BigDecimal t = toBD(pedida);
        return TASAS_IVA.stream().filter(v -> v.compareTo(t) == 0).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tasa de IVA inválida: " + pedida + " (permitidas: 0, 5, 10)"));
    }

    // Número de factura: se arma con el id de la venta, 001-001-0000024
    public static String formatoNumero(long id) {
        return String.format("001-001-%07d", id);
    }

    @Transactional(readOnly = true)
    public String proximoNumero() {
        return formatoNumero(ventaRepository.proximoId());
    }

    // fecha_venta se guarda en UTC (la respuesta la devuelve con offset Z): una fecha con
    // offset local se convierte al mismo instante en UTC en vez de descartar el offset.
    static LocalDateTime enUtc(OffsetDateTime fecha) {
        return fecha.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private Cliente buscarCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> {
                    log.error("Cliente no encontrado: {}", clienteId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Cliente no existe: " + clienteId);
                });
    }

    private Producto bloquearProducto(Long productoId) {
        return productoRepository.findByIdForUpdate(productoId)
                .orElseThrow(() -> {
                    log.error("Producto no encontrado: {}", productoId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Producto no existe: " + productoId);
                });
    }

    private static java.util.Optional<String> usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? java.util.Optional.empty() : java.util.Optional.ofNullable(auth.getName());
    }

    // El stock cambió: el producto cacheado (ProductoService.get) quedaría con el stock viejo.
    // Se invalida después del commit para que una lectura concurrente no vuelva a cachear el valor anterior.
    private void evictProductosAfterCommit(Set<Long> productoIds) {
        Runnable evict = () -> {
            try {
                Cache cache = redisCacheManager.getCache(CACHE);
                if (cache != null) productoIds.forEach(pid -> cache.evict(CACHE_PRODUCTO_PREFIX + pid));
            } catch (RuntimeException e) {
                log.warn("No se pudo invalidar la cache de productos {}: {}", productoIds, e.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { evict.run(); }
            });
        } else {
            evict.run();
        }
    }

    private static BigDecimal toBD(Number n) { return n == null ? BigDecimal.ZERO : new BigDecimal(n.toString()); }
    private static BigDecimal nz(BigDecimal d) { return d == null ? BigDecimal.ZERO : d; }
}
