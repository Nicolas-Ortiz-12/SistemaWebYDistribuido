package com.distri.proyectodistri.model.cliente;

import com.distri.proyectodistri.model.base.Persona;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "proveedor")
public class Proveedor extends Persona {

    @Column(nullable = false)
    private boolean activo = true;
}
