package com.distri.proyectoVenta.apis.entities.cliente;

import com.distri.proyectoVenta.apis.entities.base.Persona;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "cliente")
@Where(clause = "activo = true")
public class Cliente extends Persona {


}
