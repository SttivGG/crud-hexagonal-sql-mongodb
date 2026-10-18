package com.example.productoscrud.app;

import com.example.productoscrud.application.ports.ProductRepository;
import com.example.productoscrud.application.service.ProductService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Qualifier;
import com.example.productoscrud.application.ports.ProductAggregationRepository;
import com.example.productoscrud.application.service.ProductQueryService;

@SpringBootApplication(scanBasePackages = "com.example.productoscrud")
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean("mysqlProductService")
    public ProductService productService(@Qualifier("productRepositoryMySQL") ProductRepository productRepository) {
        return new ProductService(productRepository);
    }

    @Bean("mongoProductService")
    public ProductService mongoProductService(@Qualifier("productRepositoryMongoDB") ProductRepository repository) {
        return new ProductService(repository);
    }

    @Bean
    public ProductQueryService productQueryService(ProductAggregationRepository repository) {
        return new ProductQueryService(repository);
    }
}

// Origen: la JVM llama a App.main() (com.example.productoscrud.app).
// Destino: App.main() inicia Spring (org.springframework.boot); Spring llama a App.productService()
// y conecta ProductService (application.service) con ProductRepository (application.ports).
