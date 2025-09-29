package com.distri.proyectodistri.apis.entities.cliente;

import com.distri.proyectodistri.apis.entities.base.Persona;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
public class Cliente extends Persona {
    // sin campos extra por ahora (la tabla ya está alineada)
}
