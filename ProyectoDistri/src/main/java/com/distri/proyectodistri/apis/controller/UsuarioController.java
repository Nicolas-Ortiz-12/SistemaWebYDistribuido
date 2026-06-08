package com.distri.proyectodistri.apis.controller;

import com.distri.proyectodistri.apis.service.UsuarioService;
import com.distri.proyectodistri.dto.UsuarioCreateDTO;
import com.distri.proyectodistri.dto.UsuarioDTO;
import jakarta.validation.Valid;
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
    @GetMapping({"/{page}/{size}",
            "/{page}/{size}/{q}"})
    public ResponseEntity<Page<UsuarioDTO>> list(
            @PathVariable int page ,
            @PathVariable int size,
            @PathVariable(required = false) String q
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
    public ResponseEntity<UsuarioDTO> create(@Valid @RequestBody UsuarioCreateDTO in) {
        var dto = service.create(in);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
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
