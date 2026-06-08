// src/main/java/com/distri/proyectodistri/apis/mapper/ProductoMapper.java
package com.distri.proyectoVenta.apis.mapper;

import com.distri.proyectoVenta.apis.entities.inventario.Producto;
import com.distri.proyectoVenta.apis.entities.seguridad.Rol;
import com.distri.proyectoVenta.apis.mapper.MapperCentralConfig;
import com.distri.proyectoVenta.dto.ProductoDTO;
import com.distri.proyectoVenta.dto.RolDTO;

import org.mapstruct.*;

@Mapper(config = MapperCentralConfig.class, uses = { NumberMapper.class })
public interface ProductoMapper extends BaseMapper<Producto, ProductoDTO> {


    Producto toEntity(ProductoDTO dto);

    @Override
    @Mapping(source = "nombre", target = "nombre")
    ProductoDTO toDto(Producto entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(ProductoDTO dto, @MappingTarget Producto entity);
}
