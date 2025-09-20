package com.distri.proyectodistri.entities.seguridad;

import com.distri.proyectodistri.entities.base.EntidadBase;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "rol")
public class Rol extends EntidadBase {

    @Column(nullable = false, length = 50, unique = true)
    private String nombre; // ADMIN, VENDEDOR, DEPOSITO


}
