package com.avionics_systems.workflow.engine.script;

import com.avionics_systems.workflow.config.ScriptEngineProperties;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.HostAccess;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class JdcHttpApi {

    private final RestTemplate restTemplate;
    private final List<String> whitelistDomains;

    public JdcHttpApi(ScriptEngineProperties properties) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofMillis(properties.getHttpTimeoutMs()));
        this.restTemplate = new RestTemplate(factory);
        this.whitelistDomains = properties.getHttpWhitelistDomains();
    }

    @HostAccess.Export
    public Map<String, Object> get(String url, Map<String, String> headers) {
        try {
            validateUrl(url);
            HttpHeaders httpHeaders = buildHeaders(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(httpHeaders), String.class);
            return buildResponse(response);
        } catch (SecurityException e) {
            return Map.of("error", e.getMessage(), "status", 403);
        } catch (Exception e) {
            return Map.of("error", e.getMessage(), "status", 0);
        }
    }

    @HostAccess.Export
    public Map<String, Object> post(String url, Object body, Map<String, String> headers) {
        try {
            validateUrl(url);
            HttpHeaders httpHeaders = buildHeaders(headers);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, httpHeaders), String.class);
            return buildResponse(response);
        } catch (SecurityException e) {
            return Map.of("error", e.getMessage(), "status", 403);
        } catch (Exception e) {
            return Map.of("error", e.getMessage(), "status", 0);
        }
    }

    @HostAccess.Export
    public Map<String, Object> put(String url, Object body, Map<String, String> headers) {
        try {
            validateUrl(url);
            HttpHeaders httpHeaders = buildHeaders(headers);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PUT, new HttpEntity<>(body, httpHeaders), String.class);
            return buildResponse(response);
        } catch (SecurityException e) {
            return Map.of("error", e.getMessage(), "status", 403);
        } catch (Exception e) {
            return Map.of("error", e.getMessage(), "status", 0);
        }
    }

    @HostAccess.Export
    public Map<String, Object> delete(String url, Map<String, String> headers) {
        try {
            validateUrl(url);
            HttpHeaders httpHeaders = buildHeaders(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.DELETE, new HttpEntity<>(httpHeaders), String.class);
            return buildResponse(response);
        } catch (SecurityException e) {
            return Map.of("error", e.getMessage(), "status", 403);
        } catch (Exception e) {
            return Map.of("error", e.getMessage(), "status", 0);
        }
    }

    @HostAccess.Export
    public Map<String, Object> patch(String url, Object body, Map<String, String> headers) {
        try {
            validateUrl(url);
            HttpHeaders httpHeaders = buildHeaders(headers);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PATCH, new HttpEntity<>(body, httpHeaders), String.class);
            return buildResponse(response);
        } catch (SecurityException e) {
            return Map.of("error", e.getMessage(), "status", 403);
        } catch (Exception e) {
            return Map.of("error", e.getMessage(), "status", 0);
        }
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new SecurityException("URL is required");
        }

        if (whitelistDomains == null || whitelistDomains.isEmpty()) {
            throw new SecurityException("HTTP access is disabled — no domains are whitelisted. Configure avionics-systems.scripting.http-whitelist-domains.");
        }

        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) {
                throw new SecurityException("Invalid URL: no host");
            }

            String hostLower = host.toLowerCase();
            if (hostLower.equals("localhost") || hostLower.equals("127.0.0.1")
                    || hostLower.equals("0.0.0.0") || hostLower.equals("::1")) {
                throw new SecurityException("Access to localhost is blocked");
            }

            try {
                InetAddress addr = InetAddress.getByName(host);
                if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress()) {
                    throw new SecurityException("Access to internal/private networks is blocked");
                }
            } catch (SecurityException se) {
                throw se;
            } catch (Exception ignored) {}

            boolean allowed = whitelistDomains.stream()
                    .anyMatch(d -> hostLower.equals(d.toLowerCase()) || hostLower.endsWith("." + d.toLowerCase()));
            if (!allowed) {
                throw new SecurityException("Domain '" + host + "' is not in the whitelist");
            }
        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            throw new SecurityException("Invalid URL: " + e.getMessage());
        }
    }

    private HttpHeaders buildHeaders(Map<String, String> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            headers.forEach((k, v) -> {
                String key = k.toLowerCase();
                if (!key.equals("host") && !key.equals("cookie") && !key.equals("authorization")) {
                    httpHeaders.set(k, v);
                }
            });
        }
        return httpHeaders;
    }

    private Map<String, Object> buildResponse(ResponseEntity<String> response) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", response.getStatusCode().value());
        result.put("body", response.getBody());
        return result;
    }
}
