package com.jira.attachment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Jira Attachment Service API")
                        .description("API for managing issue attachments in Jira Platform")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Jira Platform Team")
                                .email("platform@jira.local")));
    }
}