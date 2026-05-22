package com.jira.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.CrossOrigin;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

import java.util.List;

@Configuration
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:8080"})
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Jira Platform API Gateway")
                        .version("1.0.0")
                        .description("Enterprise Jira Platform API Gateway - Aggregated Swagger Documentation\n\n" +
                                "## Services\n" +
                                "- **Auth Service**: Authentication & User Registration\n" +
                                "- **User Service**: User Profiles & Organizations\n" +
                                "- **Project Service**: Projects, Templates & Security\n" +
                                "- **Issue Service**: Issues, Labels & Worklogs\n" +
                                "- **Workflow Service**: Workflows & Transitions\n" +
                                "- **Comment Service**: Comments & Threading\n" +
                                "- **Notification Service**: Notifications\n" +
                                "- **Search Service**: Search & JQL\n" +
                                "- **Audit Service**: Audit Logging\n" +
                                "- **Attachment Service**: File Attachments\n" +
                                "- **Sprint Service**: Sprints & Kanban\n" +
                                "- **Plan Service**: Planning & Roadmaps\n" +
                                "- **Admin Service**: Platform Administration\n" +
                                "- **Migration Service**: Jira DC Import\n" +
                                "- **Test Service**: Test Management (Xray Plugin)\n" +
                                "- **Version Service**: Version Management\n" +
                                "- **Component Service**: Component Management\n\n" +
                                "## Authentication\n" +
                                "Most endpoints require JWT Bearer token:\n" +
                                "```\n" +
                                "Authorization: Bearer <your-jwt-token>\n" +
                                "```\n\n" +
                                "## Quick Start\n" +
                                "1. Register: `POST /api/auth/register`\n" +
                                "2. Login: `POST /api/auth/login` (returns JWT)\n" +
                                "3. Use token in Authorization header for other endpoints")
                        .contact(new Contact()
                                .name("Jira Platform Team")
                                .email("platform@jira.example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("http://localhost:8081").description("Auth Service Direct"),
                        new Server().url("http://localhost:8082").description("User Service Direct"),
                        new Server().url("http://localhost:8083").description("Project Service Direct"),
                        new Server().url("http://localhost:8084").description("Issue Service Direct"),
                        new Server().url("http://localhost:8085").description("Workflow Service Direct")
                ));
    }
}
