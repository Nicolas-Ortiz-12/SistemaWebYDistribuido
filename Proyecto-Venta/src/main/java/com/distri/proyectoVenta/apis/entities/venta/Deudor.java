package com.distri.proyectoVenta.apis.entities.venta;

import com.distri.proyectoVenta.apis.entities.base.EntidadBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "deudor")
@ToString(onlyExplicitlyIncluded = true)
public class Deudor extends EntidadBase {

    @Column(nullable = false, length = 150)
    @ToString.Include
    private String nombre;

    @Column(name = "total_adeudado", nullable = false, precision = 14, scale = 2)
    @ToString.Include
    private BigDecimal totalAdeudado = BigDecimal.ZERO;
}
