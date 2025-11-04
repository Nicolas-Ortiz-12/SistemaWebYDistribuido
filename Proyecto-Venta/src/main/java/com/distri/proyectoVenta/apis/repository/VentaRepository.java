// src/main/java/com/distri/proyectodistri/apis/repository/VentaRepository.java
package com.distri.proyectoVenta.apis.repository;

import com.distri.proyectoVenta.apis.entities.compra.Compra;
import com.distri.proyectoVenta.apis.entities.venta.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    Page<Venta> findByCliente_Id(Long clienteId, Pageable pageable);

    Optional<Venta> findByIdAndActivoTrue(@Param("id") Long id);

    Page<Venta> findByFechaVentaBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    Page<Venta> findByCliente_IdAndFechaVentaBetween(Long clienteId, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
}
