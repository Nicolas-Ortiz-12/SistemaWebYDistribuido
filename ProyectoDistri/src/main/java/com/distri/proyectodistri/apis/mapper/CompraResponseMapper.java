// src/main/java/com/distri/proyectodistri/apis/mapper/CompraResponseMapper.java
package com.distri.proyectodistri.apis.mapper;

import com.distri.proyectodistri.apis.entities.compra.Compra;
import com.distri.proyectodistri.apis.entities.compra.CompraDetalle;
import com.distri.proyectodistri.dto.CompraDetalleResponseDTO;
import com.distri.proyectodistri.dto.CompraResponseDTO;
import org.mapstruct.*;

@Mapper(config = MapperCentralConfig.class)
public interface CompraResponseMapper {

    @Mapping(target = "proveedorId", source = "proveedor.id")
    @Mapping(target = "detalles", source = "detalles")
    @Mapping(target = "fechaEmision", qualifiedByName = "asLocalDate")
    @Mapping(target = "creadoEn", source = "creadoEn", qualifiedByName = "asOffsetDateTime")
    CompraResponseDTO toDto(Compra entity);

    @Mapping(target = "productoId", source = "producto.id")

    CompraDetalleResponseDTO toDetalleDto(CompraDetalle entity);
}
