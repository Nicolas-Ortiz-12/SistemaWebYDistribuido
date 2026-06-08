package com.distri.proyectoVenta.apis.repository;

import com.distri.proyectoVenta.apis.entities.compra.Compra;
import com.distri.proyectoVenta.apis.entities.inventario.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface    ProductoRepository extends JpaRepository<Producto, Long> {
    Page<Producto> findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCaseOrCodigoBarrasContainingIgnoreCase(String nombre, String codigo, String codigoBarras, Pageable pageable);

    Optional<Producto> findByCodigoBarrasIgnoreCase(String codigoBarras);

    Optional<Producto> findByIdAndActivoTrue(@Param("id") Long id);

    boolean existsByCodigoIgnoreCase(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);
}
