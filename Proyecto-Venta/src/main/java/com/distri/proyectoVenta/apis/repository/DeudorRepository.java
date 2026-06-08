package com.distri.proyectoVenta.apis.repository;

import com.distri.proyectoVenta.apis.entities.venta.Deudor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeudorRepository extends JpaRepository<Deudor, Long> {
    Page<Deudor> findByNombreContainingIgnoreCaseAndActivoTrue(String nombre, Pageable pageable);
    Page<Deudor> findByActivoTrue(Pageable pageable);
    Optional<Deudor> findByNombreIgnoreCaseAndActivoTrue(String nombre);
    
    @Query("SELECT d FROM Deudor d WHERE d.totalAdeudado > 0 AND d.activo = true")
    Page<Deudor> findConDeuda(Pageable pageable);

    Optional<Deudor> findByIdAndActivoTrue(Long id);
}
