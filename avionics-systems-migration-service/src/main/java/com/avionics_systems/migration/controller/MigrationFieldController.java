package com.avionics_systems.migration.controller;

import com.avionics_systems.migration.entity.FieldMapping;
import com.avionics_systems.migration.repository.FieldMappingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Migration Field Management.
 * @deprecated Use {@code /api/migration/mappings} and {@code /api/fields/custom} (G-07).
 */
@Deprecated(since = "2026-05-22", forRemoval = true)
@RestController
@RequestMapping("/api/migration/fields")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Migration Fields", description = "Migration field mapping CRUD operations")
public class MigrationFieldController {

    private final FieldMappingRepository fieldMappingRepository;

    /**
     * GET /api/migration/fields - List all field mappings
     */
    @GetMapping
    @Operation(summary = "List field mappings (deprecated)", description = "Use /api/migration/mappings")
    public ResponseEntity<List<FieldMapping>> listFields(
            @Parameter(description = "Filter by mapping type") @RequestParam(required = false) String mappingType) {

        log.info("Listing field mappings: type={}", mappingType);

        List<FieldMapping> fields;
        if (mappingType != null) {
            fields = fieldMappingRepository.findByMappingType(mappingType);
        } else {
            fields = fieldMappingRepository.findAll();
        }

        return ResponseEntity.ok(fields);
    }

    /**
     * POST /api/migration/fields - Create a new field mapping
     */
    @PostMapping
    @Operation(summary = "Create field mapping", description = "Creates a new field mapping")
    public ResponseEntity<FieldMapping> createField(
            @RequestBody FieldMapping fieldMapping,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        log.info("Creating field mapping: name={}, type={}",
                fieldMapping.getMappingName(), fieldMapping.getMappingType());

        if (userId != null) {
            fieldMapping.setCreatedBy(userId);
        }

        FieldMapping saved = fieldMappingRepository.save(fieldMapping);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * GET /api/migration/fields/{id} - Get field mapping by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get field mapping", description = "Returns a specific field mapping")
    public ResponseEntity<FieldMapping> getField(
            @Parameter(description = "Field Mapping ID") @PathVariable UUID id) {

        log.info("Getting field mapping: id={}", id);

        return fieldMappingRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/migration/fields/{id} - Update field mapping
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update field mapping", description = "Updates an existing field mapping")
    public ResponseEntity<FieldMapping> updateField(
            @Parameter(description = "Field Mapping ID") @PathVariable UUID id,
            @RequestBody FieldMapping updates) {

        log.info("Updating field mapping: id={}", id);

        return fieldMappingRepository.findById(id)
                .map(existing -> {
                    if (updates.getMappingName() != null) {
                        existing.setMappingName(updates.getMappingName());
                    }
                    if (updates.getMappingType() != null) {
                        existing.setMappingType(updates.getMappingType());
                    }
                    if (updates.getSourceType() != null) {
                        existing.setSourceType(updates.getSourceType());
                    }
                    if (updates.getTargetType() != null) {
                        existing.setTargetType(updates.getTargetType());
                    }
                    if (updates.getMappings() != null) {
                        existing.setMappings(updates.getMappings());
                    }
                    if (updates.getSampleData() != null) {
                        existing.setSampleData(updates.getSampleData());
                    }

                    FieldMapping saved = fieldMappingRepository.save(existing);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/migration/fields/{id} - Delete field mapping
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete field mapping", description = "Deletes a field mapping")
    public ResponseEntity<Void> deleteField(
            @Parameter(description = "Field Mapping ID") @PathVariable UUID id) {

        log.info("Deleting field mapping: id={}", id);

        if (fieldMappingRepository.existsById(id)) {
            fieldMappingRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}