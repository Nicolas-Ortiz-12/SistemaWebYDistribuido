package com.distri.proyectodistri.apis.controller;

import com.distri.proyectodistri.apis.service.RolService;
import com.distri.proyectodistri.dto.RolDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService service;

    // -------- LISTAR --------
    @GetMapping
    public ResponseEntity<Page<RolDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(service.list(q, page, size));
    }

    // -------- OBTENER --------
    @GetMapping("/{id}")
    public ResponseEntity<RolDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    // -------- CREAR --------
    @PostMapping
    public ResponseEntity<RolDTO> create(@Validated @RequestBody RolDTO body) {
        RolDTO created = service.create(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -------- ACTUALIZAR --------
    @PutMapping("/{id}")
    public ResponseEntity<RolDTO> update(
            @PathVariable Long id,
            @Validated @RequestBody RolDTO body
    ) {
        return ResponseEntity.ok(service.update(id, body));
    }

    // -------- ELIMINAR --------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
