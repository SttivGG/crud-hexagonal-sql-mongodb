package com.example.productoscrud;

import com.example.productoscrud.application.ports.ProductAggregationRepository;
import com.example.productoscrud.application.service.ProductQueryService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductQueryServiceTest {
    private final ProductAggregationRepository repository = mock(ProductAggregationRepository.class);
    private final ProductQueryService service = new ProductQueryService(repository);

    @Test void rechazaParametrosInvalidosSinConsultar() {
        for (String text : new String[] {null, "", "  ", "a".repeat(101)}) {
            assertThrows(IllegalArgumentException.class, () -> service.buscarPorNombre(text));
        }
        assertThrows(IllegalArgumentException.class, () -> service.filtrarPorPrecios(BigDecimal.TEN, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> service.filtrarPorPrecios(new BigDecimal("-1"), BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> service.filtrarPorPrecios(null, BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class, () -> service.masCostosos(0));
        assertThrows(IllegalArgumentException.class, () -> service.masCostosos(101));
        verifyNoInteractions(repository);
    }

    @Test void delegaLosCuatroCasosDeUso() {
        service.buscarPorNombre(" Mouse ");
        service.filtrarPorPrecios(BigDecimal.ZERO, BigDecimal.TEN);
        service.masCostosos(100);
        service.estadisticas();
        verify(repository).buscarPorNombre("Mouse");
        verify(repository).filtrarPorPrecios(BigDecimal.ZERO, BigDecimal.TEN);
        verify(repository).masCostosos(100);
        verify(repository).estadisticas();
    }
}
