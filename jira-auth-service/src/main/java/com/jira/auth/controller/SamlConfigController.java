package com.jira.auth.controller;

import com.jira.auth.entity.SamlConfiguration;
import com.jira.auth.service.SamlConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/sso/saml")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class SamlConfigController {

    private final SamlConfigService samlConfigService;

    @GetMapping
    public ResponseEntity<List<SamlConfiguration>> listAll() {
        return ResponseEntity.ok(samlConfigService.listAll());
    }

    @GetMapping("/enabled")
    public ResponseEntity<List<SamlConfiguration>> listEnabled() {
        return ResponseEntity.ok(samlConfigService.listEnabled());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SamlConfiguration> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(samlConfigService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SamlConfiguration> create(@RequestBody SamlConfiguration config) {
        return ResponseEntity.status(HttpStatus.CREATED).body(samlConfigService.create(config));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SamlConfiguration> update(@PathVariable UUID id,
                                                     @RequestBody SamlConfiguration update) {
        return ResponseEntity.ok(samlConfigService.update(id, update));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        samlConfigService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sp-metadata")
    public ResponseEntity<Map<String, Object>> getSpMetadata() {
        Map<String, Object> metadata = Map.of(
                "entityId", "jira-platform-sp",
                "acsUrl", "/login/saml2/sso",
                "sloUrl", "/logout/saml2/slo",
                "nameIdFormat", "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
                "note", "Full XML metadata available when spring-security-saml2-service-provider is on classpath"
        );
        return ResponseEntity.ok(metadata);
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, Object> request) {
        String idpSsoUrl = (String) request.get("idpSsoUrl");
        if (idpSsoUrl == null || idpSsoUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "idpSsoUrl is required"));
        }
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(idpSsoUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "idpSsoUrl", idpSsoUrl,
                    "httpStatus", responseCode,
                    "reachable", responseCode < 500
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "idpSsoUrl", idpSsoUrl,
                    "message", e.getMessage(),
                    "reachable", false
            ));
        }
    }

    @PostMapping("/test-auth")
    public ResponseEntity<Map<String, Object>> testSamlAuth(@RequestBody Map<String, Object> request) {
        String nameId = (String) request.get("nameId");
        String registrationId = (String) request.get("registrationId");
        @SuppressWarnings("unchecked")
        Map<String, String> attributes = (Map<String, String>) request.getOrDefault("attributes", Map.of());

        Map<String, Object> result = samlConfigService.authenticateSamlUser(nameId, registrationId, attributes);
        return ResponseEntity.ok(result);
    }
}
