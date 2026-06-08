package com.distri.proyectoVenta.apis.repository;

import com.distri.proyectoVenta.apis.entities.venta.VentaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Long> {
}
