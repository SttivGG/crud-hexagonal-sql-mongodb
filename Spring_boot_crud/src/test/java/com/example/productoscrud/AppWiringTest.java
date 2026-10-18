package com.example.productoscrud;

import com.example.productoscrud.app.App;
import com.example.productoscrud.application.ports.ProductRepository;
import com.example.productoscrud.application.service.ProductService;
import com.mongodb.client.MongoClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = App.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
    "spring.datasource.url=jdbc:h2:mem:wiring;MODE=MySQL", "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password="
})
class AppWiringTest {
    @MockitoBean MongoClient client;
    @Autowired ApplicationContext context;
    @Autowired WebApplicationContext webContext;

    @Test void arrancaConDosAdaptadoresYDosServiciosSinAmbiguedad() {
        assertEquals(2, context.getBeansOfType(ProductRepository.class).size());
        assertEquals(2, context.getBeansOfType(ProductService.class).size());
        assertNotSame(context.getBean("mysqlProductService"), context.getBean("mongoProductService"));
    }

    @Test void corsAdmiteElFrontendLocalYRechazaOtrosOrigenes() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(webContext).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/mongodb/productos")
            .header("Origin", "http://127.0.0.1:5175").header("Access-Control-Request-Method", "POST"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                .string("Access-Control-Allow-Origin", "http://127.0.0.1:5175"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/productos")
            .header("Origin", "https://example.com").header("Access-Control-Request-Method", "POST"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
    }
}
