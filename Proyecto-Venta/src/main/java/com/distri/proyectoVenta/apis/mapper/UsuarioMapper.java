package com.distri.proyectoVenta.apis.mapper;

import com.distri.proyectoVenta.apis.entities.seguridad.Rol;
import com.distri.proyectoVenta.apis.entities.seguridad.Usuario;
import com.distri.proyectoVenta.apis.mapper.MapperCentralConfig;
import com.distri.proyectoVenta.dto.RolDTO;
import com.distri.proyectoVenta.dto.UsuarioDTO;
import org.mapstruct.*;

// UsuarioMapper.java
@Mapper(componentModel = "spring", uses = { RolMapper.class }, config = MapperCentralConfig.class)
public interface UsuarioMapper {

    // No exponer password en DTO

    UsuarioDTO toDto(Usuario entity);

    // En create/upgrade de entidad, el service setea password y roles
    //@Mapping(target = "roles", ignore = true)

    Usuario toEntity(UsuarioDTO dto);


    void updateEntityFromDto(UsuarioDTO dto, @MappingTarget Usuario entity);
}