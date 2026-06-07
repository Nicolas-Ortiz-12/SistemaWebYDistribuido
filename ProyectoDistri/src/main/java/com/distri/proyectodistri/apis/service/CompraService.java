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
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompraService {

    private final CompraRepository compraRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final CompraRequestMapper compraRequestMapper;
    private final CompraResponseMapper compraResponseMapper;
    private final CacheManager redisCacheManager;

    // CREATE
    @Transactional
    @CachePut(value = "compras", key = "'api_compra_' + #result.id", unless = "#result == null")
    public CompraResponseDTO crear(CompraRequestDTO req) {
        log.info("Creando nueva compra con proveedorId={}", req.getProveedorId());
        if (req.getDetalles() == null || req.getDetalles().isEmpty()) {
            log.error("La nueva compra no posee algun item");
            throw new IllegalArgumentException("La compra debe tener al menos un ítem");
        }

        Compra compra = compraRequestMapper.toEntity(req);

        if (req.getProveedorId() != null) {
            Proveedor prov = proveedorRepository.findById(req.getProveedorId())
                    .orElseThrow(() -> {
                        log.error("Proveedor no encontrado: {}", req.getProveedorId());
                        return new ResponseStatusException(HttpStatus.NOT_FOUND,"Proveedor no existe: " + req.getProveedorId());
                    });
            compra.setProveedor(prov);
        }

        if (req.getFechaEmision() != null) {
            compra.setFechaEmision(req.getFechaEmision());
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;

        for (CompraDetalleRequestDTO item : req.getDetalles()) {
            Long productoId = item.getProductoId();
            if (productoId == null){
                log.error("No se a puesto el ProductoId");
                throw new IllegalArgumentException("Falta productoId en ítem");}

            Producto producto = productoRepository.findByIdForUpdate(productoId)
                    .orElseThrow(() -> {
                        log.error("Producto no encontrado: {}", productoId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND,"Producto no existe: " + productoId);
                    });

            BigDecimal cantidad = toBD(item.getCantidad());
            if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("La cantidad debe ser mayor a zero");
                throw new IllegalArgumentException("Cantidad debe ser > 0 para producto id=" + productoId);
            }

            // costo unitario viene del request (compra)
            BigDecimal costoUnit = toBD(item.getCostoUnitario());
            if (costoUnit.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("El costo debe ser mayor a zero");
                throw new IllegalArgumentException("Costo unitario debe ser > 0 para producto id=" + productoId);
            }

            BigDecimal tasaIvaPct = item.getTasaIva() != null ? toBD(item.getTasaIva()) : new BigDecimal("10");

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

            // Aumentar stock (usamos stockMinimo como stock actual tipo Double)
            double stockActual = nz(producto.getStockMinimo());
            producto.setStockMinimo(stockActual + cantidad.doubleValue());

            producto.setCosto(costoUnit.doubleValue());

            subtotal = subtotal.add(subLinea);
            iva = iva.add(ivaLinea);
        }

        compra.setSubtotal(subtotal);
        compra.setIva(iva);
        compra.setTotal(subtotal.add(iva));

        compra = compraRepository.save(compra);
        clearProductCache();
        log.debug("Compra registrada: {}", compra);
        return compraResponseMapper.toDto(compra);
    }

    // GET BY ID
    @Transactional(readOnly = true)
    @Cacheable(value = "compras", key = "'api_compra_' + #id")
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

        LocalDate d = (desde != null) ? desde.toLocalDate() : null;
        LocalDate h = (hasta != null) ? hasta.toLocalDate() : null;

        if (proveedorId != null && d != null && h != null) {
            page = compraRepository.findByProveedor_IdAndFechaEmisionBetween(proveedorId, d, h, pageable);
        } else if (proveedorId != null) {
            page = compraRepository.findByProveedor_Id(proveedorId, pageable);
        } else if (d != null && h != null) {
            page = compraRepository.findByFechaEmisionBetween(d, h, pageable);
        } else {
            page = compraRepository.findAll(pageable);
        }
        log.debug("Cantidad de compras encontradas: {}", page.getTotalElements());
        return page.map(compraResponseMapper::toDto);
    }

    // UPDATE (revertir stock anterior y aplicar nuevos ítems)
    @Transactional
    @CachePut(value = "compras", key = "'api_compra_' + #result.id", unless = "#result == null")
    public CompraResponseDTO actualizar(Long id, CompraRequestDTO req) {
        log.info("Actualizando compra con Id={}", id);
        Compra compra = compraRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Compra no encontrada: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Compra no encontrada: " + id);
                });

        // devolver stock de detalles actuales (restar lo agregado previamente)
        if (compra.getDetalles() != null) {
            for (CompraDetalle det : compra.getDetalles()) {
                Producto p = productoRepository.findByIdForUpdate(det.getProducto().getId())
                        .orElseThrow();
                p.setStockMinimo(nz(p.getStockMinimo()) - det.getCantidad().doubleValue());
            }
            compra.getDetalles().clear();
        }

        if (req.getProveedorId() != null) {
            Proveedor prov = proveedorRepository.findById(req.getProveedorId())
                    .orElseThrow(() -> {
                        log.error("Proveedor no encontrado: {}", req.getProveedorId());
                        return new ResponseStatusException(HttpStatus.NOT_FOUND,"Proveedor no existe: " + req.getProveedorId());
                    });
            compra.setProveedor(prov);
        }

        if (req.getFechaEmision() != null) {
            compra.setFechaEmision(req.getFechaEmision());
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;

        for (CompraDetalleRequestDTO item : req.getDetalles()) {
            Long productoId = item.getProductoId();
            Producto producto = productoRepository.findByIdForUpdate(productoId)
                    .orElseThrow(() -> {
                        log.error("Producto no encontrado: {}", productoId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND,"Producto no existe: " + productoId);
                    });

            BigDecimal cantidad = toBD(item.getCantidad());
            BigDecimal costoUnit = toBD(item.getCostoUnitario());
            if (cantidad.compareTo(BigDecimal.ZERO) <= 0 || costoUnit.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("Cantidad y costoUnitario deben ser > 0 (producto id=" + productoId + ")");
                throw new IllegalArgumentException("Cantidad y costoUnitario deben ser > 0 (producto id=" + productoId + ")");
            }

            BigDecimal tasaIvaPct = item.getTasaIva() != null ? toBD(item.getTasaIva()) : new BigDecimal("10");

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

            // aumentar stock por nuevas cantidades
            producto.setStockMinimo(nz(producto.getStockMinimo()) + cantidad.doubleValue());

            // (Opcional) actualizar costo
            // producto.setCosto(costoUnit.doubleValue());

            subtotal = subtotal.add(subLinea);
            iva = iva.add(ivaLinea);
        }

        compra.setSubtotal(subtotal);
        compra.setIva(iva);
        compra.setTotal(subtotal.add(iva));

        compra = compraRepository.save(compra);
        clearProductCache();
        log.debug("Compra actualizada correctamente con id: {}", compra.getId());
        return compraResponseMapper.toDto(compra);
    }

    // DELETE (Quita stock previo de la compra)
    @Transactional
    @CacheEvict(value = "compras", key = "'api_compra_' + #id")
    public void eliminar(Long id) {
        log.debug("Eliminando compra con id={}", id);
        // Trae SOLO Compras activas
        Compra compra = compraRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> {
                    log.error("Compra no encontrada o inactiva: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Compra no encontrada: " + id);
                });
        // Quitar de stock
        log.debug("Quitando stock de productos comprados id={}", id);
        if (compra.getDetalles() != null) {
            for (CompraDetalle det : compra.getDetalles()) {
                Producto p = productoRepository.findByIdForUpdate(det.getProducto().getId())
                        .orElseThrow();
                // Usa tu misma lógica de stock
                p.setStockMinimo(nz(p.getStockMinimo()) + det.getCantidad().doubleValue());
                // Si CompraDetalle también tiene 'activo', lo marcamos inactivo:
                try {
                    det.setActivo(false);
                } catch (Exception ignore) { /* si no existe el setter, no pasa nada */ }
            }
        }
        // Soft delete: marcar compra como inactiva
        compra.setActivo(false);
        compraRepository.save(compra);
        clearProductCache();
        log.info("Compra marcada como inactiva id={}", id);
    }

    // helpers
    private BigDecimal toBD(Number n) { return n == null ? BigDecimal.ZERO : new BigDecimal(n.toString()); }
    private double nz(Double d) { return d == null ? 0d : d; }
    private void clearProductCache() {
        var cache = redisCacheManager.getCache("productos");
        if (cache != null) {
            cache.clear();
        }
    }
}
