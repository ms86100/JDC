package com.jira.migration.service.clients;

import com.jira.migration.service.clients.dto.UserResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service client for the User Service.
 * Provides operations for finding and managing users.
 */
@Service
@Slf4j
public class UserServiceClient extends BaseServiceClient {

    private static final String SERVICE_NAME = "userService";
    private static final String SERVICE_PATH = "/api/users";

    @Autowired
    public UserServiceClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${services.userServiceUrl:http://localhost:8083}") String baseUrl) {
        super(restTemplate, objectMapper, circuitBreakerRegistry, SERVICE_NAME, baseUrl);
    }

    @Override
    protected String getCircuitBreakerName() {
        return "userService";
    }

    @Override
    protected String getServicePathPrefix() {
        return SERVICE_PATH;
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param userId the user ID
     * @return the user response
     */
    public UserResponse getUserById(String userId) {
        log.debug("Fetching user with ID: {}", userId);
        String endpoint = SERVICE_PATH + "/" + userId;
        return executeGet(endpoint, UserResponse.class);
    }

    /**
     * Finds a user by their email address.
     *
     * @param email the user email
     * @return the user response
     */
    public UserResponse getUserByEmail(String email) {
        log.debug("Fetching user with email: {}", email);
        String endpoint = SERVICE_PATH + "/by-email/" + encodeValue(email);
        return executeGet(endpoint, UserResponse.class);
    }

    /**
     * Searches for users matching a query string.
     *
     * @param query the search query (username, name, or email)
     * @return list of matching users
     */
    public List<UserResponse> searchUsers(String query) {
        log.debug("Searching users with query: {}", query);

        ParameterizedTypeReference<List<UserResponse>> typeRef =
            new ParameterizedTypeReference<List<UserResponse>>() {};

        String endpoint = SERVICE_PATH + "/search?query=" + encodeValue(query);
        String url = buildUrl(endpoint);
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<List<UserResponse>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, typeRef);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Search users {} -> {} ({}ms), found {} users",
                    url, response.getStatusCode(), elapsed,
                    response.getBody() != null ? response.getBody().size() : 0);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Search users {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Retrieves all users associated with a specific project.
     *
     * @param projectId the project ID
     * @return list of project users
     */
    public List<UserResponse> getUsersByProject(String projectId) {
        log.debug("Fetching users for project: {}", projectId);

        ParameterizedTypeReference<List<UserResponse>> typeRef =
            new ParameterizedTypeReference<List<UserResponse>>() {};

        String endpoint = SERVICE_PATH + "/project/" + projectId;
        String url = buildUrl(endpoint);
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<List<UserResponse>> response = restTemplate.exchange(
                    url, HttpMethod.GET, requestEntity, typeRef);
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Project users {} -> {} ({}ms), found {} users",
                    url, response.getStatusCode(), elapsed,
                    response.getBody() != null ? response.getBody().size() : 0);
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Project users {} failed ({}ms): {}", url, elapsed, e.getMessage());
            throw ServiceClientException.connectionError(serviceName, endpoint, e);
        }
    }

    /**
     * Gets a user by username.
     *
     * @param username the username
     * @return the user response
     */
    public UserResponse getUserByUsername(String username) {
        log.debug("Fetching user with username: {}", username);
        String endpoint = SERVICE_PATH + "/username/" + encodeValue(username);
        return executeGet(endpoint, UserResponse.class);
    }

    /**
     * Finds a user by email, returning empty if not found.
     *
     * @param email the user email
     * @return optional user response
     */
    public UserResponse createUser(Map<String, Object> userData) {
        log.info("Creating user via user-service");
        return executePost(SERVICE_PATH, userData, UserResponse.class);
    }

    public Optional<UserResponse> findUserByEmail(String email) {
        try {
            return Optional.ofNullable(getUserByEmail(email));
        } catch (ServiceClientException e) {
            log.debug("User not found with email {}: {}", email, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Gets the current authenticated user.
     *
     * @return the current user response
     */
    public UserResponse getCurrentUser() {
        log.debug("Fetching current user");
        String endpoint = SERVICE_PATH + "/me";
        return executeGet(endpoint, UserResponse.class);
    }

    /**
     * Checks if a user exists.
     *
     * @param userId the user ID
     * @return true if user exists
     */
    public boolean userExists(String userId) {
        try {
            getUserById(userId);
            return true;
        } catch (ServiceClientException e) {
            return false;
        }
    }

    private String encodeValue(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}