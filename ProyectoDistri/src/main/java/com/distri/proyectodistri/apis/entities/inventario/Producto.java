package com.distri.proyectodistri.apis.entities.inventario;

import com.distri.proyectodistri.apis.entities.base.EntidadBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "producto")
@ToString(onlyExplicitlyIncluded = true)
@Where(clause = "activo = true")
public class Producto extends EntidadBase {

    @Column(nullable = false, length = 64, unique = true)
    @ToString.Include
    private String codigo;

    @Column(nullable = false, length = 200)
    @ToString.Include
    private String nombre;


    @Column(nullable = false, columnDefinition = "NUMERIC(14,2) DEFAULT 0")
    private BigDecimal costo = BigDecimal.ZERO;

    @Column(nullable = false, columnDefinition = "NUMERIC(14,2) DEFAULT 0")
    private BigDecimal precio = BigDecimal.ZERO;

    // OJO: pese al nombre, es el stock ACTUAL (ventas lo descuentan, compras lo suman).
    // Se mantiene el nombre porque lo usan la columna, la API y el front.
    @Column(name = "stock_minimo", nullable = false, columnDefinition = "NUMERIC(14,3) DEFAULT 0")
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    // Campos que antes tenías en detalle_producto (ahora dentro de producto)
    @Column(length = 250)
    private String descripcion;

    @Column(name = "unidad_medida", length = 20, nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'UN'")
    private String unidadMedida = "UN";
}
