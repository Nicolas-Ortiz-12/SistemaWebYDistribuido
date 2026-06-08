package com.distri.proyectoVenta.apis.controller;

import com.distri.proyectoVenta.apis.service.ClienteService;
import com.distri.proyectoVenta.dto.ClienteDTO;
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
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService service;

    // -------- LISTAR --------
    @GetMapping({"/{page}/{size}",
    "/{page}/{size}/{q}"})
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<Page<ClienteDTO>> list(
            @PathVariable int page ,
            @PathVariable int size,
            @PathVariable(required = false) String q
    ) {
        log.info("GET /clientes?page={}&size={} q={}", page, size, q);
        Page<ClienteDTO> result = service.list(q, page, size);
        log.debug("Clientes encontrados: {}", result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    // -------- CREAR --------
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<ClienteDTO> create(@Validated @RequestBody ClienteDTO body) {
        log.info("POST /clientes - creando cliente con nombre={}", body.getNombre());
        ClienteDTO created = service.create(body);
        log.info("Cliente creado exitosamente id={} nombre={}", created.getId(), created.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -------- OBTENER --------
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<ClienteDTO> get(@PathVariable Long id) {
        log.info("GET /clientes/{}", id);
        ClienteDTO dto = service.get(id);
        log.debug("Cliente obtenido: {}", dto);
        return ResponseEntity.ok(dto);
    }

    // -------- ACTUALIZAR --------
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<ClienteDTO> update(
            @PathVariable Long id,
            @Validated @RequestBody ClienteDTO body
    ) {
        log.info("PUT /clientes/{} - actualizando con nombre={}", id, body.getNombre());
        ClienteDTO updated = service.update(id, body);
        log.info("Cliente actualizado id={} nombre={}", updated.getId(), updated.getNombre());
        return ResponseEntity.ok(updated);
    }

    // -------- ELIMINAR --------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.warn("DELETE /clientes/{}", id);
        service.delete(id);
        log.info("Cliente eliminado id={}", id);
        return ResponseEntity.noContent().build();
    }
}
