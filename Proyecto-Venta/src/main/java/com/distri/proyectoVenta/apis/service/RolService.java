package com.distri.proyectoVenta.apis.service;

import com.distri.proyectoVenta.apis.entities.seguridad.Rol;
import com.distri.proyectoVenta.apis.mapper.RolMapper;
import com.distri.proyectoVenta.apis.repository.RolRepository;
import com.distri.proyectoVenta.dto.RolDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RolService {

    private final RolRepository repo;
    private final RolMapper mapper;

    // -------- LISTAR (paginado + búsqueda) --------
    @Transactional(readOnly = true)
    public Page<RolDTO> list(String q, int page, int size) {
        log.info("Listado de Roles con filtro='{}', página={}, tamaño={}", q, page, size);
        var pageable = PageRequest.of(page, size, Sort.by("id").descending());
        var pageEntities = (q == null || q.isBlank())
                ? repo.findAll(pageable)
                : repo.findByNombreContainingIgnoreCase(q.trim(), pageable);
        log.debug("Cantidad de Roles encontrados: {}", pageEntities.getTotalElements());
        return pageEntities.map(mapper::toDto);
    }

    // -------- OBTENER POR ID --------
    @Transactional(readOnly = true)
    public RolDTO get(Long id) {
        log.info("Buscando Rol con ID: {}", id);
        Rol e = repo.findById(id)
                .orElseThrow(() -> {
                    log.error("Rol no encontrado: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Rol no encontrado");
                });
        RolDTO dto = mapper.toDto(e);
        log.debug("Buscando Rol con ID: {}", dto.getId());
        return dto;
    }

    // -------- CREAR --------
    @Transactional
    public RolDTO create(RolDTO dto) {
        log.info("Creando Rol con ID: {}", dto.getId());
        Rol e = mapper.toEntity(dto);
        Rol saved = repo.save(e);
        log.debug("Rol creado con ID: {}", saved.getId());
        return mapper.toDto(saved);
    }

    // -------- ACTUALIZAR --------
    @Transactional
    public RolDTO update(Long id, RolDTO dto) {
        log.info("Actualizando Rol con ID: {}", id);
        Rol e = repo.findById(id)
                .orElseThrow(() -> {
                    log.error("Rol no encontrado: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Rol no encontrado: id=" + id);
                });

        if (dto.getNombre() != null && !dto.getNombre().isBlank()) {
            String newName = dto.getNombre().trim();
            if (!newName.equals(e.getNombre()) && repo.existsByNombre(newName)) {
                log.error("El rol ya existe en la base de datos: {}", newName);
                throw new IllegalStateException("Ya existe un rol con el nombre: " + newName);
            }
        }

        mapper.updateEntityFromDto(dto, e);
        Rol saved = repo.save(e);
        log.debug("Rol creado con ID: {}", saved.getId());
        return mapper.toDto(saved);
    }

    // -------- ELIMINAR --------
    @Transactional
    public void delete(Long id) {
        log.info("Eliminando Rol con ID: {}", id);
        Rol e = repo.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
        e.setActivo(false);
        repo.save(e);
        log.info("Eliminado Rol con ID: {}", id);

    }
}
