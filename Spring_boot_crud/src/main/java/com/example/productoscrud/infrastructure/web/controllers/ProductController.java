package com.example.productoscrud.infrastructure.web.controllers;

import com.example.productoscrud.application.service.ProductService;
import com.example.productoscrud.domain.model.Product;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/productos", "/api/{database}/productos"})
public class ProductController {
    private final Map<String, ProductService> services;

    public ProductController(@Qualifier("mysqlProductService") ProductService mysql,
                             @Qualifier("mongoProductService") ProductService mongodb) {
        this.services = Map.of("mysql", mysql, "mongodb", mongodb);
    }

    private ProductService servicio(String database) {
        ProductService service = services.get(database == null ? "mysql" : database);
        if (service == null) throw new IllegalArgumentException("Persistencia inválida: usa mysql o mongodb");
        return service;
    }

    @GetMapping({"", "/"})
    public List<Product> listar(@PathVariable(required = false) String database) {
        return servicio(database).listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> buscarPorId(@PathVariable(required = false) String database, @PathVariable int id) {
        return servicio(database).buscarPorId(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping({"", "/"})
    public ResponseEntity<?> crear(@PathVariable(required = false) String database, @RequestBody Product product) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio(database).crear(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable(required = false) String database, @PathVariable int id, @RequestBody Product product) {
        return servicio(database).actualizar(id, product)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable(required = false) String database, @PathVariable int id) {
        return servicio(database).eliminar(id)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }
}

// Origen: Spring MVC (org.springframework.web.servlet) invoca los métodos de ProductController
// (com.example.productoscrud.infrastructure.web.controllers) según la petición HTTP.
// Destino: ProductController llama a ProductService (com.example.productoscrud.application.service)
// y recibe o devuelve Product (com.example.productoscrud.domain.model) como JSON.
