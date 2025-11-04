package com.distri.proyectoVenta.apis.controller;

import com.distri.proyectoVenta.apis.service.ProductoService;
import com.distri.proyectoVenta.dto.ProductoDTO;
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
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService service;

    // -------- LISTAR --------
    @GetMapping({"/{page}/{size}",
            "/{page}/{size}/{q}"})
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<Page<ProductoDTO>> list(
            @PathVariable int page,
            @PathVariable int size,
            @PathVariable(required = false) Long categoriaId,
            @PathVariable(required = false) String q
    ) {
        log.info("GET /productos?page={}&size={} q={} categoriaId={}", page, size, q, categoriaId);
        Page<ProductoDTO> result = service.list(q, categoriaId, page, size);
        log.debug("Productos encontrados: {}", result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    // -------- CREAR --------
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<ProductoDTO> create(@Validated @RequestBody ProductoDTO dto) {
        log.info("POST /productos - creando producto: nombre={} código={}", dto.getNombre(), dto.getCodigo());
        ProductoDTO created = service.create(dto);
        log.info("Producto creado id={} nombre={}", created.getId(), created.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -------- OBTENER --------
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<ProductoDTO> get(@PathVariable Long id) {
        log.info("GET /productos/{}", id);
        ProductoDTO dto = service.get(id);
        log.debug("Producto obtenido: {}", dto);
        return ResponseEntity.ok(dto);
    }

    // -------- ACTUALIZAR --------
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<ProductoDTO> update(@PathVariable Long id, @Validated @RequestBody ProductoDTO dto) {
        log.info("PUT /productos/{} - actualizando producto nombre={}", id, dto.getNombre());
        ProductoDTO updated = service.update(id, dto);
        log.info("Producto actualizado id={} nombre={}", updated.getId(), updated.getNombre());
        return ResponseEntity.ok(updated);
    }

    // -------- ELIMINAR --------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.warn("DELETE /productos/{}", id);
        service.delete(id);
        log.info("Producto eliminado id={}", id);
        return ResponseEntity.noContent().build();
    }
}
