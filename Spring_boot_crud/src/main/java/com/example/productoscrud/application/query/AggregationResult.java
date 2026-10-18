package com.example.productoscrud.application.query;

import java.util.List;
import java.util.Map;

/** pipeline contiene Extended JSON, incluidos los valores $numberDecimal. */
public record AggregationResult<T>(String filtro, List<T> resultados, List<Map<String, Object>> pipeline) {}
