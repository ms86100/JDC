package com.jira.migration.controller;

import com.jira.migration.dto.*;
import com.jira.migration.entity.field.CustomFieldDefinition;
import com.jira.migration.entity.field.FieldDefinition;
import com.jira.migration.repository.field.CustomFieldContextRepository;
import com.jira.migration.repository.field.CustomFieldDefinitionRepository;
import com.jira.migration.repository.field.FieldDefinitionRepository;
import com.jira.migration.service.field.FieldProvisioningService;
import com.jira.migration.service.field.FieldScreenConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Jira-compatible {@code /api/custom-fields} surface (routes to migration field registry, not test-service).
 */
@RestController
@RequestMapping("/api/custom-fields")
@RequiredArgsConstructor
@Tag(name = "Custom Fields (compat)", description = "Legacy /api/custom-fields API backed by migration-service")
public class CustomFieldsCompatController {

    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    private final CustomFieldContextRepository customFieldContextRepository;
    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final FieldProvisioningService fieldProvisioningService;
    private final FieldScreenConfigurationService fieldScreenConfigurationService;

    @GetMapping
    @Operation(summary = "List custom fields")
    public ResponseEntity<List<Map<String, Object>>> listCustomFields() {
        return ResponseEntity.ok(customFieldDefinitionRepository.findAllEnabled().stream()
                .map(this::toCompat)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCustomField(@PathVariable UUID id) {
        return customFieldDefinitionRepository.findById(id)
                .map(cf -> ResponseEntity.ok(toCompat(cf)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCustomField(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        String name = stringVal(body.get("name"));
        String type = stringVal(body.getOrDefault("type", "text"));
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "name is required"));
        }
        FieldDefinition saved = fieldProvisioningService.provisionCustomField(name, type, actor);
        fieldScreenConfigurationService.ensureFieldVisibleOnScreen(saved.getFieldKey(), parseProjectId(body));
        return customFieldDefinitionRepository.findByFieldKey(saved.getFieldKey())
                .map(cf -> ResponseEntity.status(HttpStatus.CREATED).body(toCompat(cf)))
                .orElse(ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                        "id", saved.getId().toString(),
                        "name", saved.getDisplayName(),
                        "type", type,
                        "fieldKey", saved.getFieldKey(),
                        "isRequired", false,
                        "createdAt", LocalDateTime.now().toString()
                )));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCustomField(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        return customFieldDefinitionRepository.findById(id)
                .map(cf -> {
                    if (body.containsKey("name")) {
                        cf.setName(stringVal(body.get("name")));
                    }
                    if (body.containsKey("description")) {
                        cf.setDescription(stringVal(body.get("description")));
                    }
                    if (body.containsKey("type")) {
                        cf.setType(stringVal(body.get("type")));
                    }
                    if (body.containsKey("enabled")) {
                        cf.setEnabled(Boolean.TRUE.equals(body.get("enabled")));
                    }
                    CustomFieldDefinition saved = customFieldDefinitionRepository.save(cf);
                    fieldDefinitionRepository.findByFieldKey(saved.getFieldKey()).ifPresent(def -> {
                        if (body.containsKey("name")) {
                            def.setDisplayName(saved.getName());
                        }
                        if (body.containsKey("description")) {
                            def.setDescription(saved.getDescription());
                        }
                        fieldDefinitionRepository.save(def);
                    });
                    fieldScreenConfigurationService.ensureFieldVisibleOnScreen(saved.getFieldKey(), parseProjectId(body));
                    return ResponseEntity.ok(toCompat(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomField(@PathVariable UUID id) {
        return customFieldDefinitionRepository.findById(id)
                .map(cf -> {
                    cf.setEnabled(false);
                    customFieldDefinitionRepository.save(cf);
                    fieldDefinitionRepository.findByFieldKey(cf.getFieldKey()).ifPresent(def -> {
                        def.setHidden(true);
                        def.setDeprecated(true);
                        fieldDefinitionRepository.save(def);
                    });
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toCompat(CustomFieldDefinition cf) {
        int contextCount = customFieldContextRepository.findByCustomFieldId(cf.getId()).size();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", cf.getId().toString());
        m.put("name", cf.getName());
        m.put("description", cf.getDescription());
        m.put("type", cf.getType());
        m.put("fieldKey", cf.getFieldKey());
        m.put("isRequired", false);
        m.put("contextCount", contextCount);
        m.put("enabled", cf.getEnabled());
        m.put("createdAt", cf.getCreatedAt() != null ? cf.getCreatedAt().toString() : null);
        return m;
    }

    private UUID parseProjectId(Map<String, Object> body) {
        Object raw = body.get("projectId");
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String stringVal(Object o) {
        return o == null ? null : o.toString().trim();
    }
}
