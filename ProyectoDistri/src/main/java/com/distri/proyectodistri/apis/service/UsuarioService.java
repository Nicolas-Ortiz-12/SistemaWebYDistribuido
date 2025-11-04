package com.distri.proyectodistri.apis.service;

import com.distri.proyectodistri.apis.entities.seguridad.Rol;
import com.distri.proyectodistri.apis.entities.seguridad.Usuario;
import com.distri.proyectodistri.apis.mapper.RolMapper;
import com.distri.proyectodistri.apis.mapper.UsuarioMapper;
import com.distri.proyectodistri.apis.repository.UsuarioRepository;
import com.distri.proyectodistri.dto.UsuarioCreateDTO;
import com.distri.proyectodistri.dto.UsuarioDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import com.distri.proyectodistri.apis.repository.RolRepository;
import org.springframework.web.server.ResponseStatusException;


@RequiredArgsConstructor
@Service
@Slf4j
public class UsuarioService {

    private final UsuarioRepository repo;
    private final UsuarioMapper mapper;
    private final RolMapper rolMapper;
    private final RolRepository rolRepository;



    // -------- LISTAR (paginado + búsqueda) --------
    @Transactional(readOnly = true)
    public Page<UsuarioDTO> list(String q, int page, int size) {
        log.info("Listado de Usuarios con filtro='{}', página={}, tamaño={}", q, page, size);
        var pageable = PageRequest.of(page, size, Sort.by("id").descending());
        var pageEntities = (q == null || q.isBlank())
                ? repo.findAll(pageable)
                : repo.findByUsuarioContainingIgnoreCaseOrNombreCompletoContainingIgnoreCase(q.trim(), q.trim(), pageable);
        log.debug("Cantidad de Usuarios encontrados: {}", pageEntities.getTotalElements());
        return pageEntities.map(mapper::toDto);
    }

    // -------- OBTENER POR ID --------
    @Transactional(readOnly = true)
    public UsuarioDTO get(Long id) {
        log.info("Buscando Usuario con ID: {}", id);
        Usuario e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuario no encontrado: id=" + id));
        log.debug("Usuario encontrado: {}", e);
        return mapper.toDto(e);
    }

    // -------- CREAR --------

    @Transactional
    public UsuarioDTO create(UsuarioCreateDTO in) {
        log.info("Creando Usuario: {}", in);
        final String username = in.getUsuario().trim();
        if (repo.existsByUsuario(username)) {
            log.info("Usuario existente: {}", username);
            throw new IllegalStateException("Ya existe un usuario con el nombre: " + username);
        }

        // Cargar roles existentes por ID y validar que todos existan
        List<Long> ids = in.getRoleIds();
        Set<Rol> roles = new HashSet<>(rolRepository.findAllById(ids));
        if (roles.size() != ids.stream().filter(Objects::nonNull).distinct().count()) {
            log.error("Roles IDS no existen: {}", ids);
            throw new IllegalArgumentException("Alguno(s) de los roleIds no existe(n).");
        }

        // Construir entidad (sin usar mapper para roles/password)
        Usuario e = new Usuario();
        e.setUsuario(username);
        e.setNombreCompleto(in.getNombreCompleto().trim());
        e.setPassword(in.getPassword()); // hash
        e.setRoles(roles);

        Usuario saved = repo.save(e);
        UsuarioDTO out = mapper.toDto(saved);
        log.debug("Usuario creado: {}", out.getNombreCompleto());
        return out;
    }
    // -------- ACTUALIZAR --------
    @Transactional
    public UsuarioDTO update(Long id, UsuarioDTO dto) {
        log.info("Actualizando Usuario con ID: {}", id);
        Usuario e = repo.findById(id)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuario no encontrado: id=" + id);
                });

        if (dto.getUsuario() != null && !dto.getUsuario().isBlank()) {
            String newUsername = dto.getUsuario().trim();
            if (!newUsername.equals(e.getUsuario()) && repo.existsByUsuario(newUsername)) {
                log.info("Usuario existente: {}", newUsername);
                throw new IllegalStateException("Ya existe un usuario con el nombre: " + newUsername);
            }
        }

        // Evitar que la lista de roles se borre si viene nula
        if (dto.getRoles() == null) {
            dto.setRoles(rolMapper.toDtoList(new ArrayList<>(e.getRoles())));
        }

        mapper.updateEntityFromDto(dto, e);
        Usuario saved = repo.save(e);
        log.debug("Usuario actualizado: {}", saved.getNombreCompleto());
        return mapper.toDto(saved);
    }


    // -------- ELIMINAR --------
    @Transactional
    public void delete(Long id) {
        log.info("Eliminando Usuario con ID: {}", id);
        Usuario e = repo.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
        e.setActivo(false);
        repo.save(e);
        log.info("Usuario eliminado con ID: {}", id);

    }
}