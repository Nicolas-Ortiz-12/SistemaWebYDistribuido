package com.distri.proyectodistri.apis.mapper;

import com.distri.proyectodistri.apis.entities.seguridad.Rol;
import com.distri.proyectodistri.dto.RolDTO;
import org.mapstruct.Mapper;

@Mapper(config = MapperCentralConfig.class)
public interface RolMapper extends BaseMapper<Rol, RolDTO> {}
