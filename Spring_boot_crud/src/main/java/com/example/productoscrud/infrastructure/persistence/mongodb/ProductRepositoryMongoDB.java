package com.example.productoscrud.infrastructure.persistence.mongodb;

import com.example.productoscrud.application.ports.ProductRepository;
import com.example.productoscrud.application.ports.ProductAggregationRepository;
import com.example.productoscrud.application.query.AggregationResult;
import com.example.productoscrud.application.query.ProductStatistics;
import com.example.productoscrud.domain.model.Product;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/** BSON y conversiones monetarias quedan exclusivamente en este adaptador. */
@Repository
public class ProductRepositoryMongoDB extends ProductRepository implements ProductAggregationRepository {
    private static final String COLLECTION = "products";
    private final MongoTemplate mongo;

    public ProductRepositoryMongoDB(MongoTemplate mongo) { this.mongo = mongo; }

    @Override
    public List<Product> listar() {
        return mongo.find(new Query().with(Sort.by("_id")), Document.class, COLLECTION)
            .stream().map(this::producto).toList();
    }

    @Override
    public Optional<Product> buscarPorId(int id) {
        return Optional.ofNullable(mongo.findOne(porId(id), Document.class, COLLECTION)).map(this::producto);
    }

    @Override
    public Product crear(Product product) {
        // Validar el decimal antes de reservar un ID. Nunca convertir dinero a double.
        Decimal128 price = decimal(product.getPrice());
        for (int attempt = 0; attempt < 100; attempt++) {
            int id = siguienteId();
            Document document = new Document("_id", id).append("name", product.getName())
                .append("price", price).append("description", product.getDescription());
            try {
                mongo.insert(document, COLLECTION);
                return producto(document);
            } catch (DuplicateKeyException importedId) {
                // Un documento importado manualmente puede tener un ID que aún no conoce el contador.
                // Reservar otro ID atómico; nunca sobrescribir el documento ni consultar MAX(id).
            }
        }
        throw new IllegalStateException("Sincroniza counters.products antes de importar más IDs manuales");
    }

    private int siguienteId() {
        Query query = Query.query(Criteria.where("_id").is(COLLECTION));
        Update update = new Update().inc("seq", 1L);
        var options = FindAndModifyOptions.options().upsert(true).returnNew(true);
        Document counter;
        try {
            counter = mongo.findAndModify(query, update, options, Document.class, "counters");
        } catch (DuplicateKeyException concurrentInitialization) {
            // Otra petición pudo insertar el contador entre la búsqueda y el upsert inicial.
            counter = mongo.findAndModify(query, update, options, Document.class, "counters");
        }
        if (counter == null) throw new IllegalStateException("No se pudo reservar el ID");
        long value = ((Number) counter.get("seq")).longValue();
        if (value < 1 || value > Integer.MAX_VALUE) throw new IllegalStateException("Se agotaron los IDs enteros");
        return (int) value;
    }

    @Override
    public boolean actualizar(int id, Product product) {
        Update update = new Update().set("name", product.getName()).set("price", decimal(product.getPrice()))
            .set("description", product.getDescription());
        // matchedCount permite actualizar un producto sin cambios y responder 200.
        return mongo.updateFirst(porId(id), update, COLLECTION).getMatchedCount() > 0;
    }

    @Override
    public boolean eliminar(int id) {
        return mongo.remove(porId(id), COLLECTION).getDeletedCount() > 0;
    }

    @Override
    public AggregationResult<Product> buscarPorNombre(String texto) {
        return ejecutar("nombre", List.of(
            new Document("$match", new Document("name", new Document("$regex", Pattern.quote(texto)).append("$options", "i"))),
            new Document("$sort", new Document("_id", 1)),
            new Document("$limit", 100)), this::producto);
    }

    @Override
    public AggregationResult<Product> filtrarPorPrecios(BigDecimal minimo, BigDecimal maximo) {
        return ejecutar("precios", List.of(
            new Document("$match", new Document("price", new Document("$gte", decimal(minimo)).append("$lte", decimal(maximo)))),
            new Document("$sort", new Document("price", 1).append("_id", 1)),
            new Document("$limit", 100)), this::producto);
    }

    @Override
    public AggregationResult<Product> masCostosos(int limite) {
        return ejecutar("costosos", List.of(
            new Document("$sort", new Document("price", -1).append("_id", 1)),
            new Document("$limit", limite)), this::producto);
    }

    @Override
    public AggregationResult<ProductStatistics> estadisticas() {
        var result = ejecutar("estadisticas", List.of(new Document("$group", new Document("_id", null)
            .append("cantidad", new Document("$sum", 1))
            .append("precioPromedio", new Document("$avg", "$price"))
            .append("precioMinimo", new Document("$min", "$price"))
            .append("precioMaximo", new Document("$max", "$price")))),
            d -> new ProductStatistics(((Number) d.get("cantidad")).longValue(),
                dinero(d, "precioPromedio"), dinero(d, "precioMinimo"), dinero(d, "precioMaximo")));
        return result.resultados().isEmpty()
            ? new AggregationResult<>(result.filtro(), List.of(new ProductStatistics(0, null, null, null)), result.pipeline())
            : result;
    }

    private <T> AggregationResult<T> ejecutar(String filtro, List<Document> stages, Function<Document, T> mapper) {
        List<AggregationOperation> operations = stages.stream()
            .<AggregationOperation>map(stage -> context -> stage).toList();
        var aggregation = Aggregation.newAggregation(operations).withOptions(
            AggregationOptions.builder().maxTime(java.time.Duration.ofSeconds(10)).build());
        var results = mongo.aggregate(aggregation, COLLECTION, Document.class).getMappedResults();
        // Serializar las mismas etapas enviadas al motor, sin reconstruir un ejemplo.
        return new AggregationResult<>(filtro, results.stream().map(mapper).toList(),
            stages.stream().map(this::extendedJson).toList());
    }

    private Map<String, Object> extendedJson(Document document) {
        Map<String, Object> json = new LinkedHashMap<>();
        document.forEach((key, value) -> json.put(key, value instanceof Document nested ? extendedJson(nested)
            : value instanceof Decimal128 number ? Map.of("$numberDecimal", number.toString()) : value));
        return json;
    }

    private Query porId(int id) { return Query.query(Criteria.where("_id").is(id)); }

    private Product producto(Document d) {
        return new Product(d.getInteger("_id"), d.getString("name"), dinero(d, "price"), d.getString("description"));
    }

    private BigDecimal dinero(Document d, String field) {
        return d.get(field, Decimal128.class).bigDecimalValue();
    }

    private Decimal128 decimal(BigDecimal value) {
        try { return new Decimal128(value); }
        catch (NumberFormatException | ArithmeticException invalid) {
            throw new IllegalArgumentException("El precio excede la precisión o el rango decimal de MongoDB");
        }
    }
}
