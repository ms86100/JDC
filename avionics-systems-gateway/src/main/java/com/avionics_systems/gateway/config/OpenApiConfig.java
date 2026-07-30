package com.avionics_systems.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Avionics Systems API Gateway.
 * Provides comprehensive API documentation across all services.
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.openapi.title:Avionics Systems API Gateway}")
    private String apiTitle;

    @Value("${app.openapi.version:1.0.0}")
    private String apiVersion;

    @Value("${app.openapi.contact.name:Avionics Systems Team}")
    private String contactName;

    @Value("${app.openapi.contact.email:platform@avionics-systems.local}")
    private String contactEmail;

    @Value("${app.openapi.contact.url:https://avionics-systems.local}")
    private String contactUrl;

    @Value("${app.openapi.license.name:Apache 2.0}")
    private String licenseName;

    @Value("${app.openapi.license.url:https://www.apache.org/licenses/LICENSE-2.0}")
    private String licenseUrl;

    @Value("${app.openapi.servers.gateway-url:http://localhost:8080}")
    private String gatewayUrl;

    @Value("${app.openapi.servers.auth-url:http://localhost:8081}")
    private String authUrl;

    @Value("${app.openapi.servers.user-url:http://localhost:8082}")
    private String userUrl;

    @Value("${app.openapi.servers.project-url:http://localhost:8083}")
    private String projectUrl;

    @Value("${app.openapi.servers.issue-url:http://localhost:8084}")
    private String issueUrl;

    @Value("${app.openapi.servers.workflow-url:http://localhost:8085}")
    private String workflowUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(apiTitle)
                        .version(apiVersion)
                        .description(getApiDescription())
                        .contact(new Contact()
                                .name(contactName)
                                .email(contactEmail)
                                .url(contactUrl))
                        .license(new License()
                                .name(licenseName)
                                .url(licenseUrl)))
                .tags(getTags())
                .servers(getServers());
    }

    private String getApiDescription() {
        return """
                Enterprise Avionics Systems API Gateway - Aggregated Swagger Documentation

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
                - **Migration Service**: Legacy DC Import/Export

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
                        .url(gatewayUrl)
                        .description("API Gateway (Primary)"),
                new Server()
                        .url(authUrl)
                        .description("Auth Service Direct"),
                new Server()
                        .url(userUrl)
                        .description("User Service Direct"),
                new Server()
                        .url(projectUrl)
                        .description("Project Service Direct"),
                new Server()
                        .url(issueUrl)
                        .description("Issue Service Direct"),
                new Server()
                        .url(workflowUrl)
                        .description("Workflow Service Direct")
        );
    }
}