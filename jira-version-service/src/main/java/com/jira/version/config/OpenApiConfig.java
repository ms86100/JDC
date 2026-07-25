package com.jira.version.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;

@Configuration
public class OpenApiConfig {

    @Value("${app.openapi.title:Jira Version Service API}")
    private String apiTitle;

    @Value("${app.openapi.description:Enterprise-grade Version Management Service for Jira Platform}")
    private String apiDescription;

    @Value("${app.openapi.version:1.0.0}")
    private String apiVersion;

    @Value("${app.openapi.contact-name:Jira Platform Team}")
    private String contactName;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title(apiTitle)
                .description(apiDescription)
                .version(apiVersion)
                .contact(new Contact()
                    .name(contactName)));
    }
}