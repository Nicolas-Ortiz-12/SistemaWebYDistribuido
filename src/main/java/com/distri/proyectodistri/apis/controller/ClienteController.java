// src/main/java/com/distri/proyectodistri/api/ClienteController.java
package com.distri.proyectodistri.apis.controller;

import com.distri.proyectodistri.apis.service.ClienteService;
import com.distri.proyectodistri.dto.ClienteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService service;

    // -------- LISTAR --------
    @GetMapping
    public ResponseEntity<Page<ClienteDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(service.list(q, page, size));
    }

    // -------- CREAR --------
    @PostMapping
    public ResponseEntity<ClienteDTO> create(@Validated @RequestBody ClienteDTO body) {
        ClienteDTO created = service.create(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -------- OBTENER --------
    @GetMapping("/{id}")
    public ResponseEntity<ClienteDTO> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    // -------- ACTUALIZAR --------
    @PutMapping("/{id}")
    public ResponseEntity<ClienteDTO> update(
            @PathVariable Long id,
            @Validated @RequestBody ClienteDTO body
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
