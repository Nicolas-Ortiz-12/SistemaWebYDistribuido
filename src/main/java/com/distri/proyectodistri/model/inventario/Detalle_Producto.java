

package com.distri.proyectodistri.model.inventario;

import com.distri.proyectodistri.model.base.EntidadBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "detalle_producto")
public class Detalle_Producto extends EntidadBase {

    @OneToOne
    @JoinColumn(name = "producto_id", nullable = false, unique = true)
    private Producto producto;

    @Column(length = 250)
    private String descripcion;

    @Column(name = "unidad_medida", length = 20, nullable = false)
    private String unidadMedida = "UN";
}
