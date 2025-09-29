// src/main/java/com/distri/proyectodistri/mapper/MapperCentralConfig.java
package com.distri.proyectodistri.apis.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
        componentModel = "spring",                // para @Autowired o constructor injection
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface MapperCentralConfig {}
