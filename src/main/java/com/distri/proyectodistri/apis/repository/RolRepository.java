package com.distri.proyectodistri.apis.repository;

import com.distri.proyectodistri.apis.entities.seguridad.Rol;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    // Buscar por nombre exacto
    Optional<Rol> findByNombre(String nombre);

    // Verificar si existe un rol con ese nombre
    boolean existsByNombre(String nombre);

    // Buscar por nombre parcial (paginado, insensible a mayúsculas)
    Page<Rol> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
}
