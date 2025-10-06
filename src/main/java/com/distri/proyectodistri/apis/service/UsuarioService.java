package com.distri.proyectodistri.apis.service;

import com.distri.proyectodistri.apis.entities.seguridad.Usuario;
import com.distri.proyectodistri.apis.mapper.RolMapper;
import com.distri.proyectodistri.apis.mapper.UsuarioMapper;
import com.distri.proyectodistri.apis.repository.UsuarioRepository;
import com.distri.proyectodistri.dto.UsuarioDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository repo;
    private final UsuarioMapper mapper;
    private final RolMapper rolMapper;

    // -------- LISTAR (paginado + búsqueda) --------
    @Transactional(readOnly = true)
    public Page<UsuarioDTO> list(String q, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("id").descending());
        var pageEntities = (q == null || q.isBlank())
                ? repo.findAll(pageable)
                : repo.findByUsuarioContainingIgnoreCaseOrNombreCompletoContainingIgnoreCase(q.trim(), q.trim(), pageable);
        return pageEntities.map(mapper::toDto);
    }

    // -------- OBTENER POR ID --------
    @Transactional(readOnly = true)
    public UsuarioDTO get(Long id) {
        Usuario e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: id=" + id));
        return mapper.toDto(e);
    }

    // -------- CREAR --------
    @Transactional
    public UsuarioDTO create(UsuarioDTO dto) {
        if (repo.existsByUsuario(dto.getUsuario())) {
            throw new IllegalStateException("Ya existe un usuario con el nombre: " + dto.getUsuario());
        }
        Usuario e = mapper.toEntity(dto);
        Usuario saved = repo.save(e);
        return mapper.toDto(saved);
    }

    // -------- ACTUALIZAR --------
    @Transactional
    public UsuarioDTO update(Long id, UsuarioDTO dto) {
        Usuario e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: id=" + id));

        if (dto.getUsuario() != null && !dto.getUsuario().isBlank()) {
            String newUsername = dto.getUsuario().trim();
            if (!newUsername.equals(e.getUsuario()) && repo.existsByUsuario(newUsername)) {
                throw new IllegalStateException("Ya existe un usuario con el nombre: " + newUsername);
            }
        }

        // Evitar que la lista de roles se borre si viene nula
        if (dto.getRoles() == null) {
            dto.setRoles(rolMapper.toDtoList(new ArrayList<>(e.getRoles())));
        }

        mapper.updateEntityFromDto(dto, e);
        Usuario saved = repo.save(e);
        return mapper.toDto(saved);
    }


    // -------- ELIMINAR --------
    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("Usuario no encontrado: id=" + id);
        }
        repo.deleteById(id);
    }
}
