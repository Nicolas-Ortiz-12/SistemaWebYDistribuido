package com.distri.proyectodistri.apis.mapper;
import com.distri.proyectodistri.apis.entities.cliente.Proveedor;
import com.distri.proyectodistri.apis.entities.inventario.Categoria;
import com.distri.proyectodistri.apis.entities.seguridad.Rol;
import com.distri.proyectodistri.dto.CategoriaDTO;
import com.distri.proyectodistri.dto.ProveedorDTO;
import com.distri.proyectodistri.dto.RolDTO;
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
