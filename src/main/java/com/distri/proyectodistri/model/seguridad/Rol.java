package com.distri.proyectodistri.model.seguridad;

import com.distri.proyectodistri.model.base.EntidadBase;
import jakarta.persistence.*;
import lombok.Data;


@Data
@Entity
@Table(name = "rol")
public class Rol extends EntidadBase {

    @Column(nullable = false, length = 50, unique = true)
    private String nombre; // ADMIN, VENDEDOR, DEPOSITO


}
