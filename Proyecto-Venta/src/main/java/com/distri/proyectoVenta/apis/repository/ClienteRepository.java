package com.distri.proyectoVenta.apis.repository;

import com.distri.proyectoVenta.apis.entities.cliente.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;


import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByRuc(String ruc);

    boolean existsByRuc(String ruc);

    // Búsqueda combinada por nombre o RUC (LIKE, case-insensitive)
    Page<Cliente> findByNombreContainingIgnoreCaseOrRucContainingIgnoreCase(
            String nombre, String ruc, Pageable pageable
    );
    Optional<Cliente> findByIdAndActivoTrue(@Param("id") Long id);


    Page<Cliente> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    // Solo por RUC (LIKE)
    Page<Cliente> findByRucContainingIgnoreCase(String ruc, Pageable pageable);
}

