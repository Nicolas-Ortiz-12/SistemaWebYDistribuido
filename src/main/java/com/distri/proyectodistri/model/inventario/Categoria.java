package com.distri.proyectodistri.model.inventario;

import com.distri.proyectodistri.model.base.EntidadBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "categoria")
public class Categoria extends EntidadBase {

    @Column(nullable = false, length = 120, unique = true)
    private String nombre;


}
