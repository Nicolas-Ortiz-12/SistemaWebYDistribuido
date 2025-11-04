// src/main/java/com/distri/proyectodistri/apis/mapper/ProductoMapper.java
package com.distri.proyectodistri.apis.mapper;

import com.distri.proyectodistri.apis.entities.inventario.Producto;
import com.distri.proyectodistri.apis.entities.seguridad.Rol;
import com.distri.proyectodistri.dto.ProductoDTO;
import com.distri.proyectodistri.dto.RolDTO;

import org.mapstruct.*;

@Mapper(config = MapperCentralConfig.class, uses = { NumberMapper.class })
public interface ProductoMapper extends BaseMapper<Producto, ProductoDTO> {


    Producto toEntity(ProductoDTO dto);

    @Override
    //@Mapping(source = "nombre", target = "nombre")
    ProductoDTO toDto(Producto entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(ProductoDTO dto, @MappingTarget Producto entity);
}
