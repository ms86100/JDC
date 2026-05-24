package com.jira.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Jira Platform API Gateway.
 * Provides comprehensive API documentation across all services.
 */
@Configuration
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:8080"})
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Jira Platform API Gateway")
                        .version("1.0.0")
                        .description(getApiDescription())
                        .contact(new Contact()
                                .name("Jira Platform Team")
                                .email("platform@jira.example.com")
                                .url("https://jira.example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .tags(getTags())
                .servers(getServers());
    }

    private String getApiDescription() {
        return """
                Enterprise Jira Platform API Gateway - Aggregated Swagger Documentation

                ## Authentication
                Most endpoints require JWT Bearer token:
                ```
                Authorization: Bearer <your-jwt-token>
                ```

                ## Services

                ### Core Services
                - **Auth Service**: Authentication, Registration, JWT Token Management
                - **User Service**: User Profiles, Organizations, Groups
                - **Project Service**: Projects, Templates, Security Schemes

                ### Issue Management
                - **Issue Service**: Issues, Labels, Worklogs, Epics, Components
                - **Comment Service**: Comments, Threading, Mentions

                ### Workflow & Planning
                - **Workflow Service**: Workflows, Transitions, Validators
                - **Sprint Service**: Sprints, Kanban, Burndown
                - **Plan Service**: Planning, Roadmaps, Dependencies

                ### Search & Discovery
                - **Search Service**: Full-text Search, JQL, Autocomplete

                ### Support Services
                - **Notification Service**: Email, Webhook Notifications
                - **Attachment Service**: File Attachments, Media
                - **Audit Service**: Audit Logging, Compliance

                ### Administration
                - **Admin Service**: System Settings, User Management
                - **Migration Service**: Jira DC Import/Export

                ### Specialized
                - **Version Service**: Version Management
                - **Component Service**: Component Management
                - **Test Service**: Test Management (Xray Plugin)

                ## Quick Start
                1. Register: `POST /api/auth/register`
                2. Login: `POST /api/auth/login` (returns JWT)
                3. Use token in Authorization header for other endpoints

                ## Rate Limiting
                The API Gateway enforces rate limits:
                - 100 requests/minute per user
                - 1000 requests/hour per user
                - 10000 requests/day per user

                ## Response Codes
                - `200 OK`: Successful request
                - `201 Created`: Resource created
                - `400 Bad Request`: Invalid input
                - `401 Unauthorized`: Missing/invalid token
                - `403 Forbidden`: Insufficient permissions
                - `404 Not Found`: Resource not found
                - `429 Too Many Requests`: Rate limit exceeded
                - `500 Internal Server Error`: Server error
                """;
    }

    private List<Tag> getTags() {
        return List.of(
                new Tag()
                        .name("Authentication")
                        .description("User registration, login, and token management"),
                new Tag()
                        .name("Users")
                        .description("User profile and organization management"),
                new Tag()
                        .name("Projects")
                        .description("Project CRUD operations and settings"),
                new Tag()
                        .name("Issues")
                        .description("Issue management and operations"),
                new Tag()
                        .name("Search")
                        .description("Full-text search and JQL queries"),
                new Tag()
                        .name("Workflow")
                        .description("Workflow and transition management"),
                new Tag()
                        .name("Comments")
                        .description("Comment management"),
                new Tag()
                        .name("Sprints")
                        .description("Sprint and sprint board management"),
                new Tag()
                        .name("Plans")
                        .description("Planning and roadmap management"),
                new Tag()
                        .name("Notifications")
                        .description("Notification management"),
                new Tag()
                        .name("Administration")
                        .description("System administration endpoints"),
                new Tag()
                        .name("Health")
                        .description("Health check and monitoring"),
                new Tag()
                        .name("Benchmark")
                        .description("Performance testing and monitoring")
        );
    }

    private List<Server> getServers() {
        return List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("API Gateway (Primary)"),
                new Server()
                        .url("http://localhost:8081")
                        .description("Auth Service Direct"),
                new Server()
                        .url("http://localhost:8082")
                        .description("User Service Direct"),
                new Server()
                        .url("http://localhost:8083")
                        .description("Project Service Direct"),
                new Server()
                        .url("http://localhost:8084")
                        .description("Issue Service Direct"),
                new Server()
                        .url("http://localhost:8085")
                        .description("Workflow Service Direct")
        );
    }
}