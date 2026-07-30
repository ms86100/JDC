package com.avionics_systems.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.avionics_systems.document.entity")
@EnableJpaRepositories(basePackages = "com.avionics_systems.document.repository")
public class AvionicsSystemDocumentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AvionicsSystemDocumentServiceApplication.class, args);
    }
}
