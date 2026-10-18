package com.example.productoscrud.application.query;

import java.math.BigDecimal;

public record ProductStatistics(long cantidad, BigDecimal precioPromedio,
                                BigDecimal precioMinimo, BigDecimal precioMaximo) {}
