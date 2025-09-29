package com.distri.proyectodistri.apis.entities.inventario;

import com.distri.proyectodistri.apis.entities.base.EntidadBase;
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
