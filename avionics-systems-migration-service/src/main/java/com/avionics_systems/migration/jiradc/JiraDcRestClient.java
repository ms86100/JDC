package com.avionics_systems.migration.jiradc;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.*;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
public class JiraDcRestClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String pat;
    private final int maxRetries;
    private final long retryDelayMs;

    public JiraDcRestClient(JiraDcConnectionConfig config, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.baseUrl = config.getBaseUrl().replaceAll("/+$", "");
        this.pat = config.getPat();
        this.maxRetries = config.getRetryAttempts();
        this.retryDelayMs = config.getRetryDelayMs();
        this.restTemplate = buildRestTemplate(config);
    }

    private RestTemplate buildRestTemplate(JiraDcConnectionConfig config) {
        if (config.isTrustAllCertificates()) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }}, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            } catch (Exception e) {
                log.warn("Failed to configure trust-all SSL, falling back to default", e);
            }
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getConnectTimeoutMs());
        factory.setReadTimeout(config.getReadTimeoutMs());
        return new RestTemplate(factory);
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(pat);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeGet(String endpoint) {
        return executeWithRetry(() -> {
            URI uri = URI.create(baseUrl + endpoint);
            log.debug("GET {}", uri);
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            try {
                return objectMapper.readValue(response.getBody(), Map.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse response", e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private List<Object> executeGetList(String endpoint) {
        return executeWithRetry(() -> {
            URI uri = URI.create(baseUrl + endpoint);
            log.debug("GET (list) {}", uri);
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            try {
                return objectMapper.readValue(response.getBody(), List.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse list response", e);
            }
        });
    }

    public byte[] downloadBinary(String url) {
        return executeWithRetry(() -> {
            log.debug("GET (binary) {}", url);
            HttpHeaders headers = createHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL));
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            return response.getBody();
        });
    }

    private <T> T executeWithRetry(java.util.function.Supplier<T> action) {
        int attempt = 0;
        while (true) {
            try {
                return action.get();
            } catch (HttpServerErrorException | ResourceAccessException e) {
                attempt++;
                if (attempt > maxRetries) {
                    throw new JiraDcApiException("Jira DC API call failed after " + maxRetries + " retries", e);
                }
                long delay = retryDelayMs * attempt;
                log.warn("Jira DC API call failed (attempt {}/{}), retrying in {}ms: {}",
                        attempt, maxRetries, delay, e.getMessage());
                try { Thread.sleep(delay); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new JiraDcApiException("Interrupted during retry", ie);
                }
            } catch (HttpClientErrorException e) {
                throw new JiraDcApiException("Jira DC API client error: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
            }
        }
    }

    // ========== Server Info ==========

    public Map<String, Object> getServerInfo() {
        return executeGet("/rest/api/2/serverInfo");
    }

    public Map<String, Object> getMyself() {
        return executeGet("/rest/api/2/myself");
    }

    // ========== Metadata ==========

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getProjects() {
        List<Object> raw = executeGetList("/rest/api/2/project");
        return raw.stream().map(o -> (Map<String, Object>) o).toList();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getIssueTypes() {
        List<Object> raw = executeGetList("/rest/api/2/issuetype");
        return raw.stream().map(o -> (Map<String, Object>) o).toList();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPriorities() {
        List<Object> raw = executeGetList("/rest/api/2/priority");
        return raw.stream().map(o -> (Map<String, Object>) o).toList();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getStatuses() {
        List<Object> raw = executeGetList("/rest/api/2/status");
        return raw.stream().map(o -> (Map<String, Object>) o).toList();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getResolutions() {
        List<Object> raw = executeGetList("/rest/api/2/resolution");
        return raw.stream().map(o -> (Map<String, Object>) o).toList();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getFields() {
        List<Object> raw = executeGetList("/rest/api/2/field");
        return raw.stream().map(o -> (Map<String, Object>) o).toList();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getProjectComponents(String projectKey) {
        List<Object> raw = executeGetList("/rest/api/2/project/" + encode(projectKey) + "/components");
        return raw.stream().map(o -> (Map<String, Object>) o).toList();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getProjectVersions(String projectKey) {
        List<Object> raw = executeGetList("/rest/api/2/project/" + encode(projectKey) + "/versions");
        return raw.stream().map(o -> (Map<String, Object>) o).toList();
    }

    // ========== Issue Search ==========

    @SuppressWarnings("unchecked")
    public SearchResult searchIssues(String jql, int startAt, int maxResults, String expand) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/rest/api/2/search")
                .queryParam("jql", jql)
                .queryParam("startAt", startAt)
                .queryParam("maxResults", maxResults);
        if (expand != null && !expand.isBlank()) {
            builder.queryParam("expand", expand);
        }
        URI uri = builder.encode().build().toUri();
        Map<String, Object> response = executeWithRetry(() -> {
            log.debug("GET {}", uri);
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> resp = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            try {
                return objectMapper.readValue(resp.getBody(), Map.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse search response", e);
            }
        });
        int total = ((Number) response.getOrDefault("total", 0)).intValue();
        List<Map<String, Object>> issues = ((List<Object>) response.getOrDefault("issues", List.of()))
                .stream().map(o -> (Map<String, Object>) o).toList();
        return new SearchResult(total, startAt, issues);
    }

    // ========== Single Issue ==========

    public Map<String, Object> getIssue(String issueKey, String expand) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/rest/api/2/issue/" + issueKey);
        if (expand != null && !expand.isBlank()) {
            builder.queryParam("expand", expand);
        }
        URI uri = builder.encode().build().toUri();
        return executeWithRetry(() -> {
            log.debug("GET {}", uri);
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> resp = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            try {
                return objectMapper.readValue(resp.getBody(), Map.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse issue response", e);
            }
        });
    }

    // ========== Comments ==========

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getIssueComments(String issueKey) {
        Map<String, Object> response = executeGet("/rest/api/2/issue/" + encode(issueKey) + "/comment");
        List<Object> comments = (List<Object>) response.getOrDefault("comments", List.of());
        return comments.stream().map(o -> (Map<String, Object>) o).toList();
    }

    // ========== Worklogs ==========

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getIssueWorklogs(String issueKey) {
        Map<String, Object> response = executeGet("/rest/api/2/issue/" + encode(issueKey) + "/worklog");
        List<Object> worklogs = (List<Object>) response.getOrDefault("worklogs", List.of());
        return worklogs.stream().map(o -> (Map<String, Object>) o).toList();
    }

    // ========== Watchers ==========

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getIssueWatchers(String issueKey) {
        Map<String, Object> response = executeGet("/rest/api/2/issue/" + encode(issueKey) + "/watchers");
        List<Object> watchers = (List<Object>) response.getOrDefault("watchers", List.of());
        return watchers.stream().map(o -> (Map<String, Object>) o).toList();
    }

    // ========== Helpers ==========

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public record SearchResult(int total, int startAt, List<Map<String, Object>> issues) {}

    public static class JiraDcApiException extends RuntimeException {
        public JiraDcApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
