package com.distri.proyectodistri.apis.service;

import com.distri.proyectodistri.apis.mapper.ClienteMapper;
import com.distri.proyectodistri.apis.repository.ClienteRepository;
import com.distri.proyectodistri.apis.entities.cliente.Cliente;
import com.distri.proyectodistri.dto.ClienteDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository repo;
    private final ClienteMapper mapper;

    // -------- LISTAR (paginado + búsqueda por nombre/RUC)
    @Transactional(readOnly = true)
    public Page<ClienteDTO> list(String q, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("id").descending());
        var pageEntities = (q == null || q.isBlank())
                ? repo.findAll(pageable)
                : repo.findByNombreContainingIgnoreCaseOrRucContainingIgnoreCase(q.trim(), q.trim(), pageable);
        return pageEntities.map(mapper::toDto);
    }

    // -------- OBTENER POR ID
    @Transactional(readOnly = true)
    public ClienteDTO get(Long id) {
        Cliente e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: id=" + id));
        return mapper.toDto(e);
    }

    // -------- CREAR
    @Transactional
    public ClienteDTO create(ClienteDTO dto) {
        // Si validás unicidad de RUC (opcional):
        if (dto.getRuc() != null && !dto.getRuc().isBlank() && repo.existsByRuc(dto.getRuc().trim())) {
            throw new IllegalStateException("Ya existe un cliente con el RUC: " + dto.getRuc());
        }
        Cliente e = mapper.toEntity(dto);
        Cliente saved = repo.save(e);
        return mapper.toDto(saved);
    }

    // -------- ACTUALIZAR
    @Transactional
    public ClienteDTO update(Long id, ClienteDTO dto) {
        Cliente e = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: id=" + id));

        // Si validás cambio de RUC (opcional):
        if (dto.getRuc() != null && !dto.getRuc().isBlank()) {
            String newRuc = dto.getRuc().trim();
            if (!newRuc.equals(e.getRuc()) && repo.existsByRuc(newRuc)) {
                throw new IllegalStateException("Ya existe un cliente con el RUC: " + newRuc);
            }
        }

        mapper.updateEntityFromDto(dto, e); // copia solo no-nulos
        Cliente saved = repo.save(e);
        return mapper.toDto(saved);
    }

    // -------- ELIMINAR
    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("Cliente no encontrado: id=" + id);
        }
        repo.deleteById(id);
    }
}
