package com.avionics_systems.admin.controller;

import com.avionics_systems.admin.dto.SystemConfigRequest;
import com.avionics_systems.admin.dto.SystemConfigResponse;
import com.avionics_systems.admin.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
@Tag(name = "System Configuration", description = "Dynamic system configuration management")
public class SystemConfigController {

    private final SystemConfigService configService;
    private final MessageSource messageSource;

    @GetMapping
    @Operation(summary = "Get all system configurations")
    public ResponseEntity<List<SystemConfigResponse>> getAllConfigs() {
        List<SystemConfigResponse> configs = configService.getAllConfigs().stream()
                .map(SystemConfigResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/{key}")
    @Operation(summary = "Get a single configuration by key")
    public ResponseEntity<SystemConfigResponse> getConfig(@PathVariable String key) {
        return ResponseEntity.ok(SystemConfigResponse.fromEntity(configService.getByKey(key)));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get all configurations in a category")
    public ResponseEntity<List<SystemConfigResponse>> getConfigsByCategory(@PathVariable String category) {
        List<SystemConfigResponse> configs = configService.getByCategory(category).stream()
                .map(SystemConfigResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(configs);
    }

    @PutMapping("/{key}")
    @Operation(summary = "Update a configuration value")
    public ResponseEntity<SystemConfigResponse> updateConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String newValue = body.get("value");
        if (newValue == null) {
            throw new IllegalArgumentException(
                    messageSource.getMessage("error.request.body.value.required", null, Locale.ENGLISH));
        }
        return ResponseEntity.ok(SystemConfigResponse.fromEntity(configService.updateByKey(key, newValue)));
    }

    @PostMapping
    @Operation(summary = "Create a new configuration entry")
    public ResponseEntity<SystemConfigResponse> createConfig(@Valid @RequestBody SystemConfigRequest request) {
        return new ResponseEntity<>(
                SystemConfigResponse.fromEntity(configService.create(request)),
                HttpStatus.CREATED);
    }

    @DeleteMapping("/{key}")
    @Operation(summary = "Delete a configuration entry")
    public ResponseEntity<Void> deleteConfig(@PathVariable String key) {
        configService.deleteByKey(key);
        return ResponseEntity.noContent().build();
    }
}
