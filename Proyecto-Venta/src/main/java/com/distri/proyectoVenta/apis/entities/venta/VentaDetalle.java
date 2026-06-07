package com.distri.proyectoVenta.apis.entities.venta;

import com.distri.proyectoVenta.apis.entities.base.EntidadBase;
import com.distri.proyectoVenta.apis.entities.inventario.Producto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(
        name = "venta_detalle",
        uniqueConstraints = @UniqueConstraint(columnNames = {"venta_id", "producto_id"})
)
@Where(clause = "activo = true")
public class VentaDetalle extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "tasa_iva", nullable = false, precision = 5, scale = 2)
    private BigDecimal tasaIva = BigDecimal.valueOf(10);

    @Column(name = "subtotal_linea", precision = 14, scale = 2)
    private BigDecimal subtotalLinea;

    @Column(name = "total_linea", precision = 14, scale = 2)
    private BigDecimal totalLinea;

}
