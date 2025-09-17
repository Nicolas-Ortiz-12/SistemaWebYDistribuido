package com.distri.proyectodistri.model.base;

import jakarta.persistence.*;
import lombok.Data;

@Data
@MappedSuperclass
public abstract class EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
