package com.distri.proyectodistri.entities.base;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
