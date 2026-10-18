package com.example.productoscrud;

import com.example.productoscrud.application.ports.ProductRepository;
import com.example.productoscrud.application.ports.ProductAggregationRepository;
import com.example.productoscrud.application.query.AggregationResult;
import com.example.productoscrud.application.service.ProductService;
import com.example.productoscrud.application.service.ProductQueryService;
import com.example.productoscrud.domain.model.Product;
import com.example.productoscrud.infrastructure.web.ApiExceptionHandler;
import com.example.productoscrud.infrastructure.web.controllers.ProductController;
import com.example.productoscrud.infrastructure.web.controllers.ProductQueryController;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductHttpTest {
    private final ProductRepository mysql = mock(ProductRepository.class);
    private final ProductRepository mongo = mock(ProductRepository.class);
    private final ProductAggregationRepository queries = mock(ProductAggregationRepository.class);
    private MockMvc mvc;
    private final Product product = new Product(1, "Mouse", new BigDecimal("10.25"), "USB");
    private static final String BODY = "{\"name\":\"Mouse\",\"price\":\"10.25\",\"description\":\"USB\"}";

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new ProductController(new ProductService(mysql), new ProductService(mongo)),
            new ProductQueryController(new ProductQueryService(queries)))
            .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test void rutasSqlYLegacyMantienenCompatibilidadYMongoEstaSeparado() throws Exception {
        when(mysql.listar()).thenReturn(List.of(product));
        when(mongo.listar()).thenReturn(List.of());
        mvc.perform(get("/productos")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Mouse"));
        mvc.perform(get("/api/mysql/productos/")).andExpect(status().isOk()).andExpect(jsonPath("$[0].price").value(10.25));
        mvc.perform(get("/api/mongodb/productos")).andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(mysql, times(2)).listar();
        verify(mongo).listar();
    }

    @Test void crudHttpEnLasDosTecnologias() throws Exception {
        for (var entry : Map.of("mysql", mysql, "mongodb", mongo).entrySet()) {
            var repo = entry.getValue();
            String base = "/api/" + entry.getKey() + "/productos";
            when(repo.crear(any(Product.class))).thenReturn(product);
            when(repo.buscarPorId(1)).thenReturn(Optional.of(product));
            when(repo.actualizar(eq(1), any(Product.class))).thenReturn(true);
            when(repo.eliminar(1)).thenReturn(true);
            mvc.perform(post(base).contentType("application/json").content(BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1));
            mvc.perform(get(base + "/1")).andExpect(status().isOk());
            mvc.perform(put(base + "/1").contentType("application/json").content(BODY)).andExpect(status().isOk());
            mvc.perform(delete(base + "/1")).andExpect(status().isNoContent());
            mvc.perform(get(base + "/99")).andExpect(status().isNotFound());
            mvc.perform(put(base + "/99").contentType("application/json").content(BODY)).andExpect(status().isNotFound());
            mvc.perform(delete(base + "/99")).andExpect(status().isNotFound());
        }
    }

    @Test void parametrosInvalidosYErroresNoFiltranDetallesInternos() throws Exception {
        mvc.perform(get("/api/otra/productos")).andExpect(status().isBadRequest());
        mvc.perform(get("/productos/0")).andExpect(status().isBadRequest());
        mvc.perform(get("/productos/abc")).andExpect(status().isBadRequest());
        mvc.perform(post("/productos").contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        mvc.perform(post("/productos").contentType("application/json").content("{"))
            .andExpect(status().isBadRequest());
        when(mysql.listar()).thenThrow(new DataAccessResourceFailureException("private connection details"));
        mvc.perform(get("/productos")).andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.message").value("No se pudo completar la operación en la base de datos seleccionada"));
    }

    @Test void consultasDevuelvenResultadosYPipelineExtendedJson() throws Exception {
        when(queries.filtrarPorPrecios(new BigDecimal("0.10"), new BigDecimal("20.00")))
            .thenReturn(new AggregationResult<>("precios", List.of(product), List.of(Map.of("$match",
                Map.of("price", Map.of("$gte", Map.of("$numberDecimal", "0.10")))))));
        mvc.perform(get("/api/mongodb/productos/filtros/precios").param("minimo", "0.10").param("maximo", "20.00"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.resultados[0].id").value(1))
            .andExpect(jsonPath("$.pipeline[0]['$match'].price['$gte']['$numberDecimal']").value("0.10"));
        when(queries.buscarPorNombre("Mouse")).thenReturn(new AggregationResult<>("nombre", List.of(), List.of()));
        when(queries.masCostosos(5)).thenReturn(new AggregationResult<>("costosos", List.of(), List.of()));
        when(queries.estadisticas()).thenReturn(new AggregationResult<>("estadisticas", List.of(), List.of()));
        mvc.perform(get("/api/mongodb/productos/filtros/nombre").param("texto", "Mouse")).andExpect(status().isOk());
        mvc.perform(get("/api/mongodb/productos/filtros/costosos")).andExpect(status().isOk());
        mvc.perform(get("/api/mongodb/productos/filtros/estadisticas")).andExpect(status().isOk());
        mvc.perform(get("/api/mongodb/productos/filtros/nombre")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/mongodb/productos/filtros/nombre").param("texto", "  ")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/mongodb/productos/filtros/precios").param("minimo", "20").param("maximo", "1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/mongodb/productos/filtros/precios").param("minimo", "abc").param("maximo", "1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/mongodb/productos/filtros/costosos").param("limite", "101")).andExpect(status().isBadRequest());
    }
}
