// Cliente
// src/main/java/com/distri/proyectodistri/mapper/ClienteMapper.java
package com.distri.proyectodistri.apis.mapper;

import com.distri.proyectodistri.apis.entities.cliente.Cliente;
import com.distri.proyectodistri.apis.entities.seguridad.Rol;
import com.distri.proyectodistri.dto.ClienteDTO;
import com.distri.proyectodistri.dto.RolDTO;
import org.mapstruct.*;

@Mapper(config = MapperCentralConfig.class, uses = { NumberMapper.class })
public interface ClienteMapper extends BaseMapper<Cliente, ClienteDTO> {

    @Override
    Cliente toEntity(ClienteDTO dto);
    @Override
    @Mapping(source = "nombre", target = "nombre")
    ClienteDTO toDto(Cliente entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(ClienteDTO dto, @MappingTarget Cliente entity);
}
