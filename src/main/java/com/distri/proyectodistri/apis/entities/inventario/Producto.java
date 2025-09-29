package com.distri.proyectodistri.apis.entities.inventario;

import com.distri.proyectodistri.apis.entities.base.EntidadBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "producto")
@ToString(onlyExplicitlyIncluded = true)
public class Producto extends EntidadBase {

    @Column(nullable = false, length = 64, unique = true)
    @ToString.Include
    private String codigo;

    @Column(nullable = false, length = 200)
    @ToString.Include
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal costo = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal precio = BigDecimal.ZERO;

    @Column(name = "stock_minimo", nullable = false, precision = 14, scale = 3)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @OneToOne(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Detalle_Producto detalle;

    public void setDetalle(Detalle_Producto detalle) {

        if (this.detalle != null) {
            this.detalle.setProducto(null);
        }
        this.detalle = detalle;
        if (detalle != null) {
            detalle.setProducto(this);
        }
    }
}
