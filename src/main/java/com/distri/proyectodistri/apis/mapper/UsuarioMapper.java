package com.distri.proyectodistri.apis.mapper;

import com.distri.proyectodistri.apis.entities.seguridad.Usuario;
import com.distri.proyectodistri.dto.UsuarioDTO;
import org.mapstruct.Mapper;

@Mapper(config = MapperCentralConfig.class, uses = {RolMapper.class})
public interface UsuarioMapper extends BaseMapper<Usuario, UsuarioDTO> {}

