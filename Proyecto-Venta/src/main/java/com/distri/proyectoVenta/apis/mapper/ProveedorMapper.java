package com.distri.proyectoVenta.apis.mapper;
import com.distri.proyectoVenta.apis.entities.cliente.Proveedor;
import com.distri.proyectoVenta.apis.entities.inventario.Categoria;
import com.distri.proyectoVenta.apis.entities.seguridad.Rol;
import com.distri.proyectoVenta.apis.mapper.MapperCentralConfig;
import com.distri.proyectoVenta.dto.CategoriaDTO;
import com.distri.proyectoVenta.dto.ProveedorDTO;
import com.distri.proyectoVenta.dto.RolDTO;
import org.mapstruct.*;

@Mapper(config = MapperCentralConfig.class)
public interface ProveedorMapper extends BaseMapper<Proveedor, ProveedorDTO> {
    @Override
    Proveedor toEntity(ProveedorDTO dto);

    @Override
    //@Mapping(source = "nombre", target = "nombre")
    ProveedorDTO toDto(Proveedor entity);

    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(ProveedorDTO dto, @MappingTarget Proveedor entity);
}
