package com.example.productoscrud;

import com.example.productoscrud.domain.model.Product;
import com.example.productoscrud.infrastructure.persistence.mongodb.ProductRepositoryMongoDB;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** Solo se ejecuta con -Pintegration. Cada caso usa y borra SU base temporal. */
class MongoPersistenceIT {
    private MongoClient client;
    private MongoTemplate mongo;
    private ProductRepositoryMongoDB repository;

    @BeforeEach void setup() {
        String uri = System.getenv("MONGO_TEST_URI");
        assertNotNull(uri, "Define MONGO_TEST_URI para las pruebas de integración");
        client = MongoClients.create(uri);
        mongo = new MongoTemplate(client, "productoscrud_test_" + UUID.randomUUID().toString().replace("-", ""));
        mongo.getDb().runCommand(new Document("ping", 1));
        repository = new ProductRepositoryMongoDB(mongo);
    }

    @AfterEach void cleanup() {
        if (mongo != null) mongo.getDb().drop();
        if (client != null) client.close();
    }

    private Product crear(String name, String price) {
        return repository.crear(new Product(999, name, new BigDecimal(price), "Prueba"));
    }

    @Test void crudRealConActualizacionSinCambiosYPreciosDecimal128() {
        assertTrue(repository.listar().isEmpty());
        var p = crear("Mouse", "0.10");
        assertEquals(1, p.getId());
        assertEquals(new BigDecimal("0.10"), repository.buscarPorId(p.getId()).orElseThrow().getPrice());
        assertInstanceOf(Decimal128.class, mongo.getCollection("products").find().first().get("price"));
        assertTrue(repository.actualizar(p.getId(), p));
        assertTrue(repository.actualizar(p.getId(), new Product(null, "Teclado", new BigDecimal("5.25"), "Nuevo")));
        assertEquals("Teclado", repository.listar().getFirst().getName());
        assertFalse(repository.actualizar(999, p));
        assertTrue(repository.eliminar(p.getId()));
        assertFalse(repository.eliminar(p.getId()));
        assertTrue(repository.buscarPorId(p.getId()).isEmpty());
    }

    @Test void cuatroPipelinesRealesYRepresentacionReejecutable() {
        assertEquals(0, repository.estadisticas().resultados().getFirst().cantidad());
        assertNull(repository.estadisticas().resultados().getFirst().precioMinimo());
        crear("Mouse [USB]", "0.10");
        crear("MOUSE HD", "0.20");
        crear("Teclado", "0.30");
        assertEquals(2, repository.buscarPorNombre("mouse").resultados().size());
        assertEquals(1, repository.buscarPorNombre("[USB]").resultados().size());
        assertEquals(0, repository.buscarPorNombre(".*").resultados().size());
        var range = repository.filtrarPorPrecios(new BigDecimal("0.10"), new BigDecimal("0.20"));
        assertEquals(2, range.resultados().size());
        var expensive = repository.masCostosos(2);
        assertEquals("Teclado", expensive.resultados().getFirst().getName());
        assertEquals(2, expensive.resultados().size());
        var stats = repository.estadisticas().resultados().getFirst();
        assertEquals(3, stats.cantidad());
        assertEquals(0, new BigDecimal("0.20").compareTo(stats.precioPromedio()));
        assertEquals(0, new BigDecimal("0.10").compareTo(stats.precioMinimo()));
        assertEquals(0, new BigDecimal("0.30").compareTo(stats.precioMaximo()));
        var mapper = new JsonMapper();
        for (var response : List.of(repository.buscarPorNombre("mouse"), range, expensive, repository.estadisticas())) {
            var stages = response.pipeline().stream().map(stage -> Document.parse(mapper.writeValueAsString(stage))).toList();
            assertEquals(response.resultados().size(), mongo.getCollection("products").aggregate(stages).into(new java.util.ArrayList<>()).size());
        }
    }

    @Test void idsSonUnicosBajoCreacionConcurrente() throws Exception {
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<Callable<Integer>> tasks = IntStream.range(0, 40)
                .mapToObj(i -> (Callable<Integer>) () -> crear("P" + i, "0.15").getId()).toList();
            var ids = new HashSet<Integer>();
            for (var future : executor.invokeAll(tasks)) assertTrue(ids.add(future.get()));
            assertEquals(40, ids.size());
            assertEquals(40, repository.listar().size());
        }
    }

    @Test void datosSqlYMongodbPermanecenSeparadosConElMismoId() {
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE products (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), price DECIMAL(12,2), description VARCHAR(255))");
        var sql = new com.example.productoscrud.infrastructure.persistence.mysql.ProductRepositoryMySQL(jdbc);
        var sqlProduct = sql.crear(new Product(null, "Solo SQL", BigDecimal.ONE, ""));
        var mongoProduct = crear("Solo Mongo", "2.00");
        assertEquals(sqlProduct.getId(), mongoProduct.getId());
        assertEquals("Solo SQL", sql.buscarPorId(1).orElseThrow().getName());
        assertEquals("Solo Mongo", repository.buscarPorId(1).orElseThrow().getName());
        repository.eliminar(1);
        assertEquals(1, sql.listar().size());
        assertTrue(repository.listar().isEmpty());
    }

    @Test void respetaIdsImportadosSinContadorPrevio() {
        mongo.getCollection("products").insertOne(new Document("_id", 1).append("name", "Importado")
            .append("price", new Decimal128(BigDecimal.ONE)).append("description", ""));
        assertEquals(2, crear("Nuevo", "2.00").getId());
        assertEquals("Importado", repository.buscarPorId(1).orElseThrow().getName());
    }
}
