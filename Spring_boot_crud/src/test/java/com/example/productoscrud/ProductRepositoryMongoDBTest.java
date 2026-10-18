package com.example.productoscrud;

import com.example.productoscrud.domain.model.Product;
import com.example.productoscrud.infrastructure.persistence.mongodb.ProductRepositoryMongoDB;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductRepositoryMongoDBTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final ProductRepositoryMongoDB repository = new ProductRepositoryMongoDB(mongo);

    private Document documento() {
        return new Document("_id", 7).append("name", "Mouse").append("price", new Decimal128(new BigDecimal("0.10")))
            .append("description", "USB");
    }

    @Test void creaConContadorAtomicoYDecimalSinAceptarElIdDelCliente() {
        when(mongo.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Document.class), eq("counters")))
            .thenReturn(new Document("seq", 7L));
        var product = repository.crear(new Product(999, "Mouse", new BigDecimal("0.10"), "USB"));
        assertEquals(7, product.getId());
        assertEquals(new BigDecimal("0.10"), product.getPrice());
        var query = ArgumentCaptor.forClass(Query.class);
        var update = ArgumentCaptor.forClass(Update.class);
        var options = ArgumentCaptor.forClass(FindAndModifyOptions.class);
        verify(mongo).findAndModify(query.capture(), update.capture(), options.capture(), eq(Document.class), eq("counters"));
        assertEquals(new Document("_id", "products"), query.getValue().getQueryObject());
        assertEquals(new Document("$inc", new Document("seq", 1L)), update.getValue().getUpdateObject());
        assertTrue(options.getValue().isUpsert());
        assertTrue(options.getValue().isReturnNew());
        verify(mongo).insert(documento(), "products");
    }

    @Test void reintentaLaInicializacionConcurrenteDelContador() {
        when(mongo.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Document.class), eq("counters")))
            .thenThrow(new DuplicateKeyException("carrera")).thenReturn(new Document("seq", 2L));
        assertEquals(2, repository.crear(new Product(null, "A", BigDecimal.ONE, "")).getId());
    }

    @Test void saltaIdsImportadosSinSobrescribirDocumentos() {
        when(mongo.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Document.class), eq("counters")))
            .thenReturn(new Document("seq", 1L), new Document("seq", 2L));
        when(mongo.insert(any(Document.class), eq("products")))
            .thenThrow(new DuplicateKeyException("ID importado")).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals(2, repository.crear(new Product(null, "Nuevo", BigDecimal.ONE, "")).getId());
    }

    @Test void rechazaDesbordamientoDeIdYDecimalesNoRepresentables() {
        when(mongo.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(Document.class), eq("counters")))
            .thenReturn(new Document("seq", 2147483648L));
        assertThrows(IllegalStateException.class, () -> repository.crear(new Product(null, "A", BigDecimal.ONE, "")));
        assertThrows(IllegalArgumentException.class, () -> repository.crear(new Product(null, "A", new BigDecimal("1E+7000"), "")));
        verify(mongo, never()).insert(any(Document.class), eq("products"));
    }

    @Test void listaBuscaActualizaSinCambiosYElimina() {
        when(mongo.find(any(Query.class), eq(Document.class), eq("products"))).thenReturn(List.of(documento()));
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("products"))).thenReturn(documento(), (Document) null);
        assertEquals(7, repository.listar().getFirst().getId());
        assertEquals("Mouse", repository.buscarPorId(7).orElseThrow().getName());
        assertTrue(repository.buscarPorId(8).isEmpty());
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq("products")))
            .thenReturn(UpdateResult.acknowledged(1, 0L, null), UpdateResult.acknowledged(0, 0L, null));
        var product = new Product(null, "Mouse", new BigDecimal("0.10"), "USB");
        assertTrue(repository.actualizar(7, product));
        assertFalse(repository.actualizar(8, product));
        when(mongo.remove(any(Query.class), eq("products"))).thenReturn(DeleteResult.acknowledged(1), DeleteResult.acknowledged(0));
        assertTrue(repository.eliminar(7));
        assertFalse(repository.eliminar(8));
    }

    @Test void ejecutaYDevuelveExactamenteLasMismasEtapasEnLosCuatroPipelines() {
        when(mongo.aggregate(any(Aggregation.class), eq("products"), eq(Document.class)))
            .thenReturn(new AggregationResults<>(List.of(), new Document()));
        var nombre = repository.buscarPorNombre("a.*[x]\\E");
        var precios = repository.filtrarPorPrecios(new BigDecimal("0.10"), new BigDecimal("20.25"));
        var costosos = repository.masCostosos(5);
        var estadisticas = repository.estadisticas();
        var captor = ArgumentCaptor.forClass(Aggregation.class);
        verify(mongo, times(4)).aggregate(captor.capture(), eq("products"), eq(Document.class));
        var responses = List.of(nombre, precios, costosos, estadisticas);
        var mapper = new JsonMapper();
        for (int i = 0; i < responses.size(); i++) {
            var expected = responses.get(i).pipeline().stream()
                .map(stage -> Document.parse(mapper.writeValueAsString(stage))).toList();
            var actual = captor.getAllValues().get(i).toPipeline(Aggregation.DEFAULT_CONTEXT).stream()
                .map(stage -> Document.parse(stage.toJson())).toList();
            assertEquals(expected, actual);
        }
        var match = (Map<?, ?>) nombre.pipeline().getFirst().get("$match");
        var regex = (String) ((Map<?, ?>) match.get("name")).get("$regex");
        assertTrue(java.util.regex.Pattern.compile(regex).matcher("A a.*[x]\\E B").find());
        var priceMatch = (Map<?, ?>) precios.pipeline().getFirst().get("$match");
        assertEquals(Map.of("$numberDecimal", "0.10"), ((Map<?, ?>) priceMatch.get("price")).get("$gte"));
        assertEquals(5, costosos.pipeline().get(1).get("$limit"));
        assertEquals(0, estadisticas.resultados().getFirst().cantidad());
        assertNull(estadisticas.resultados().getFirst().precioPromedio());
    }

    @Test void convierteProductosYEstadisticasDeLaAgregacion() {
        var stats = new Document("cantidad", 2).append("precioPromedio", new Decimal128(new BigDecimal("0.15")))
            .append("precioMinimo", new Decimal128(new BigDecimal("0.10")))
            .append("precioMaximo", new Decimal128(new BigDecimal("0.20")));
        when(mongo.aggregate(any(Aggregation.class), eq("products"), eq(Document.class)))
            .thenReturn(new AggregationResults<>(List.of(documento()), new Document()))
            .thenReturn(new AggregationResults<>(List.of(stats), new Document()));
        assertEquals(new BigDecimal("0.10"), repository.masCostosos(1).resultados().getFirst().getPrice());
        assertEquals(new BigDecimal("0.15"), repository.estadisticas().resultados().getFirst().precioPromedio());
    }
}
