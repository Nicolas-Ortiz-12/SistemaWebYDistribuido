package com.distri.proyectodistri.apis.mapper;


import com.distri.proyectodistri.apis.entities.inventario.Categoria;
import com.distri.proyectodistri.dto.CategoriaDTO;
import org.mapstruct.*;

@Mapper(config = MapperCentralConfig.class)
public interface CategoriaMapper extends  BaseMapper<Categoria, CategoriaDTO> {

    Categoria toEntity(CategoriaDTO dto);

    @Override
    //@Mapping(source = "nombre", target = "nombre")
    CategoriaDTO toDto(Categoria entity);


    @Override
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(CategoriaDTO dto, @MappingTarget Categoria entity);
}
