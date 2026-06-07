// src/main/java/com/distri/proyectodistri/apis/service/ProveedorService.java
package com.distri.proyectoVenta.apis.service;

import com.distri.proyectoVenta.apis.mapper.ProveedorMapper;
import com.distri.proyectoVenta.apis.repository.ProveedorRepository;
import com.distri.proyectoVenta.apis.entities.cliente.Proveedor;
import com.distri.proyectoVenta.dto.ProveedorDTO;
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
public class ProveedorService {

    private final ProveedorRepository repo;
    private final ProveedorMapper mapper;

    // LISTAR (paginado + búsqueda por nombre/RUC)
    @Transactional(readOnly = true)
    public Page<ProveedorDTO> list(String q, int page, int size) {
        log.info("Listado de Proveedores con filtro='{}', página={}, tamaño={}", q, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Proveedor> data = (q == null || q.isBlank())
                ? repo.findAll(pageable)
                : repo.findByNombreContainingIgnoreCaseOrRucContainingIgnoreCase(q.trim(), q.trim(), pageable);
        log.debug("Cantidad de proveedores encontradas: {}", data.getTotalElements());
        return data.map(mapper::toDto);
    }

    // OBTENER POR ID
    @Transactional(readOnly = true)
    @Cacheable(value = "sd", key = "'api_proveedor_' + #id")
    public ProveedorDTO get(Long id) {
        log.info("Buscando Proveedor con ID: {}", id);
        Proveedor e = repo.findById(id)
                .orElseThrow(() -> {
                    log.error("Proveedor no encontrado: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Proveedor no encontrado");
                });
        log.debug("Proveedor encontrado con ID: {} y nombre {}", e.getId(), e.getNombre());
        return mapper.toDto(e);
    }

    // CREAR
    @Transactional
    @CachePut(value = "sd", key = "'api_proveedor_' + #result.id", unless = "#result == null")
    public ProveedorDTO create(ProveedorDTO dto) {
        log.info("Creando Proveedor con ID: {}", dto.getId());
        if (dto.getRuc() != null && !dto.getRuc().isBlank() && repo.existsByRuc(dto.getRuc().trim())) {
            log.error("Proveedor con Ruc existente: {}", dto.getRuc());
            throw new IllegalStateException("Ya existe un proveedor con el RUC: " + dto.getRuc());
        }
        Proveedor e = mapper.toEntity(dto);
        Proveedor saved = repo.save(e);
        log.debug("Proveedor creado con ID: {}", saved.getId());
        return mapper.toDto(saved);
    }

    // ACTUALIZAR
    @Transactional
    @CachePut(value = "sd", key = "'api_proveedor_' + #result.id", unless = "#result == null")
    public ProveedorDTO update(Long id, ProveedorDTO dto) {
        log.info("Actualizando Proveedor con ID: {}", id);
        Proveedor e = repo.findById(id)
                .orElseThrow(() -> {
                    log.error("Proveedor no encontrado: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Proveedor no encontrado: id=" + id);
                });

        if (dto.getRuc() != null && !dto.getRuc().isBlank()) {
            String newRuc = dto.getRuc().trim();
            if (!newRuc.equals(e.getRuc()) && repo.existsByRuc(newRuc)) {
                log.error("Proveedor con Ruc existente: {}", newRuc);
                throw new IllegalStateException("Ya existe un proveedor con el RUC: " + newRuc);
            }
        }
        mapper.updateEntityFromDto(dto, e);

        Proveedor saved = repo.save(e);
        return mapper.toDto(saved);
    }

    // ELIMINAR
    @Transactional
    @CacheEvict(value = "sd", key = "'api_proveedor_' + #id")
    public void delete(Long id) {
        log.info("Eliminando Proveedor con ID: {}", id);
        Proveedor e = repo.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor no encontrada"));
        e.setActivo(false);
        repo.save(e);
        log.info("Eliminado Proveedor con ID: {}", id);

    }
}