package com.distri.proyectoVenta.apis.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// Traduce las excepciones de negocio a respuestas JSON {error, message} (el front lee "message").
// Sin esto, las validaciones de los services (IllegalArgument/IllegalState) salían como 500.
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        return body(HttpStatus.BAD_REQUEST, "bad_request", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException ex) {
        return body(HttpStatus.CONFLICT, "conflict", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> integrity(DataIntegrityViolationException ex) {
        // El detalle de la BD no se devuelve al cliente (expone nombres de tablas/constraints)
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return body(HttpStatus.CONFLICT, "conflict", "La operación viola una restricción de datos (duplicado o referencia inválida)");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> status(ResponseStatusException ex) {
        HttpStatusCode code = ex.getStatusCode();
        String error = code instanceof HttpStatus hs ? hs.name().toLowerCase() : String.valueOf(code.value());
        return body(code, error, ex.getReason());
    }

    private ResponseEntity<Map<String, String>> body(HttpStatusCode status, String error, String message) {
        return ResponseEntity.status(status).body(Map.of("error", error, "message", message == null ? "" : message));
    }
}
