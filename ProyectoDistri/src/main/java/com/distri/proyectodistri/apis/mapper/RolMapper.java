package com.distri.proyectodistri.apis.mapper;

import com.distri.proyectodistri.apis.entities.seguridad.Rol;
import com.distri.proyectodistri.dto.RolDTO;
import org.mapstruct.*;

@Mapper(config = MapperCentralConfig.class)
public interface RolMapper extends BaseMapper<Rol, RolDTO> {

    @Override
    Rol toEntity(RolDTO dto);

    @Override
    //@Mapping(source = "nombre", target = "nombre")
    RolDTO toDto(Rol entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(RolDTO dto, @MappingTarget Rol entity);
}
