package com.distri.proyectoVenta.apis.service;

import com.distri.proyectoVenta.apis.mapper.ProductoMapper;
import com.distri.proyectoVenta.apis.repository.ProductoRepository;
import com.distri.proyectoVenta.apis.entities.inventario.Producto;
import com.distri.proyectoVenta.dto.ProductoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoService {

    private final ProductoRepository repo;
    private final ProductoMapper mapper;

    @Transactional(readOnly = true)
    public Page<ProductoDTO> list(String q, Long categoriaId, int page, int size) {
        log.info("Listado de producto con filtro='{}', página={}, tamaño={}", q, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Producto> data;

        if (categoriaId != null)
            data = repo.findByCategoriaId(categoriaId, pageable);
        else if (q != null && !q.isBlank())
            data = repo.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(q.trim(), q.trim(), pageable);
        else
            data = repo.findAll(pageable);
        log.debug("Cantidad de productos encontradas: {}", data.getTotalElements());
        return data.map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "sd", key = "'api_producto_' + #id")
    public ProductoDTO get(Long id) {
        log.info("Buscando producto con id {}", id);
        Producto e = repo.findById(id)
                .orElseThrow(() -> {
                    log.error("Producto con id {} no encontrado", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Producto no encontrado");
                });
        log.debug("Producto con id {} encontrado", id);
        return mapper.toDto(e);
    }

    @Transactional
    @CachePut(value = "sd", key = "'api_producto_' + #result.id", unless = "#result == null")
    public ProductoDTO create(ProductoDTO dto) {
        log.info("Creando producto con id {}", dto.getId());
        if (repo.existsByCodigoIgnoreCase(dto.getCodigo())) {
            log.error("Producto con id {} no encontrado", dto.getCodigo());
            throw new IllegalStateException("Ya existe un producto con ese código");
        }
        Producto e = mapper.toEntity(dto);
        log.debug("Producto con creado {}", e.getCodigo());
        return mapper.toDto(repo.save(e));
    }

    @Transactional
    @CachePut(value = "sd", key = "'api_producto_' + #result.id", unless = "#result == null")
    public ProductoDTO update(Long id, ProductoDTO dto) {
        log.info("Actualizando producto con id {}", id);
        Producto e = repo.findById(id)
                .orElseThrow(() -> {
                    log.error("Producto con id {} no encontrado", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Producto no encontrado");
                });
        mapper.updateEntityFromDto(dto, e);
        log.debug("Producto creado correctamente con id {} ", id);
        return mapper.toDto(repo.save(e));
    }

    @Transactional
    @CacheEvict(value = "sd", key = "'api_venta_' + #id")
    public void delete(Long id) {
        log.info("Eliminando producto con id {}", id);
        Producto e = repo.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrada"));
        e.setActivo(false);
        repo.save(e);
        log.info("Producto con id {} eliminado", id);

    }
}