package com.distri.proyectodistri.model.cliente;

import com.distri.proyectodistri.model.base.Persona;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
public class Cliente extends Persona {
    // sin campos extra por ahora (la tabla ya está alineada)
}
