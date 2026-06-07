package com.distri.proyectodistri.apis.repository;

import com.distri.proyectodistri.apis.entities.compra.Compra;
import com.distri.proyectodistri.apis.entities.seguridad.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    // Buscar por nombre exacto
    Optional<Rol> findByNombre(String nombre);

    // Verificar si existe un rol con ese nombre
    boolean existsByNombre(String nombre);

    Optional<Rol> findByIdAndActivoTrue(@Param("id") Long id);

    // Buscar por nombre parcial (paginado, insensible a mayúsculas)
    Page<Rol> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
