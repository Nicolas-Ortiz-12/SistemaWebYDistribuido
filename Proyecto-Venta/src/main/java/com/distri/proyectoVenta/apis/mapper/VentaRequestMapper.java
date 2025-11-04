package com.distri.proyectoVenta.apis.mapper;


import com.distri.proyectoVenta.apis.entities.venta.Venta;
import com.distri.proyectoVenta.apis.mapper.MapperCentralConfig;
import com.distri.proyectoVenta.dto.VentaRequestDTO;
import org.mapstruct.*;

@Mapper(config = MapperCentralConfig.class)
public interface VentaRequestMapper {

    //@Mapping(target = "cliente", ignore = true)
   //@Mapping(target = "creadoPor", ignore = true)
    //@Mapping(target = "detalles", ignore = true) // los detalles se crean en el service
    //@Mapping(target = "subtotal", ignore = true)
    //@Mapping(target = "iva", ignore = true)
    //@Mapping(target = "total", ignore = true)
    //@Mapping(target = "fechaVenta", qualifiedByName = "asLocalDateTime")
    Venta toEntity(VentaRequestDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    //@Mapping(target = "fechaVenta", qualifiedByName = "asLocalDateTime")
    void updateEntityFromRequest(VentaRequestDTO dto, @MappingTarget Venta entity);
}
