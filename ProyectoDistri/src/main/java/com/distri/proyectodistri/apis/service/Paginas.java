package com.distri.proyectodistri.apis.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

// Valida los parámetros de paginación que llegan por la URL: sin tope, un "size" enorme
// trae la tabla entera en una sola request.
public final class Paginas {
    public static final int MAX_SIZE = 200;

    private Paginas() {}

    public static PageRequest of(int page, int size) {
        return of(page, size, Sort.unsorted());
    }

    public static PageRequest of(int page, int size, Sort sort) {
        if (page < 0) throw new IllegalArgumentException("page debe ser >= 0");
        if (size < 1 || size > MAX_SIZE) throw new IllegalArgumentException("size debe estar entre 1 y " + MAX_SIZE);
        return PageRequest.of(page, size, sort);
    }
}
