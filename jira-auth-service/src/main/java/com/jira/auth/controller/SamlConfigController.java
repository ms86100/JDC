package com.jira.auth.controller;

import com.jira.auth.entity.SamlConfiguration;
import com.jira.auth.service.SamlConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/sso/saml")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
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
