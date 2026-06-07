// src/main/java/com/distri/proyectodistri/apis/controller/venta/VentaController.java
package com.distri.proyectoVenta.apis.controller;

import com.distri.proyectoVenta.apis.service.VentaService;
import com.distri.proyectoVenta.dto.VentaRequestDTO;
import com.distri.proyectoVenta.dto.VentaResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.OffsetDateTime;

@Slf4j
@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public VentaResponseDTO crear(@RequestBody VentaRequestDTO req) {
        log.info("POST /ventas clienteId={}", req.getClienteId());
        return ventaService.crear(req);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public VentaResponseDTO obtener(@PathVariable Long id) {
        log.info("GET /ventas/{}", id);
        return ventaService.obtenerPorId(id);
    }

    @GetMapping({
            "/{page}/{size}",
            "/{page}/{size}/{clienteId}",
            "/{page}/{size}/{clienteId}/{desde}",
            "/{page}/{size}/{clienteId}/{desde}/{hasta}"
    })
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public Page<VentaResponseDTO> listar(
            @PathVariable int page,
            @PathVariable int size,
            @PathVariable(required = false) Long clienteId,
            @PathVariable(required = false) String desde,
            @PathVariable(required = false) String hasta
    ) {
        Pageable pageable = PageRequest.of(page, size);

        OffsetDateTime desdeOD = null;
        OffsetDateTime hastaOD = null;
        try {
            if (desde != null && !desde.isBlank()) desdeOD = OffsetDateTime.parse(desde);
            if (hasta != null && !hasta.isBlank()) hastaOD = OffsetDateTime.parse(hasta);
        } catch (Exception e) {
            log.warn("Error parseando fechas: {} - {}", desde, hasta);
        }

        log.info("GET /ventas page={} size={} filtros: clienteId={}, desde={}, hasta={}",
                pageable.getPageNumber(), pageable.getPageSize(), clienteId, desdeOD, hastaOD);

        return ventaService.listar(pageable, clienteId, desdeOD, hastaOD);
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public VentaResponseDTO actualizar(@PathVariable Long id, @RequestBody VentaRequestDTO req) {
        log.info("PUT /ventas/{}", id);
        return ventaService.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        log.info("DELETE /ventas/{}", id);
        ventaService.eliminar(id);
    }
}
