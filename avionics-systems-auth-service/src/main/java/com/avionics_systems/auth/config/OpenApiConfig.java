package com.avionics_systems.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${app.openapi.title:Avionics Systems Auth Service API}")
    private String apiTitle;

    @Value("${app.openapi.version:1.0.0}")
    private String apiVersion;

    @Value("${app.openapi.description:Microservice for user authentication, registration, and JWT token management}")
    private String apiDescription;

    @Value("${app.openapi.contact-name:Avionics Systems Platform Team}")
    private String contactName;

    @Value("${app.openapi.license-name:Apache 2.0}")
    private String licenseName;

    @Value("${app.openapi.license-url:https://www.apache.org/licenses/LICENSE-2.0}")
    private String licenseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(apiTitle)
                        .version(apiVersion)
                        .description(apiDescription)
                        .contact(new Contact()
                                .name(contactName))
                        .license(new License()
                                .name(licenseName)
                                .url(licenseUrl)));
    }
}
