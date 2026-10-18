package com.example.productoscrud.application.ports;

import com.example.productoscrud.application.query.AggregationResult;
import com.example.productoscrud.application.query.ProductStatistics;
import com.example.productoscrud.domain.model.Product;
import java.math.BigDecimal;

/** Puerto de consultas: no expone tipos de MongoDB a los casos de uso. */
public interface ProductAggregationRepository {
    AggregationResult<Product> buscarPorNombre(String texto);
    AggregationResult<Product> filtrarPorPrecios(BigDecimal minimo, BigDecimal maximo);
    AggregationResult<Product> masCostosos(int limite);
    AggregationResult<ProductStatistics> estadisticas();
}
