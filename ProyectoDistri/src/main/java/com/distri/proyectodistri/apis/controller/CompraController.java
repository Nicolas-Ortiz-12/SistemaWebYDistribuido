// src/main/java/com/distri/proyectodistri/apis/controller/compra/CompraController.java
package com.distri.proyectodistri.apis.controller;

import com.distri.proyectodistri.apis.service.CompraService;
import com.distri.proyectodistri.dto.CompraRequestDTO;
import com.distri.proyectodistri.dto.CompraResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@Slf4j
@RestController
@RequestMapping("/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public CompraResponseDTO crear(@RequestBody CompraRequestDTO req) {
        log.info("POST /compras proveedorId={}", req.getProveedorId());
        return compraService.crear(req);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public CompraResponseDTO obtener(@PathVariable Long id) {
        log.info("GET /compras/{}", id);
        return compraService.obtenerPorId(id);
    }

    @GetMapping({
            "/{page}/{size}",
            "/{page}/{size}/{proveedorId}",
            "/{page}/{size}/{proveedorId}/{desde}",
            "/{page}/{size}/{proveedorId}/{desde}/{hasta}"
    })
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public Page<CompraResponseDTO> listar(
            @PathVariable int page,
            @PathVariable int size,
            @PathVariable(required = false) Long proveedorId,
            @PathVariable(required = false) String desde,
            @PathVariable(required = false) String hasta
    ) {
        Pageable pageable = PageRequest.of(page, size);
        OffsetDateTime desdeOD = null;
        OffsetDateTime hastaOD = null;
        try {
            if (desde != null && !desde.isBlank()) {
                desdeOD = OffsetDateTime.parse(desde);
            }
            if (hasta != null && !hasta.isBlank()) {
                hastaOD = OffsetDateTime.parse(hasta);
            }
        } catch (Exception e) {
            log.warn("Error parseando fechas: {} - {}", desde, hasta);
        }
        log.info("GET /compras page={} size={} filtros: proveedorId={}, desde={}, hasta={}",
                page, size, proveedorId, desdeOD, hastaOD);
        return compraService.listar(pageable, proveedorId, desdeOD, hastaOD);
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public CompraResponseDTO actualizar(@PathVariable Long id, @RequestBody CompraRequestDTO req) {
        log.info("PUT /compras/{}", id);
        return compraService.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        log.info("DELETE /compras/{}", id);
        compraService.eliminar(id);
    }
}
