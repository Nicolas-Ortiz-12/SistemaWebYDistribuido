package com.distri.proyectoVenta.apis.repository;

import com.distri.proyectoVenta.apis.entities.inventario.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    Page<Categoria> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
    boolean existsByNombreIgnoreCase(String nombre);
   Optional<Categoria> findByIdAndActivoTrue(@Param("id") Long id);
}
