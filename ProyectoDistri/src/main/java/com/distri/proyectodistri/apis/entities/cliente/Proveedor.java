package com.distri.proyectodistri.apis.entities.cliente;

import com.distri.proyectodistri.apis.entities.base.Persona;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@Table(name = "proveedor")
@Where(clause = "activo = true")
public class Proveedor extends Persona {

}
