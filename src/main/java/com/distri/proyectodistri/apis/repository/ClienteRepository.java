package com.distri.proyectodistri.apis.repository;

import com.distri.proyectodistri.apis.entities.cliente.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    // Buscar por RUC exacto (útil para validar duplicados)
    Optional<Cliente> findByRuc(String ruc);

    boolean existsByRuc(String ruc);

    // Búsqueda combinada por nombre o RUC (LIKE, case-insensitive)
    Page<Cliente> findByNombreContainingIgnoreCaseOrRucContainingIgnoreCase(
            String nombre, String ruc, Pageable pageable
    );

    // Solo por nombre
    Page<Cliente> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    // Solo por RUC (LIKE)
    Page<Cliente> findByRucContainingIgnoreCase(String ruc, Pageable pageable);
}

