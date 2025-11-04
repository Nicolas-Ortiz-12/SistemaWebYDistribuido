package com.distri.proyectodistri.apis.controller;

import com.distri.proyectodistri.apis.service.CategoriaService;
import com.distri.proyectodistri.dto.CategoriaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService service;

    @GetMapping
    public ResponseEntity<Page<CategoriaDTO>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q
    ) {
        log.info("GET /categorias?page={}&size={}&q={}", page, size, q);
        return ResponseEntity.ok(service.list(q, page, size));
    }

    @GetMapping("/{page}/{size}")
    public ResponseEntity<Page<CategoriaDTO>> listPath(
            @PathVariable int page,
            @PathVariable int size,
            @RequestParam(required = false) String q
    ) {
        log.info("GET /categorias/{}/{}?q={}", page, size, q);
        return ResponseEntity.ok(service.list(q, page, size));
    }


    @PostMapping
    public ResponseEntity<CategoriaDTO> create(@Validated @RequestBody CategoriaDTO dto) {
        log.info("POST /categorias - creando categoría: {}", dto.getNombre());
        var res = service.create(dto);
        log.info("Categoría creada id={} nombre={}", res.getId(), res.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaDTO> get(@PathVariable Long id) {
        log.info("GET /categorias/{}", id);
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaDTO> update(@PathVariable Long id, @Validated @RequestBody CategoriaDTO dto) {
        log.info("PUT /categorias/{} - actualizando con nombre={}", id, dto.getNombre());
        var res = service.update(id, dto);
        log.info("Categoría actualizada id={} nombre={}", res.getId(), res.getNombre());
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.warn("DELETE /categorias/{}", id);
        service.delete(id);
        log.info("Categoría eliminada id={}", id);
        return ResponseEntity.noContent().build();
    }
}
