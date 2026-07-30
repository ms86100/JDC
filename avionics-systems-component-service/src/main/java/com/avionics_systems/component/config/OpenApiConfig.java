package com.avionics_systems.component.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Avionics Systems Component Service API")
                .description("Enterprise-grade Component Management Service for Avionics Systems Platform")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Avionics Systems Platform Team")));
    }
}
