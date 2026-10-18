package com.example.productoscrud;

import com.example.productoscrud.domain.model.Product;
import com.example.productoscrud.infrastructure.persistence.mysql.ProductRepositoryMySQL;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Ejecuta el SQL del adaptador en H2, sin necesitar un servidor MySQL. */
class ProductRepositoryMySQLTest {
    @Test void crudSqlConIdsGeneradosYDecimales() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
            + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE products (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, "
            + "price DECIMAL(12,2) NOT NULL CHECK(price >= 0), description VARCHAR(255) NOT NULL)");
        var repository = new ProductRepositoryMySQL(jdbc);
        assertTrue(repository.listar().isEmpty());
        var created = repository.crear(new Product(999, "Mouse", new BigDecimal("0.10"), "USB"));
        assertEquals(1, created.getId());
        assertEquals(new BigDecimal("0.10"), repository.buscarPorId(1).orElseThrow().getPrice());
        assertEquals(1, repository.listar().size());
        assertTrue(repository.actualizar(1, new Product(null, "Mouse HD", new BigDecimal("25.15"), "Actualizado")));
        assertEquals("Mouse HD", repository.buscarPorId(1).orElseThrow().getName());
        assertFalse(repository.actualizar(99, created));
        assertTrue(repository.eliminar(1));
        assertFalse(repository.eliminar(1));
        assertTrue(repository.buscarPorId(1).isEmpty());
    }
}
