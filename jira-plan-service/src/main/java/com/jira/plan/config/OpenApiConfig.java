package com.jira.plan.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI planServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("JIRA Plan Service API")
                        .description("API for managing Jira DC Plan Management features")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("JIRA Platform Team")
                                .email("platform@jira.com")));
    }
}
