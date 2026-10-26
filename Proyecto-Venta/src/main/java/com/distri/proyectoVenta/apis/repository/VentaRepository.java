// src/main/java/com/distri/proyectodistri/apis/repository/VentaRepository.java
package com.distri.proyectoVenta.apis.repository;

import com.distri.proyectoVenta.apis.entities.compra.Compra;
import com.distri.proyectoVenta.apis.entities.venta.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    // Id que va a tener la próxima venta: el último valor entregado por la secuencia más uno.
    // Es solo una vista previa; el número real lo fija la base al insertar.
    @Query(value = "SELECT COALESCE(pg_sequence_last_value(pg_get_serial_sequence('venta', 'id')::regclass), 0) + 1",
            nativeQuery = true)
    Long proximoId();

    Page<Venta> findByCliente_Id(Long clienteId, Pageable pageable);

    Optional<Venta> findByIdAndActivoTrue(@Param("id") Long id);

    Page<Venta> findByFechaVentaBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    Page<Venta> findByCliente_IdAndFechaVentaBetween(Long clienteId, LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
}
