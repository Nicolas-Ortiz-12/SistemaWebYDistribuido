// src/main/java/com/distri/proyectodistri/apis/repository/ProveedorRepository.java
package com.distri.proyectoVenta.apis.repository;

import com.distri.proyectoVenta.apis.entities.cliente.Proveedor;
import com.distri.proyectoVenta.apis.entities.compra.Compra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByRuc(String ruc);

    boolean existsByRuc(String ruc);

    Page<Proveedor> findByNombreContainingIgnoreCaseOrRucContainingIgnoreCase(
            String nombre, String ruc, Pageable pageable
    );
    Optional<Proveedor> findByIdAndActivoTrue(@Param("id") Long id);

    Page<Proveedor> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    Page<Proveedor> findByRucContainingIgnoreCase(String ruc, Pageable pageable);
}
