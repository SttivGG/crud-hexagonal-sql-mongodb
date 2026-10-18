package com.example.productoscrud.application.service;

import com.example.productoscrud.application.ports.ProductAggregationRepository;
import com.example.productoscrud.application.query.AggregationResult;
import com.example.productoscrud.application.query.ProductStatistics;
import com.example.productoscrud.domain.model.Product;
import java.math.BigDecimal;

public class ProductQueryService {
    private final ProductAggregationRepository repository;

    public ProductQueryService(ProductAggregationRepository repository) {
        this.repository = repository;
    }

    public AggregationResult<Product> buscarPorNombre(String texto) {
        if (texto == null || texto.isBlank() || texto.length() > 100) {
            throw new IllegalArgumentException("El texto debe tener entre 1 y 100 caracteres");
        }
        return repository.buscarPorNombre(texto.trim());
    }

    public AggregationResult<Product> filtrarPorPrecios(BigDecimal minimo, BigDecimal maximo) {
        if (minimo == null || maximo == null || minimo.signum() < 0 || minimo.compareTo(maximo) > 0) {
            throw new IllegalArgumentException("Los precios deben cumplir 0 <= mínimo <= máximo");
        }
        return repository.filtrarPorPrecios(minimo, maximo);
    }

    public AggregationResult<Product> masCostosos(int limite) {
        if (limite < 1 || limite > 100) {
            throw new IllegalArgumentException("El límite debe estar entre 1 y 100");
        }
        return repository.masCostosos(limite);
    }

    public AggregationResult<ProductStatistics> estadisticas() {
        return repository.estadisticas();
    }
}
