package com.distri.proyectoVenta.apis.controller;

import com.distri.proyectoVenta.apis.service.ProveedorService;
import com.distri.proyectoVenta.dto.ProveedorDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j // 👈 Activa el logger "log"
@RestController
@RequestMapping("/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService service;

    // -------- LISTAR --------
    @GetMapping({"/{page}/{size}",
            "/{page}/{size}/{q}"})
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<Page<ProveedorDTO>> list(
            @PathVariable int page ,
            @PathVariable int size,
            @PathVariable(required = false) String q
    ) {
        log.info("GET /proveedores?page={}&size={}&q={}", page, size, q);
        Page<ProveedorDTO> result = service.list(q, page, size);
        log.debug("Proveedores encontrados: {}", result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    // -------- CREAR --------
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ProveedorDTO> create(@Validated @RequestBody ProveedorDTO body) {
        log.info("POST /proveedores - creando proveedor con nombre={} ruc={}", body.getNombre(), body.getRuc());
        ProveedorDTO created = service.create(body);
        log.info("Proveedor creado id={} nombre={}", created.getId(), created.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -------- OBTENER --------
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<ProveedorDTO> get(@PathVariable Long id) {
        log.info("GET /proveedores/{}", id);
        ProveedorDTO dto = service.get(id);
        log.debug("Proveedor obtenido: {}", dto);
        return ResponseEntity.ok(dto);
    }

    // -------- ACTUALIZAR --------
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ProveedorDTO> update(@PathVariable Long id, @Validated @RequestBody ProveedorDTO body) {
        log.info("PUT /proveedores/{} - actualizando proveedor nombre={}", id, body.getNombre());
        ProveedorDTO updated = service.update(id, body);
        log.info("Proveedor actualizado id={} nombre={}", updated.getId(), updated.getNombre());
        return ResponseEntity.ok(updated);
    }

    // -------- ELIMINAR --------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.warn("DELETE /proveedores/{}", id);
        service.delete(id);
        log.info("Proveedor eliminado id={}", id);
        return ResponseEntity.noContent().build();
    }
}
