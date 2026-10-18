package com.example.productoscrud.infrastructure.web.controllers;

import com.example.productoscrud.application.query.AggregationResult;
import com.example.productoscrud.application.query.ProductStatistics;
import com.example.productoscrud.application.service.ProductQueryService;
import com.example.productoscrud.domain.model.Product;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mongodb/productos/filtros")
public class ProductQueryController {
    private final ProductQueryService service;

    public ProductQueryController(ProductQueryService service) { this.service = service; }

    @GetMapping("/nombre")
    public AggregationResult<Product> nombre(@RequestParam String texto) {
        return service.buscarPorNombre(texto);
    }

    @GetMapping("/precios")
    public AggregationResult<Product> precios(@RequestParam BigDecimal minimo, @RequestParam BigDecimal maximo) {
        return service.filtrarPorPrecios(minimo, maximo);
    }

    @GetMapping("/costosos")
    public AggregationResult<Product> costosos(@RequestParam(defaultValue = "5") int limite) {
        return service.masCostosos(limite);
    }

    @GetMapping("/estadisticas")
    public AggregationResult<ProductStatistics> estadisticas() { return service.estadisticas(); }
}
