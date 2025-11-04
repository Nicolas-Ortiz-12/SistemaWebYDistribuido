package com.distri.proyectoVenta.apis.mapper;

import com.distri.proyectoVenta.apis.entities.seguridad.Rol;
import com.distri.proyectoVenta.apis.mapper.BaseMapper;
import com.distri.proyectoVenta.apis.mapper.MapperCentralConfig;
import com.distri.proyectoVenta.dto.RolDTO;
import org.mapstruct.*;

@Mapper(config = MapperCentralConfig.class)
public interface RolMapper extends BaseMapper<Rol, RolDTO> {

    @Override
    Rol toEntity(RolDTO dto);

    @Override
    @Mapping(source = "nombre", target = "nombre")
    RolDTO toDto(Rol entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(RolDTO dto, @MappingTarget Rol entity);
}
