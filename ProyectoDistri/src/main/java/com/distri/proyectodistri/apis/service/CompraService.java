// src/main/java/com/distri/proyectodistri/apis/service/CompraService.java
package com.distri.proyectodistri.apis.service;

import com.distri.proyectodistri.apis.entities.compra.Compra;
import com.distri.proyectodistri.apis.entities.compra.CompraDetalle;
import com.distri.proyectodistri.apis.entities.inventario.Producto;
import com.distri.proyectodistri.apis.entities.cliente.Proveedor;
import com.distri.proyectodistri.apis.mapper.CompraRequestMapper;
import com.distri.proyectodistri.apis.mapper.CompraResponseMapper;
import com.distri.proyectodistri.apis.repository.*;
import com.distri.proyectodistri.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompraService {

    // Tasas de IVA vigentes. Se valida la que manda el cliente en lugar de aceptar cualquier valor.
    static final List<BigDecimal> TASAS_IVA = List.of(new BigDecimal("0"), new BigDecimal("5"), new BigDecimal("10"));
    static final BigDecimal TASA_IVA_DEFAULT = new BigDecimal("10");

    // El producto lo cachea Proyecto-Venta (ProductoService) en la cache "sd" con clave "api_producto_{id}".
    // Ambos servicios usan el mismo Redis y el mismo nombre de cache, así que la clave es la misma.
    static final String CACHE = "sd";
    static final String CACHE_PRODUCTO_PREFIX = "api_producto_";

    private static final LocalDate FECHA_MIN = LocalDate.of(1900, 1, 1);
    private static final LocalDate FECHA_MAX = LocalDate.of(9999, 12, 31);

    private final CompraRepository compraRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final CompraRequestMapper compraRequestMapper;
    private final CompraResponseMapper compraResponseMapper;
    private final CacheManager cacheManager;

    // CREATE
    @Transactional
    @CachePut(value = "sd", key = "'api_compra_' + #result.id", unless = "#result == null")
    public CompraResponseDTO crear(CompraRequestDTO req) {
        log.info("Creando nueva compra con proveedorId={}", req.getProveedorId());
        validarItems(req.getDetalles());

        Compra compra = compraRequestMapper.toEntity(req);

        if (req.getProveedorId() != null) {
            compra.setProveedor(buscarProveedor(req.getProveedorId()));
            validarComprobanteNuevo(req.getProveedorId(), req.getNumero());
        }

        if (req.getFechaEmision() != null) {
            compra.setFechaEmision(req.getFechaEmision());
        }

        Map<Long, Producto> tocados = new HashMap<>();
        aplicarItems(compra, req.getDetalles(), true, tocados);

        compra = compraRepository.save(compra);
        log.debug("Compra registrada: {}", compra);
        evictProductosAfterCommit(tocados.keySet());
        return compraResponseMapper.toDto(compra);
    }

    // GET BY ID
    @Transactional(readOnly = true)
    @Cacheable(value = "sd", key = "'api_compra_' + #id")
    public CompraResponseDTO obtenerPorId(Long id) {
        log.debug("Obteniendo compra por id: {}", id);
        Compra c = compraRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Compra no encontrado: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Compra no encontrada");
                });
        log.debug("Compra encontrada por id: {}", c);
        return compraResponseMapper.toDto(c);
    }

    @Transactional(readOnly = true)
    public Page<CompraResponseDTO> listar(Pageable pageable, Long proveedorId,OffsetDateTime desde, OffsetDateTime hasta) {
        log.info("Trayendo lista de compras realizadas pagina={} por el proveedor con ID={} desde={} hasta={}",pageable,proveedorId, desde, hasta);
        Page<Compra> page;

        boolean filtraFecha = desde != null || hasta != null;
        // Si viene una sola fecha, el otro extremo queda abierto
        LocalDate d = (desde != null) ? desde.toLocalDate() : FECHA_MIN;
        LocalDate h = (hasta != null) ? hasta.toLocalDate() : FECHA_MAX;

        if (proveedorId != null && filtraFecha) {
            page = compraRepository.findByProveedor_IdAndFechaEmisionBetween(proveedorId, d, h, pageable);
        } else if (proveedorId != null) {
            page = compraRepository.findByProveedor_Id(proveedorId, pageable);
        } else if (filtraFecha) {
            page = compraRepository.findByFechaEmisionBetween(d, h, pageable);
        } else {
            page = compraRepository.findAll(pageable);
        }
        log.debug("Cantidad de compras encontradas: {}", page.getTotalElements());
        return page.map(compraResponseMapper::toDto);
    }

    // UPDATE (revertir stock anterior y aplicar nuevos ítems)
    @Transactional
    @CachePut(value = "sd", key = "'api_compra_' + #result.id", unless = "#result == null")
    public CompraResponseDTO actualizar(Long id, CompraRequestDTO req) {
        log.info("Actualizando compra con Id={}", id);
        validarItems(req.getDetalles());

        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Compra no encontrada: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Compra no encontrada: " + id);
                });

        // devolver stock de detalles actuales (restar lo agregado previamente)
        Map<Long, Producto> tocados = new HashMap<>();
        for (CompraDetalle det : compra.getDetalles()) {
            Producto p = bloquearProducto(det.getProducto().getId());
            p.setStockMinimo(nz(p.getStockMinimo()).subtract(det.getCantidad()));
            tocados.put(p.getId(), p);
        }
        compra.getDetalles().clear();
        // Borra los detalles viejos ANTES de insertar los nuevos: Hibernate ejecuta los INSERT antes que
        // los DELETE de huérfanos, y volver a cargar el mismo producto violaba el unique (compra_id, producto_id).
        compraRepository.flush();

        if (req.getProveedorId() != null) {
            compra.setProveedor(buscarProveedor(req.getProveedorId()));
        }

        if (req.getFechaEmision() != null) {
            compra.setFechaEmision(req.getFechaEmision());
        }

        aplicarItems(compra, req.getDetalles(), false, tocados);

        // Si bajó la cantidad comprada de algo que ya se vendió, el stock quedaría negativo
        verificarStockNoNegativo(tocados.values());

        compra = compraRepository.save(compra);
        log.debug("Compra actualizada correctamente con id: {}", compra.getId());
        evictProductosAfterCommit(tocados.keySet());
        return compraResponseMapper.toDto(compra);
    }

    // DELETE (Quita stock previo de la compra)
    @Transactional
    @CacheEvict(value = "sd", key = "'api_compra_' + #id")
    public void eliminar(Long id) {
        log.debug("Eliminando compra con id={}", id);
        // Trae SOLO Compras activas
        Compra compra = compraRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> {
                    log.error("Compra no encontrada o inactiva: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Compra no encontrada: " + id);
                });
        // Quitar de stock lo que había ingresado esta compra
        log.debug("Quitando stock de productos comprados id={}", id);
        Map<Long, Producto> tocados = new HashMap<>();
        for (CompraDetalle det : compra.getDetalles()) {
            Producto p = bloquearProducto(det.getProducto().getId());
            p.setStockMinimo(nz(p.getStockMinimo()).subtract(det.getCantidad()));
            det.setActivo(false);
            tocados.put(p.getId(), p);
        }
        verificarStockNoNegativo(tocados.values());
        // Soft delete: marcar compra como inactiva
        compra.setActivo(false);
        compraRepository.save(compra);
        evictProductosAfterCommit(tocados.keySet());
        log.info("Compra marcada como inactiva id={}", id);
    }

    // helpers

    private void validarItems(List<CompraDetalleRequestDTO> items) {
        if (items == null || items.isEmpty()) {
            log.error("La compra no posee algun item");
            throw new IllegalArgumentException("La compra debe tener al menos un ítem");
        }
        Set<Long> vistos = new HashSet<>();
        for (CompraDetalleRequestDTO item : items) {
            if (item.getProductoId() == null) {
                log.error("No se a puesto el ProductoId");
                throw new IllegalArgumentException("Falta productoId en ítem");
            }
            if (!vistos.add(item.getProductoId())) {
                throw new IllegalArgumentException("Producto repetido en la compra: " + item.getProductoId() + " (sumar las cantidades en un solo ítem)");
            }
        }
    }

    /** Arma los detalles, suma stock y calcula los totales. */
    private void aplicarItems(Compra compra, List<CompraDetalleRequestDTO> items, boolean actualizarCosto,
                              Map<Long, Producto> tocados) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;

        for (CompraDetalleRequestDTO item : items) {
            Long productoId = item.getProductoId();
            Producto producto = bloquearProducto(productoId);

            BigDecimal cantidad = toBD(item.getCantidad());
            BigDecimal costoUnit = toBD(item.getCostoUnitario());
            if (cantidad.compareTo(BigDecimal.ZERO) <= 0 || costoUnit.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("Cantidad y costoUnitario deben ser > 0 (producto id={})", productoId);
                throw new IllegalArgumentException("Cantidad y costoUnitario deben ser > 0 (producto id=" + productoId + ")");
            }

            BigDecimal tasaIvaPct = tasaIva(item.getTasaIva());

            BigDecimal subLinea = costoUnit.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            BigDecimal ivaLinea = subLinea.multiply(tasaIvaPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal totalLinea = subLinea.add(ivaLinea);

            CompraDetalle det = new CompraDetalle();
            det.setCompra(compra);
            det.setProducto(producto);
            det.setCantidad(cantidad);
            det.setCostoUnitario(costoUnit);
            det.setTasaIva(tasaIvaPct);
            det.setSubtotalLinea(subLinea);
            det.setTotalLinea(totalLinea);

            compra.getDetalles().add(det);

            // Aumentar stock (stockMinimo es el stock actual)
            producto.setStockMinimo(nz(producto.getStockMinimo()).add(cantidad));

            // Al registrar una compra nueva, el costo del producto pasa a ser el último costo de compra
            if (actualizarCosto) producto.setCosto(costoUnit);
            tocados.put(productoId, producto);

            subtotal = subtotal.add(subLinea);
            iva = iva.add(ivaLinea);
        }

        compra.setSubtotal(subtotal);
        compra.setIva(iva);
        compra.setTotal(subtotal.add(iva));
    }

    private void verificarStockNoNegativo(Iterable<Producto> productos) {
        for (Producto p : productos) {
            if (nz(p.getStockMinimo()).compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("No se puede: el stock del producto id=" + p.getId()
                        + " quedaría negativo (parte de lo comprado ya se vendió)");
            }
        }
    }

    static BigDecimal tasaIva(Number pedida) {
        if (pedida == null) return TASA_IVA_DEFAULT;
        BigDecimal t = toBD(pedida);
        return TASAS_IVA.stream().filter(v -> v.compareTo(t) == 0).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tasa de IVA inválida: " + pedida + " (permitidas: 0, 5, 10)"));
    }

    private void validarComprobanteNuevo(Long proveedorId, String numero) {
        if (numero != null && !numero.isBlank() && compraRepository.existsByProveedor_IdAndNumero(proveedorId, numero.trim())) {
            log.error("Comprobante duplicado: proveedorId={} numero={}", proveedorId, numero);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya se registró el comprobante " + numero.trim() + " de este proveedor.");
        }
    }

    private Proveedor buscarProveedor(Long proveedorId) {
        return proveedorRepository.findById(proveedorId)
                .orElseThrow(() -> {
                    log.error("Proveedor no encontrado: {}", proveedorId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Proveedor no existe: " + proveedorId);
                });
    }

    private Producto bloquearProducto(Long productoId) {
        return productoRepository.findByIdForUpdate(productoId)
                .orElseThrow(() -> {
                    log.error("Producto no encontrado: {}", productoId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Producto no existe: " + productoId);
                });
    }

    // El stock cambió: el producto cacheado por Proyecto-Venta quedaría con el stock viejo.
    // Se invalida después del commit para que una lectura concurrente no vuelva a cachear el valor anterior.
    private void evictProductosAfterCommit(Set<Long> productoIds) {
        Set<Long> ids = Set.copyOf(productoIds);
        Runnable evict = () -> {
            try {
                Cache cache = cacheManager.getCache(CACHE);
                if (cache != null) ids.forEach(pid -> cache.evict(CACHE_PRODUCTO_PREFIX + pid));
            } catch (RuntimeException e) {
                log.warn("No se pudo invalidar la cache de productos {}: {}", ids, e.getMessage());
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
