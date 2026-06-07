package com.distri.proyectodistri.apis.entities.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

@Getter
@Setter
@MappedSuperclass
@Where(clause = "activo = true")
public abstract class Persona extends EntidadBase {

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(length = 50)
    private String ruc;

    @Column(length = 50)
    private String telefono;

    @Column(length = 200)
    private String correo;

    @Column(length = 200)
    private String direccion;

}
