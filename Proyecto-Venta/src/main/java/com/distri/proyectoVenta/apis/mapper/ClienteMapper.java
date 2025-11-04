// Cliente
// src/main/java/com/distri/proyectodistri/mapper/ClienteMapper.java
package com.distri.proyectoVenta.apis.mapper;

import com.distri.proyectoVenta.apis.entities.cliente.Cliente;
import com.distri.proyectoVenta.apis.entities.seguridad.Rol;
import com.distri.proyectoVenta.apis.mapper.MapperCentralConfig;
import com.distri.proyectoVenta.dto.ClienteDTO;
import com.distri.proyectoVenta.dto.RolDTO;
import org.mapstruct.*;

@Mapper(config = MapperCentralConfig.class, uses = { NumberMapper.class })
public interface ClienteMapper extends BaseMapper<Cliente, ClienteDTO> {

    @Override
    Cliente toEntity(ClienteDTO dto);
    @Override
    //@Mapping(source = "nombre", target = "nombre")
    ClienteDTO toDto(Cliente entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(ClienteDTO dto, @MappingTarget Cliente entity);
}
