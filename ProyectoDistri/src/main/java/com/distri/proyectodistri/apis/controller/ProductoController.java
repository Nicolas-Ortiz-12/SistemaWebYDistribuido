package com.distri.proyectodistri.apis.controller;

import com.distri.proyectodistri.apis.service.ProductoService;
import com.distri.proyectodistri.dto.ProductoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // 👈 Importa Lombok logger
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j // 👈 Activa el logger "log"
@RestController
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService service;

    // -------- LISTAR --------
    @GetMapping({"/{page}/{size}",
            "/{page}/{size}/{q}"})
    public ResponseEntity<Page<ProductoDTO>> list(
            @PathVariable int page,
            @PathVariable int size,
            @PathVariable(required = false) String q
    ) {
        log.info("GET /productos?page={}&size={} q={}", page, size, q);
        Page<ProductoDTO> result = service.list(q, page, size);
        log.debug("Productos encontrados: {}", result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    // -------- CREAR --------
    @PostMapping
    public ResponseEntity<ProductoDTO> create(@Validated @RequestBody ProductoDTO dto) {
        log.info("POST /productos - creando producto: nombre={} código={}", dto.getNombre(), dto.getCodigo());
        ProductoDTO created = service.create(dto);
        log.info("Producto creado id={} nombre={}", created.getId(), created.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -------- OBTENER --------
    @GetMapping("/{id}")
    public ResponseEntity<ProductoDTO> get(@PathVariable Long id) {
        log.info("GET /productos/{}", id);
        ProductoDTO dto = service.get(id);
        log.debug("Producto obtenido: {}", dto);
        return ResponseEntity.ok(dto);
    }

    // -------- ACTUALIZAR --------
    @PutMapping("/{id}")
    public ResponseEntity<ProductoDTO> update(@PathVariable Long id, @Validated @RequestBody ProductoDTO dto) {
        log.info("PUT /productos/{} - actualizando producto nombre={}", id, dto.getNombre());
        ProductoDTO updated = service.update(id, dto);
        log.info("Producto actualizado id={} nombre={}", updated.getId(), updated.getNombre());
        return ResponseEntity.ok(updated);
    }

    // -------- ELIMINAR --------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.warn("DELETE /productos/{}", id);
        service.delete(id);
        log.info("Producto eliminado id={}", id);
        return ResponseEntity.noContent().build();
    }
}
