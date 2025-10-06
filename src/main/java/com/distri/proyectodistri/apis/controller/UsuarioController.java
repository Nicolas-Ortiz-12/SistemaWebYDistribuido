package com.distri.proyectodistri.apis.controller;

import com.distri.proyectodistri.apis.service.UsuarioService;
import com.distri.proyectodistri.dto.UsuarioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService service;

    // -------- LISTAR --------
    @GetMapping
    public ResponseEntity<Page<UsuarioDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(service.list(q, page, size));
    }

    // -------- OBTENER --------
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    // -------- CREAR --------
    @PostMapping
    public ResponseEntity<UsuarioDTO> create(@Validated @RequestBody UsuarioDTO body) {
        UsuarioDTO created = service.create(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -------- ACTUALIZAR --------
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> update(
            @PathVariable Long id,
            @Validated @RequestBody UsuarioDTO body
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
