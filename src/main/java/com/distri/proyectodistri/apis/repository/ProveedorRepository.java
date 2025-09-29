package com.distri.proyectodistri.apis.repository;

import com.distri.proyectodistri.apis.entities.cliente.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
}
