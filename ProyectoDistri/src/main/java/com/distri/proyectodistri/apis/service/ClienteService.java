package com.distri.proyectodistri.apis.service;

import com.distri.proyectodistri.apis.mapper.ClienteMapper;
import com.distri.proyectodistri.apis.repository.ClienteRepository;
import com.distri.proyectodistri.apis.entities.cliente.Cliente;
import com.distri.proyectodistri.dto.ClienteDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repo;
    private final ClienteMapper mapper;

    // -------- LISTAR (paginado + búsqueda por nombre/RUC)
    @Transactional(readOnly = true)
    public Page<ClienteDTO> list(String q, int page, int size) {
        log.info("Listado de clientes con filtro='{}', página={}, tamaño={}", q, page, size);
        var pageable = PageRequest.of(page, size, Sort.by("id").descending());
        var pageEntities = (q == null || q.isBlank())
                ? repo.findAll(pageable)
                : repo.findByNombreContainingIgnoreCaseOrRucContainingIgnoreCase(q.trim(), q.trim(), pageable);
        log.debug("Cantidad de clientes encontrados: {}", pageEntities.getTotalElements());
        return pageEntities.map(mapper::toDto);
    }

    // -------- OBTENER POR ID
    @Transactional(readOnly = true)
    public ClienteDTO get(Long id) {
        log.info("Buscando cliente por ID: {}", id);
        Cliente e = repo.findById(id)
                .orElseThrow(() ->{
                    log.error("Cliente con ID: {} no encontrado", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado");
                });
        log.debug("Cliente con ID: {} encontrado", e.getNombre());
        return mapper.toDto(e);
    }

    // -------- CREAR
    @Transactional
    public ClienteDTO create(ClienteDTO dto) {
        log.info("Creando cliente con nombre='{}' y RUC='{}'", dto.getNombre(), dto.getRuc());
        if (dto.getRuc() != null && !dto.getRuc().isBlank() && repo.existsByRuc(dto.getRuc().trim())) {
            log.error("Intento de creación duplicada: ya existe cliente con RUC={}", dto.getRuc());
            throw new IllegalStateException("Ya existe un cliente con el RUC: " + dto.getRuc());
        }
        Cliente e = mapper.toEntity(dto);
        Cliente saved = repo.save(e);
        log.debug("Cliente guardado correctamente con id={}", saved.getId());
        return mapper.toDto(saved);
    }

    // -------- ACTUALIZAR
    @Transactional
    public ClienteDTO update(Long id, ClienteDTO dto) {
        log.info("Actualizando cliente con id={}", id);
        Cliente e = repo.findById(id)
                .orElseThrow(() -> {
                    log.error("Cliente con id={} no encontrado", id);
                    return new IllegalArgumentException("Cliente no encontrado: id=" + id);
                });

        // Si validás cambio de RUC (opcional):
        if (dto.getRuc() != null && !dto.getRuc().isBlank()) {
            String newRuc = dto.getRuc().trim();
            if (!newRuc.equals(e.getRuc()) && repo.existsByRuc(newRuc)) {
                log.error("Cliente con ruc={} ya existente ", newRuc);
                throw new IllegalStateException("Ya existe un cliente con el RUC: " + newRuc);
            }
        }

        mapper.updateEntityFromDto(dto, e); // copia solo no-nulos
        Cliente saved = repo.save(e);
        log.debug("Cliente actualizado correctamente con id={}", saved.getId());
        return mapper.toDto(saved);
    }

    // -------- ELIMINAR
    @Transactional
    public void delete(Long id) {
        log.warn("Intentando eliminar cliente con id={}", id);
        Cliente e = repo.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrada"));
        e.setActivo(false);
        repo.save(e);
        log.info("Cliente eliminado exitosamente id={}", id);

    }
}
