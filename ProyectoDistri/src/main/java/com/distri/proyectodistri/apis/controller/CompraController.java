// src/main/java/com/distri/proyectodistri/apis/controller/compra/CompraController.java
package com.distri.proyectodistri.apis.controller;

import com.distri.proyectodistri.apis.service.CompraService;
import com.distri.proyectodistri.apis.service.Paginas;
import com.distri.proyectodistri.dto.CompraRequestDTO;
import com.distri.proyectodistri.dto.CompraResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

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
            @PathVariable(name = "proveedorId", required = false) Long proveedorIdPath,
            @PathVariable(name = "desde", required = false) String desdePath,
            @PathVariable(name = "hasta", required = false) String hastaPath,
            // Los filtros también van por query (?desde=...): en la ruta, filtrar por fecha obliga a pasar un proveedor
            @RequestParam(name = "proveedorId", required = false) Long proveedorIdQuery,
            @RequestParam(name = "desde", required = false) String desdeQuery,
            @RequestParam(name = "hasta", required = false) String hastaQuery
    ) {
        Pageable pageable = Paginas.of(page, size);
        Long proveedorId = proveedorIdPath != null ? proveedorIdPath : proveedorIdQuery;
        OffsetDateTime desdeOD = parseFecha(desdePath != null ? desdePath : desdeQuery);
        OffsetDateTime hastaOD = parseFecha(hastaPath != null ? hastaPath : hastaQuery);
        log.info("GET /compras page={} size={} filtros: proveedorId={}, desde={}, hasta={}",
                page, size, proveedorId, desdeOD, hastaOD);
        return compraService.listar(pageable, proveedorId, desdeOD, hastaOD);
    }


    // Acepta fecha-hora ISO con offset ("2026-10-01T00:00:00-03:00") o solo fecha ("2026-10-01",
    // lo que manda el <input type="date"> del front). La compra filtra por fecha de emisión (LocalDate).
    static OffsetDateTime parseFecha(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return OffsetDateTime.parse(valor);
        } catch (DateTimeParseException e) {
            try {
                return OffsetDateTime.of(LocalDate.parse(valor), LocalTime.MIN, ZoneOffset.UTC);
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Fecha inválida: " + valor + " (usar yyyy-MM-dd o ISO-8601 con offset)");
            }
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public CompraResponseDTO actualizar(@PathVariable Long id, @RequestBody CompraRequestDTO req) {
        log.info("PUT /compras/{}", id);
        return compraService.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        log.info("DELETE /compras/{}", id);
        compraService.eliminar(id);
    }
}
