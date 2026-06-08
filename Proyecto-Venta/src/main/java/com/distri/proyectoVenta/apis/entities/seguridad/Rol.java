package com.distri.proyectoVenta.apis.entities.seguridad;

import com.distri.proyectoVenta.apis.entities.base.EntidadBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;


@Getter
@Setter
@Entity
@Table(name = "rol")
@Where(clause = "activo = true")
public class Rol extends EntidadBase {

    @Column(nullable = false, length = 50, unique = true)
    private String nombre; // ADMIN, VENDEDOR, DEPOSITO


}
