package com.example.productoscrud;

import com.example.productoscrud.application.ports.ProductRepository;
import com.example.productoscrud.application.service.ProductService;
import com.example.productoscrud.domain.model.Product;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTest {
    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductService service = new ProductService(repository);

    @Test void validaProductosAntesDeAccederALaPersistencia() {
        for (Product invalid : new Product[] {
            new Product(null, null, BigDecimal.ZERO, ""), new Product(null, "  ", BigDecimal.ZERO, ""),
            new Product(null, "a".repeat(101), BigDecimal.ZERO, ""), new Product(null, "A", null, ""),
            new Product(null, "A", new BigDecimal("-0.01"), ""), new Product(null, "A", BigDecimal.ONE, null),
            new Product(null, "A", BigDecimal.ONE, "a".repeat(256))
        }) {
            assertFalse(invalid.esValido());
            assertThrows(IllegalArgumentException.class, () -> service.crear(invalid));
            assertThrows(IllegalArgumentException.class, () -> service.actualizar(1, invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> service.crear(null));
        verifyNoInteractions(repository);
    }

    @Test void aceptaLosLimitesDelDominio() {
        Product valid = new Product(null, "a".repeat(100), BigDecimal.ZERO, "a".repeat(255));
        assertTrue(valid.esValido());
        when(repository.crear(valid)).thenReturn(valid);
        assertSame(valid, service.crear(valid));
    }

    @Test void validaIdsYDevuelveAusencias() {
        assertThrows(IllegalArgumentException.class, () -> service.buscarPorId(0));
        assertThrows(IllegalArgumentException.class, () -> service.eliminar(-1));
        assertThrows(IllegalArgumentException.class, () -> service.actualizar(0, new Product()));
        when(repository.buscarPorId(8)).thenReturn(Optional.empty());
        assertTrue(service.buscarPorId(8).isEmpty());
        Product valid = new Product(null, "A", BigDecimal.ONE, "");
        assertTrue(service.actualizar(8, valid).isEmpty());
        assertFalse(service.eliminar(8));
    }
}
