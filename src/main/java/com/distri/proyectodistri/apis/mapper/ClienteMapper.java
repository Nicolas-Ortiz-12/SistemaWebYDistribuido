// Cliente
// src/main/java/com/distri/proyectodistri/mapper/ClienteMapper.java
package com.distri.proyectodistri.apis.mapper;

import com.distri.proyectodistri.apis.entities.cliente.Cliente;
import com.distri.proyectodistri.dto.ClienteDTO;
import org.mapstruct.Mapper;

@Mapper(config = MapperCentralConfig.class)
public interface ClienteMapper extends BaseMapper<Cliente, ClienteDTO> {}
