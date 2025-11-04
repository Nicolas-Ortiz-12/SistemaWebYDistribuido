package com.distri.proyectoVenta.apis.controller;

import com.distri.proyectoVenta.apis.service.RolService;
import com.distri.proyectoVenta.dto.RolDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j // 👈 Activa el logger SLF4J
@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService service;

    // -------- LISTAR --------
    @GetMapping({"/{page}/{size}",
            "/{page}/{size}/{q}"})
    public ResponseEntity<Page<RolDTO>> list(
            @PathVariable int page ,
            @PathVariable int size,
            @PathVariable(required = false) String q
    ) {
        log.info("GET /roles?page={}&size={}&q={}", page, size, q);
        Page<RolDTO> result = service.list(q, page, size);
        log.debug("Roles encontrados: {}", result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    // -------- OBTENER --------
    @GetMapping("/{id}")
    public ResponseEntity<RolDTO> get(@PathVariable Long id) {
        log.info("GET /roles/{}", id);
        RolDTO dto = service.get(id);
        log.debug("Rol obtenido: {}", dto);
        return ResponseEntity.ok(dto);
    }

    // -------- CREAR --------
    @PostMapping
    public ResponseEntity<RolDTO> create(@Valid @RequestBody RolDTO body) {
        log.info("POST /roles - creando rol nombre={}", body.getNombre());
        RolDTO created = service.create(body);
        log.info("Rol creado id={} nombre={}", created.getId(), created.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -------- ACTUALIZAR --------
    @PutMapping("/{id}")
    public ResponseEntity<RolDTO> update(
            @PathVariable Long id,
            @Validated @RequestBody RolDTO body
    ) {
        log.info("PUT /roles/{} - actualizando rol nombre={}", id, body.getNombre());
        RolDTO updated = service.update(id, body);
        log.info("Rol actualizado id={} nombre={}", updated.getId(), updated.getNombre());
        return ResponseEntity.ok(updated);
    }

    // -------- ELIMINAR --------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.warn("DELETE /roles/{}", id);
        service.delete(id);
        log.info("Rol eliminado id={}", id);
        return ResponseEntity.noContent().build();
    }
}
