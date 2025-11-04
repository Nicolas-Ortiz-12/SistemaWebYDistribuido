package com.distri.proyectodistri.apis.entities.seguridad;

import com.distri.proyectodistri.apis.entities.base.EntidadBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "usuario")
@Where(clause = "activo = true")
public class Usuario extends EntidadBase {

    @Column(nullable = false, length = 80, unique = true)
    private String usuario;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "nombre_completo", nullable = false, length = 200)
    private String nombreCompleto;

    @ManyToMany
    @JoinTable(
            name = "usuario_rol",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Rol> roles = new HashSet<>();
}
