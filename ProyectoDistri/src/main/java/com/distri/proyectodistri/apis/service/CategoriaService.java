package com.distri.proyectodistri.apis.service;

import com.distri.proyectodistri.apis.mapper.CategoriaMapper;
import com.distri.proyectodistri.apis.repository.CategoriaRepository;
import com.distri.proyectodistri.apis.entities.inventario.Categoria;
import com.distri.proyectodistri.dto.CategoriaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository repo;
    private final CategoriaMapper mapper;


    @Transactional(readOnly = true)
    public Page<CategoriaDTO> list(String q, int page, int size) {
        log.info("Listando categorias con filtro='{}', página={}, tamaño={}", q, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Categoria> data = (q == null || q.isBlank())
                ? repo.findAll(pageable)
                : repo.findByNombreContainingIgnoreCase(q.trim(), pageable);
        log.debug("Cantidad de categorias encontradas: {}", data.getTotalElements());
        return data.map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public CategoriaDTO get(Long id) {
        log.info("Buscando categoria con id={}", id);
        Categoria e = repo.findById(id)
                .orElseThrow(() -> {
                    log.error("Categoria no encontrada: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND);
                });
        log.info("Categoria encontrada: {}", e.getNombre());
        return mapper.toDto(e);
    }

    @Transactional
    public CategoriaDTO create(CategoriaDTO dto) {
        log.info("Creando categoria con nombre='{}'", dto.getNombre());
        if (repo.existsByNombreIgnoreCase(dto.getNombre())) {
            log.error("Error al crear: ya existe esa categoria='{}'", dto.getNombre());
            throw new IllegalStateException("Ya existe una categoría con ese nombre");
        }
        Categoria e = mapper.toEntity(dto);
        Categoria saved = repo.save(e);
        log.info("Categoria creada exitosamente con id={} y nombre='{}'", saved.getId(), saved.getNombre());
        return mapper.toDto(saved);
    }

    @Transactional
    public CategoriaDTO update(Long id, CategoriaDTO dto) {
        log.info("Actualizando categoria con id={} y nuevo nombre='{}'", id, dto.getNombre());
        Categoria e = repo.findById(id)
                .orElseThrow(() -> {
                    log.error("Error al actualizar: categoria no encontrada con id={}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada");
                });

        mapper.updateEntityFromDto(dto, e);
        Categoria saved = repo.save(e);
        log.info("Categoria actualizada con exito id={} nombre='{}'", saved.getId(), saved.getNombre());
        return mapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        log.warn("Intentando eliminar (soft) categoría con id={}", id);
        Categoria e = repo.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
        e.setActivo(false);
        repo.save(e);
        log.info("Categoría marcada como inactiva id={}", id);
    }

}
