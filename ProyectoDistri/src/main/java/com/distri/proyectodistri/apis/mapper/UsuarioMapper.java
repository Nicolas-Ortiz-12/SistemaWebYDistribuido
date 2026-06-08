package com.distri.proyectodistri.apis.mapper;

import com.distri.proyectodistri.apis.entities.seguridad.Rol;
import com.distri.proyectodistri.apis.entities.seguridad.Usuario;
import com.distri.proyectodistri.dto.RolDTO;
import com.distri.proyectodistri.dto.UsuarioDTO;
import org.mapstruct.*;

// UsuarioMapper.java
@Mapper(componentModel = "spring", uses = { RolMapper.class }, config = MapperCentralConfig.class)
public interface UsuarioMapper {

    // No exponer password en DTO

    UsuarioDTO toDto(Usuario entity);

    // En create/upgrade de entidad, el service setea password y roles
    @Mapping(target = "roles", ignore = true)

    Usuario toEntity(UsuarioDTO dto);


    void updateEntityFromDto(UsuarioDTO dto, @MappingTarget Usuario entity);
}