package com.distri.proyectoVenta.apis.controller;

import com.distri.proyectoVenta.apis.service.DeudorService;
import com.distri.proyectoVenta.dto.DeudorDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/deudores")
@RequiredArgsConstructor
public class DeudorController {

    private final DeudorService service;

    @GetMapping({"/{page}/{size}", "/{page}/{size}/{q}"})
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<Page<DeudorDTO>> list(
            @PathVariable int page,
            @PathVariable int size,
            @PathVariable(required = false) String q
    ) {
        return ResponseEntity.ok(service.list(q, page, size));
    }

    @GetMapping("/con-deuda/{page}/{size}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<Page<DeudorDTO>> listConDeuda(
            @PathVariable int page,
            @PathVariable int size
    ) {
        return ResponseEntity.ok(service.listConDeuda(page, size));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<DeudorDTO> create(@Validated @RequestBody DeudorDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PutMapping({"/{id}/saldar", "/{id}/clear-debt"})
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<Void> saldar(@PathVariable Long id) {
        service.saldar(id);
        return ResponseEntity.ok().build();
    }
}
