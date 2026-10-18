package com.example.productoscrud;

import com.example.productoscrud.domain.model.Product;
import com.example.productoscrud.infrastructure.persistence.mysql.ProductRepositoryMySQL;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

/** Requiere permiso CREATE/DROP DATABASE; solo elimina el esquema que acaba de crear. */
@EnabledIfEnvironmentVariable(named = "MYSQL_TEST_URL", matches = ".+")
class MySQLPersistenceIT {
    @Test void crudEnServidorMySqlConEsquemaTemporal() {
        String url = System.getenv("MYSQL_TEST_URL");
        assertTrue(url.matches("jdbc:mysql://[^/]+/(\\?.*)?"), "MYSQL_TEST_URL debe apuntar al servidor SIN base de datos");
        String user = System.getenv().getOrDefault("MYSQL_TEST_USER", "root");
        String password = System.getenv().getOrDefault("MYSQL_TEST_PASSWORD", "");
        var admin = new JdbcTemplate(new DriverManagerDataSource(url, user, password));
        String database = "productoscrud_test_" + UUID.randomUUID().toString().replace("-", "");
        admin.execute("CREATE DATABASE " + database);
        try {
            int options = url.indexOf('?');
            String dbUrl = options < 0 ? url + database : url.substring(0, options) + database + url.substring(options);
            var jdbc = new JdbcTemplate(new DriverManagerDataSource(dbUrl, user, password));
            jdbc.execute("CREATE TABLE products (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, "
                + "price DECIMAL(12,2) NOT NULL CHECK(price >= 0), description VARCHAR(255) NOT NULL DEFAULT '')");
            var repository = new ProductRepositoryMySQL(jdbc);
            assertTrue(repository.listar().isEmpty());
            var p = repository.crear(new Product(null, "Prueba SQL", new BigDecimal("0.10"), "Temporal"));
            assertEquals(1, p.getId());
            assertEquals(new BigDecimal("0.10"), repository.buscarPorId(p.getId()).orElseThrow().getPrice());
            assertTrue(repository.actualizar(p.getId(), new Product(null, "Actualizado", new BigDecimal("0.20"), "")));
            assertEquals("Actualizado", repository.listar().getFirst().getName());
            assertTrue(repository.eliminar(p.getId()));
            assertTrue(repository.buscarPorId(p.getId()).isEmpty());
            assertFalse(repository.eliminar(p.getId()));
        } finally {
            admin.execute("DROP DATABASE " + database);
        }
    }
}
