// src/main/java/com/distri/proyectodistri/apis/controller/venta/VentaController.java
package com.distri.proyectoVenta.apis.controller;

import com.distri.proyectoVenta.apis.service.VentaService;
import com.distri.proyectoVenta.dto.VentaRequestDTO;
import com.distri.proyectoVenta.dto.VentaResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import com.distri.proyectoVenta.apis.service.Paginas;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

@Slf4j
@RestController
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

    // Vista previa del número que va a tener la próxima venta (se muestra en la pantalla de nueva venta)
    @GetMapping("/proximo-numero")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public Map<String, String> proximoNumero() {
        return Map.of("numero", ventaService.proximoNumero());
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
            @PathVariable(name = "clienteId", required = false) Long clienteIdPath,
            @PathVariable(name = "desde", required = false) String desdePath,
            @PathVariable(name = "hasta", required = false) String hastaPath,
            // Los filtros también van por query (?desde=...): en la ruta, filtrar por fecha obliga a pasar un cliente
            @RequestParam(name = "clienteId", required = false) Long clienteIdQuery,
            @RequestParam(name = "desde", required = false) String desdeQuery,
            @RequestParam(name = "hasta", required = false) String hastaQuery
    ) {
        Pageable pageable = Paginas.of(page, size);

        Long clienteId = clienteIdPath != null ? clienteIdPath : clienteIdQuery;
        OffsetDateTime desdeOD = parseFecha(desdePath != null ? desdePath : desdeQuery, false);
        OffsetDateTime hastaOD = parseFecha(hastaPath != null ? hastaPath : hastaQuery, true);

        log.info("GET /ventas page={} size={} filtros: clienteId={}, desde={}, hasta={}",
                pageable.getPageNumber(), pageable.getPageSize(), clienteId, desdeOD, hastaOD);

        return ventaService.listar(pageable, clienteId, desdeOD, hastaOD);
    }


    // Acepta fecha-hora ISO con offset ("2026-10-01T00:00:00-03:00") o solo fecha ("2026-10-01",
    // lo que manda el <input type="date"> del front): "desde" es el inicio del día y "hasta" el final.
    static OffsetDateTime parseFecha(String valor, boolean finDelDia) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return OffsetDateTime.parse(valor);
        } catch (DateTimeParseException e) {
            try {
                LocalDate d = LocalDate.parse(valor);
                return OffsetDateTime.of(d, finDelDia ? LocalTime.MAX : LocalTime.MIN, ZoneOffset.UTC);
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Fecha inválida: " + valor + " (usar yyyy-MM-dd o ISO-8601 con offset)");
            }
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public VentaResponseDTO actualizar(@PathVariable Long id, @RequestBody VentaRequestDTO req) {
        log.info("PUT /ventas/{}", id);
        return ventaService.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        log.info("DELETE /ventas/{}", id);
        ventaService.eliminar(id);
    }
}
