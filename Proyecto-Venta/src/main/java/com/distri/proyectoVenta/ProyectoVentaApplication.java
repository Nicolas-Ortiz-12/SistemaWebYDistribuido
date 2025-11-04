package com.distri.proyectoVenta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ProyectoVentaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProyectoVentaApplication.class, args);
    }

}
