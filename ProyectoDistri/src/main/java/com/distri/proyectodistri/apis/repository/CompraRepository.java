// src/main/java/com/distri/proyectodistri/apis/repository/CompraRepository.java
package com.distri.proyectodistri.apis.repository;

import com.distri.proyectodistri.apis.entities.compra.Compra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    Page<Compra> findByProveedor_Id(Long proveedorId, Pageable pageable);

    // Usa el nombre EXACTO del campo en la entidad:
    Page<Compra> findByFechaEmisionBetween(LocalDate desde, LocalDate hasta, Pageable pageable);

   Optional<Compra> findByIdAndActivoTrue(@Param("id") Long id);

    Page<Compra> findByProveedor_IdAndFechaEmisionBetween(Long proveedorId, LocalDate desde, LocalDate hasta, Pageable pageable);
}

