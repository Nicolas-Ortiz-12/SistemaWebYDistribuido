package com.distri.proyectoVenta.apis.mapper;

import com.distri.proyectoVenta.apis.entities.venta.Venta;
import com.distri.proyectoVenta.apis.mapper.MapperCentralConfig;
import com.distri.proyectoVenta.dto.VentaDTO;
import org.mapstruct.*;

@Mapper(config = MapperCentralConfig.class)
public interface VentaMapper {
    //@Mapping(target = "cliente", ignore = true)
    //@Mapping(target = "fechaVenta", qualifiedByName = "asLocalDateTime")
    Venta toEntity(VentaDTO dto);

    //@Mapping(target = "clienteId", source = "cliente.id")
    //@Mapping(target = "fechaVenta", qualifiedByName = "asOffsetDateTime")
    VentaDTO toDto(Venta entity);


    //@Mapping(target = "fechaVenta", qualifiedByName = "asLocalDateTime")
    void updateEntityFromDto(VentaDTO dto, @MappingTarget Venta entity);
}

