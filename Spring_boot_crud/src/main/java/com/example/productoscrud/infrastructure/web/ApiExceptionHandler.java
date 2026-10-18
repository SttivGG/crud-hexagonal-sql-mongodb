package com.example.productoscrud.infrastructure.web;

import com.mongodb.MongoException;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalid(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Map.of("message", error.getMessage()));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
                       HttpMessageNotReadableException.class})
    public ResponseEntity<?> malformed(Exception error) {
        return ResponseEntity.badRequest().body(Map.of("message", "Parámetros o JSON inválidos; revisa los campos enviados"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> integrity(DataIntegrityViolationException error) {
        return ResponseEntity.status(409).body(Map.of("message", "Los datos no cumplen las restricciones de la persistencia"));
    }

    @ExceptionHandler({DataAccessException.class, MongoException.class})
    public ResponseEntity<?> unavailable(Exception error) {
        return ResponseEntity.status(503).body(Map.of("message", "No se pudo completar la operación en la base de datos seleccionada"));
    }
}
