package com.distri.proyectoVenta.apis.service;

import com.distri.proyectoVenta.apis.entities.venta.Deudor;
import com.distri.proyectoVenta.apis.repository.DeudorRepository;
import com.distri.proyectoVenta.dto.DeudorDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeudorService {

    private final DeudorRepository repo;

    private DeudorDTO toDto(Deudor e) {
        DeudorDTO dto = new DeudorDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setTotalAdeudado(e.getTotalAdeudado() != null ? e.getTotalAdeudado().doubleValue() : 0.0);
        return dto;
    }

    @Transactional(readOnly = true)
    public Page<DeudorDTO> list(String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Deudor> data;

        if (q != null && !q.isBlank()) {
            data = repo.findByNombreContainingIgnoreCaseAndActivoTrue(q.trim(), pageable);
        } else {
            data = repo.findByActivoTrue(pageable);
        }
        return data.map(this::toDto);
    }
    
    @Transactional(readOnly = true)
    public Page<DeudorDTO> listConDeuda(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("totalAdeudado").descending());
        return repo.findConDeuda(pageable).map(this::toDto);
    }

    @Transactional
    public DeudorDTO create(DeudorDTO dto) {
        Deudor e = new Deudor();
        e.setNombre(dto.getNombre());
        e.setTotalAdeudado(BigDecimal.ZERO); // New debtor has 0 debt initially
        return toDto(repo.save(e));
    }

    @Transactional
    public void saldar(Long id) {
        Deudor e = repo.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deudor no encontrado"));
        e.setTotalAdeudado(BigDecimal.ZERO);
        repo.save(e);
        log.info("Deuda saldada para el deudor: {}", id);
    }
    
    @Transactional
    public void addDeuda(Long id, BigDecimal monto) {
        Deudor e = repo.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deudor no encontrado"));
        e.setTotalAdeudado(e.getTotalAdeudado().add(monto));
        repo.save(e);
    }
}
