// src/main/java/com/distri/proyectodistri/apis/mapper/VentaResponseMapper.java
package com.distri.proyectoVenta.apis.mapper;

import com.distri.proyectoVenta.apis.entities.venta.Venta;
import com.distri.proyectoVenta.apis.entities.venta.VentaDetalle;
import com.distri.proyectoVenta.dto.VentaDetalleResponseDTO;
import com.distri.proyectoVenta.dto.VentaResponseDTO;
import org.mapstruct.*;

@Mapper(config = MapperCentralConfig.class)
public interface VentaResponseMapper {

    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "deudorId", source = "deudor.id")
    @Mapping(target = "creadoPorId", source = "creadoPor.id")
    @Mapping(target = "items", source = "detalles")
    @Mapping(target = "fechaVenta", qualifiedByName = "asOffsetDateTime")
    VentaResponseDTO toDto(Venta entity);

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "cantidad", source = "cantidad")
    @Mapping(target = "tasaIva", source = "tasaIva")
    @Mapping(target = "subtotalLinea", source = "subtotalLinea")
    @Mapping(target = "totalLinea", source = "totalLinea")
    VentaDetalleResponseDTO toDetalleDto(VentaDetalle entity);
}
