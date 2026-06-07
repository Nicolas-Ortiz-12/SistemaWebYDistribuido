// src/main/java/com/distri/proyectodistri/apis/mapper/CompraRequestMapper.java
package com.distri.proyectodistri.apis.mapper;

import com.distri.proyectodistri.apis.entities.compra.Compra;
import com.distri.proyectodistri.dto.CompraRequestDTO;
import org.mapstruct.*;

@Mapper(config = MapperCentralConfig.class)
public interface CompraRequestMapper {

    @Mapping(target = "proveedor", ignore = true)
    @Mapping(target = "detalles", ignore = true) // detalles se arman en el service
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "iva", ignore = true)
    @Mapping(target = "total", ignore = true)
    Compra toEntity(CompraRequestDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(CompraRequestDTO dto, @MappingTarget Compra entity);
}
