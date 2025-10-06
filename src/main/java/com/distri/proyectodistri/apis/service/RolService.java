package com.distri.proyectodistri.apis.service;

import com.distri.proyectodistri.apis.entities.seguridad.Rol;
import com.distri.proyectodistri.apis.mapper.RolMapper;
import com.distri.proyectodistri.apis.repository.RolRepository;
import com.distri.proyectodistri.dto.RolDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RolService {

    private final RolRepository repo;
    private final RolMapper mapper;

    // -------- LISTAR (paginado + búsqueda) --------
    @Transactional(readOnly = true)
    public Page<RolDTO> list(String q, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("id").descending());
        var pageEntities = (q == null || q.isBlank())
                ? repo.findAll(pageable)
                : repo.findByNombreContainingIgnoreCase(q.trim(), pageable);
        return pageEntities.map(mapper::toDto);
    }

    // -------- OBTENER POR ID --------
    @Transactional(readOnly = true)
    public RolDTO get(Long id) {
        Rol e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: id=" + id));
        return mapper.toDto(e);
    }

    // -------- CREAR --------
    @Transactional
    public RolDTO create(RolDTO dto) {
        if (repo.existsByNombre(dto.getNombre())) {
            throw new IllegalStateException("Ya existe un rol con el nombre: " + dto.getNombre());
        }
        Rol e = mapper.toEntity(dto);
        Rol saved = repo.save(e);
        return mapper.toDto(saved);
    }

    // -------- ACTUALIZAR --------
    @Transactional
    public RolDTO update(Long id, RolDTO dto) {
        Rol e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: id=" + id));

        if (dto.getNombre() != null && !dto.getNombre().isBlank()) {
            String newName = dto.getNombre().trim();
            if (!newName.equals(e.getNombre()) && repo.existsByNombre(newName)) {
                throw new IllegalStateException("Ya existe un rol con el nombre: " + newName);
            }
        }

        mapper.updateEntityFromDto(dto, e);
        Rol saved = repo.save(e);
        return mapper.toDto(saved);
    }

    // -------- ELIMINAR --------
    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("Rol no encontrado: id=" + id);
        }
        repo.deleteById(id);
    }
}
