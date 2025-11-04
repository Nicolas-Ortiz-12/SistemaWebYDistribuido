package com.distri.proyectodistri.apis.repository;

import com.distri.proyectodistri.apis.entities.compra.Compra;
import com.distri.proyectodistri.apis.entities.seguridad.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar por nombre de usuario exacto
    Optional<Usuario> findByUsuario(String usuario);

    // Verificar existencia de un nombre de usuario
    boolean existsByUsuario(String usuario);

    Optional<Usuario> findByIdAndActivoTrue(@Param("id") Long id);

    // Búsqueda combinada (por usuario o nombre completo)
    Page<Usuario> findByUsuarioContainingIgnoreCaseOrNombreCompletoContainingIgnoreCase(
            String usuario, String nombreCompleto, Pageable pageable
    );
}
