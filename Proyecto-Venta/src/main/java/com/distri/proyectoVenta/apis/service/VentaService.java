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
import com.distri.proyectoVenta.apis.repository.VentaRepository;
import com.distri.proyectoVenta.dto.*;
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
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Slf4j
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
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
        if (req.getItems() == null || req.getItems().isEmpty()) {
            log.error("La venta no posee ítems");
            throw new IllegalArgumentException("La venta debe tener al menos un ítem");
        }

        Venta venta = ventaRequestMapper.toEntity(req);

        if (req.getClienteId() != null) {
            Cliente cliente = clienteRepository.findById(req.getClienteId())
                    .orElseThrow(() -> {
                        log.error("Cliente no encontrado");
                        return new ResponseStatusException(HttpStatus.NOT_FOUND,"Cliente no existe: " + req.getClienteId());
                    });
            venta.setCliente(cliente);
        }

        if (req.getFechaVenta() != null) {
            LocalDateTime fv = OffsetDateTime.parse(req.getFechaVenta().toString()).toLocalDateTime();
            venta.setFechaVenta(fv);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;

        for (VentaDetalleRequestDTO item : req.getItems()) {
            Long productoId = item.getProductoId();
            if (productoId == null) {
                log.error("Falta productoId en ítem");
                throw new IllegalArgumentException("Falta productoId en ítem");
            }

            Producto producto = productoRepository.findByIdForUpdate(productoId)
                    .orElseThrow(() ->{
                        log.error("Producto no encontrado: {}", productoId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND,"Producto no existe: " + productoId);
                    });

            BigDecimal cantidad = toBD(item.getCantidad());
            if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("Cantidad debe ser mayor que cero para producto id={}", productoId);
                throw new IllegalArgumentException("Cantidad debe ser > 0 para producto id=" + productoId);
            }

            double stock = nz(producto.getStockMinimo());
            double cantD = cantidad.doubleValue();
            if (Double.compare(stock, cantD) < 0) {
                log.error("Stock insuficiente para producto id={} (stock actual={})", productoId, stock);
                throw new IllegalStateException("Stock insuficiente para producto id=" + productoId);
            }

            BigDecimal precioUnit = toBD(producto.getPrecio()); // precio real
            BigDecimal tasaIvaPct = item.getTasaIva() != null ? toBD(item.getTasaIva()) : new BigDecimal("10");

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

            producto.setStockMinimo(stock - cantD);

            subtotal = subtotal.add(subLinea);
            iva = iva.add(ivaLinea);
            log.debug("Detalle agregado productoId={} cantidad={} subtotal={}", productoId, cantidad, subLinea);
        }

        venta.setSubtotal(subtotal);
        venta.setIva(iva);
        venta.setTotal(subtotal.add(iva));

        venta = ventaRepository.save(venta);
        log.debug("Venta registrada exitosamente con id={}", venta.getId());
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
        LocalDateTime d = desde != null ? desde.toLocalDateTime() : null;
        LocalDateTime h = hasta != null ? hasta.toLocalDateTime() : null;

        if (clienteId != null && d != null && h != null) {
            page = ventaRepository.findByCliente_IdAndFechaVentaBetween(clienteId, d, h, pageable);
        } else if (clienteId != null) {
            page = ventaRepository.findByCliente_Id(clienteId, pageable);
        } else if (d != null && h != null) {
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
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Venta no encontrada: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Venta no encontrada: " + id);
                });

        // devolver stock de los detalles actuales
        if (venta.getDetalles() != null) {
            for (VentaDetalle det : venta.getDetalles()) {
                Producto p = productoRepository.findByIdForUpdate(det.getProducto().getId())
                        .orElseThrow();
                p.setStockMinimo(nz(p.getStockMinimo()) + det.getCantidad().doubleValue());
            }
            venta.getDetalles().clear();
        }
        log.debug("Devolviendo stock de venta previa id={}", id);

        // actualizar cabecera (cliente / fecha)
        if (req.getClienteId() != null) {
            Cliente c = clienteRepository.findById(req.getClienteId())
                    .orElseThrow(() -> {
                        log.error("Cliente no encontrado: {}", req.getClienteId());
                        return new ResponseStatusException(HttpStatus.NOT_FOUND,"Cliente no existe: " + req.getClienteId());
                    });
            venta.setCliente(c);
        }
        if (req.getFechaVenta() != null) {
            log.debug("Actualizando cliente de venta id={}", id);
            venta.setFechaVenta(req.getFechaVenta().atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
        }

        // aplicar nuevos items
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;

        for (VentaDetalleRequestDTO item : req.getItems()) {
            Long productoId = item.getProductoId();
            Producto producto = productoRepository.findByIdForUpdate(productoId)
                    .orElseThrow(() -> {
                        log.error("Producto no encontrado: {}", productoId);
                        return new ResponseStatusException(HttpStatus.NOT_FOUND,"Producto no existe: " + productoId);
                    });

            BigDecimal cantidad = toBD(item.getCantidad());
            double stock = nz(producto.getStockMinimo());
            double cantD = cantidad.doubleValue();
            if (Double.compare(stock, cantD) < 0) {
                log.error("Stock insuficiente para producto id={} (stock actual={})", productoId, stock);
                throw new IllegalStateException("Stock insuficiente para producto id=" + productoId);
            }

            BigDecimal precioUnit = toBD(producto.getPrecio());
            BigDecimal tasaIvaPct = item.getTasaIva() != null ? toBD(item.getTasaIva()) : new BigDecimal("10");

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
            producto.setStockMinimo(stock - cantD);

            subtotal = subtotal.add(subLinea);
            iva = iva.add(ivaLinea);
            log.debug("Detalle actualizado productoId={} cantidad={} subtotal={}", productoId, cantidad, subLinea);
        }

        venta.setSubtotal(subtotal);
        venta.setIva(iva);
        venta.setTotal(subtotal.add(iva));

        venta = ventaRepository.save(venta);
        log.debug("Venta actualizada correctamente id={}", venta.getId());
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
        if (venta.getDetalles() != null) {
            for (VentaDetalle det : venta.getDetalles()) {
                Producto p = productoRepository.findByIdForUpdate(det.getProducto().getId())
                        .orElseThrow();
                // Usa tu misma lógica de stock
                p.setStockMinimo(nz(p.getStockMinimo()) + det.getCantidad().doubleValue());
                // Si VentaDetalle también tiene 'activo', lo marcamos inactivo:
                try {
                    det.setActivo(false);
                } catch (Exception ignore) { /* si no existe el setter, no pasa nada */ }
            }
        }
        // Soft delete: marcar venta como inactiva
        venta.setActivo(false);
        // Persistir cambios (venta + detalles + productos)
        ventaRepository.save(venta);
        log.info("Venta marcada como inactiva id={}", id);
    }

    // helpers
    private BigDecimal toBD(Number n) { return n == null ? BigDecimal.ZERO : new BigDecimal(n.toString()); }
    private double nz(Double d) { return d == null ? 0d : d; }
}