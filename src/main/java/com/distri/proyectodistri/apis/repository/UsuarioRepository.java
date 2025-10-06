package com.distri.proyectodistri.apis.repository;

import com.distri.proyectodistri.apis.entities.seguridad.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar por nombre de usuario exacto
    Optional<Usuario> findByUsuario(String usuario);

    // Verificar existencia de un nombre de usuario
    boolean existsByUsuario(String usuario);

    // Búsqueda combinada (por usuario o nombre completo)
    Page<Usuario> findByUsuarioContainingIgnoreCaseOrNombreCompletoContainingIgnoreCase(
            String usuario, String nombreCompleto, Pageable pageable
    );
}
