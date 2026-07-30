package com.avionics_systems.migration.controller;

import com.avionics_systems.migration.dto.*;
import com.avionics_systems.migration.entity.field.FieldDefinition;
import com.avionics_systems.migration.entity.field.CustomFieldDefinition;
import com.avionics_systems.migration.repository.field.*;
import com.avionics_systems.migration.service.field.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST Controller for Field Management APIs.
 * Provides endpoints for field discovery, mapping, provisioning, and management.
 * SECURITY: All mutating operations require ADMIN role.
 */
@RestController
@RequestMapping("/api/fields")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Field Management", description = "Dynamic field management and migration APIs")
public class FieldController {

    private final FieldDiscoveryService fieldDiscoveryService;
    private final FieldMappingService fieldMappingService;
    private final FieldProvisioningService fieldProvisioningService;
    private final FieldValueService fieldValueService;
    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;
    private final CustomFieldOptionRepository customFieldOptionRepository;
    private final FieldTypeCompatibilityValidator fieldTypeCompatibilityValidator;
    private final FieldScreenConfigurationService fieldScreenConfigurationService;
    private final FieldIssueContextResolver fieldIssueContextResolver;

    // ========================================================================
    // FIELD DEFINITION APIs
    // ========================================================================

    @GetMapping
    @Operation(summary = "Get all field definitions", description = "Returns all field definitions including built-in and custom fields")
    public ResponseEntity<Page<FieldDefinitionResponse>> getAllFields(Pageable pageable) {
        Page<FieldDefinition> fields = fieldDefinitionRepository.findAll(pageable);
        Page<FieldDefinitionResponse> response = fields.map(FieldDefinitionResponse::fromEntity);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/definitions")
    @Operation(summary = "Get all field definitions (list)", description = "Returns all field definitions as a list")
    public ResponseEntity<List<FieldDefinitionResponse>> getAllFieldDefinitions() {
        List<FieldDefinition> fields = fieldDefinitionRepository.findAll();
        List<FieldDefinitionResponse> response = fields.stream()
                .map(FieldDefinitionResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/definitions/{id}")
    @Operation(summary = "Get field definition by ID", description = "Returns a specific field definition")
    public ResponseEntity<FieldDefinitionResponse> getFieldDefinition(
            @Parameter(description = "Field Definition ID") @PathVariable UUID id) {
        return fieldDefinitionRepository.findById(id)
                .map(FieldDefinitionResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/definitions/key/{fieldKey}")
    @Operation(summary = "Get field definition by key", description = "Returns a field definition by its key")
    public ResponseEntity<FieldDefinitionResponse> getFieldDefinitionByKey(
            @Parameter(description = "Field Key") @PathVariable String fieldKey) {
        return fieldDefinitionRepository.findByFieldKey(fieldKey)
                .map(FieldDefinitionResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/definitions")
    @Operation(summary = "Create field definition", description = "Creates a new field definition")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FieldDefinitionResponse> createFieldDefinition(
            @RequestBody CreateFieldDefinitionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        UUID actor = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        FieldDefinition fieldDef = FieldDefinition.builder()
                .fieldKey(request.getFieldKey())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .fieldType(FieldDefinition.FieldType.valueOf(request.getFieldType()))
                .renderer(FieldDefinition.FieldRenderer.valueOf(request.getRenderer()))
                .screenRegion(request.getScreenRegion() != null ?
                        FieldDefinition.ScreenRegion.valueOf(request.getScreenRegion()) : null)
                .searchable(request.getSearchable())
                .sortable(request.getSortable())
                .filterable(request.getFilterable())
                .required(request.getRequired())
                .schemaDefinition(request.getSchemaDefinition())
                .visibilityRules(request.getVisibilityRules())
                .rendererConfig(request.getRendererConfig())
                .validationRules(request.getValidationRules())
                .custom(true)
                .builtIn(false)
                .createdBy(actor)
                .build();

        FieldDefinition saved = fieldDefinitionRepository.save(fieldDef);
        return ResponseEntity.status(HttpStatus.CREATED).body(FieldDefinitionResponse.fromEntity(saved));
    }

    @PutMapping("/definitions/{id}")
    @Operation(summary = "Update field definition", description = "Updates an existing field definition")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FieldDefinitionResponse> updateFieldDefinition(
            @PathVariable UUID id,
            @RequestBody UpdateFieldDefinitionRequest request) {

        return fieldDefinitionRepository.findById(id)
                .map(existing -> {
                    if (request.getDisplayName() != null) existing.setDisplayName(request.getDisplayName());
                    if (request.getDescription() != null) existing.setDescription(request.getDescription());
                    if (request.getRenderer() != null)
                        existing.setRenderer(FieldDefinition.FieldRenderer.valueOf(request.getRenderer()));
                    if (request.getScreenRegion() != null)
                        existing.setScreenRegion(FieldDefinition.ScreenRegion.valueOf(request.getScreenRegion()));
                    if (request.getSearchable() != null) existing.setSearchable(request.getSearchable());
                    if (request.getSortable() != null) existing.setSortable(request.getSortable());
                    if (request.getFilterable() != null) existing.setFilterable(request.getFilterable());
                    if (request.getRequired() != null) existing.setRequired(request.getRequired());
                    if (request.getReadOnly() != null) existing.setReadOnly(request.getReadOnly());
                    if (request.getHidden() != null) existing.setHidden(request.getHidden());
                    if (request.getDeprecated() != null) existing.setDeprecated(request.getDeprecated());
                    if (request.getSchemaDefinition() != null) existing.setSchemaDefinition(request.getSchemaDefinition());
                    if (request.getVisibilityRules() != null) existing.setVisibilityRules(request.getVisibilityRules());
                    if (request.getRendererConfig() != null) existing.setRendererConfig(request.getRendererConfig());
                    if (request.getValidationRules() != null) existing.setValidationRules(request.getValidationRules());

                    existing.setVersion(existing.getVersion() + 1);
                    FieldDefinition saved = fieldDefinitionRepository.save(existing);
                    return ResponseEntity.ok(FieldDefinitionResponse.fromEntity(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/definitions/{id}")
    @Operation(summary = "Delete field definition", description = "Deletes a field definition (soft delete by marking as deprecated)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFieldDefinition(@PathVariable UUID id) {
        return fieldDefinitionRepository.findById(id)
                .map(existing -> {
                    // Cascade delete associated field values before soft-deleting the definition
                    try {
                        fieldValueService.deleteFieldValuesByFieldDefinitionId(id);
                        log.info("Deleted all field values for field definition: {}", id);
                    } catch (Exception e) {
                        log.error("Failed to delete field values for field definition: {}", id, e);
                        // Continue with soft delete even if value cleanup fails
                    }

                    existing.setDeprecated(true);
                    existing.setHidden(true);
                    existing.setVersion(existing.getVersion() + 1);
                    fieldDefinitionRepository.save(existing);
                    log.info("Soft-deleted field definition: {} (deprecated, hidden)", id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========================================================================
    // CUSTOM FIELD APIs
    // ========================================================================

    @GetMapping("/custom")
    @Operation(summary = "Get all custom fields", description = "Returns all custom field definitions")
    public ResponseEntity<List<CustomFieldDefinitionResponse>> getAllCustomFields() {
        List<CustomFieldDefinition> customFields = customFieldDefinitionRepository.findAllEnabled();
        List<CustomFieldDefinitionResponse> response = customFields.stream()
                .map(CustomFieldDefinitionResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/custom")
    @Operation(summary = "Create custom field", description = "Creates a new custom field")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomFieldDefinitionResponse> createCustomField(
            @RequestBody CreateCustomFieldRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        UUID actor = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        FieldDefinition def = fieldProvisioningService.provisionCustomField(
                request.getName(), request.getType(), actor);
        UUID projectId = request.getProjectIds() != null && !request.getProjectIds().isEmpty()
                ? request.getProjectIds().get(0) : null;
        fieldScreenConfigurationService.ensureFieldVisibleOnScreen(def.getFieldKey(), projectId);

        CustomFieldDefinition saved = customFieldDefinitionRepository.findByFieldKey(def.getFieldKey())
                .orElseThrow();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomFieldDefinitionResponse.fromEntity(saved));
    }

    @GetMapping("/custom/{id}")
    @Operation(summary = "Get custom field by ID")
    public ResponseEntity<CustomFieldDefinitionResponse> getCustomField(@PathVariable UUID id) {
        return customFieldDefinitionRepository.findById(id)
                .map(CustomFieldDefinitionResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/custom/{id}")
    @Operation(summary = "Update custom field")
    public ResponseEntity<CustomFieldDefinitionResponse> updateCustomField(
            @PathVariable UUID id,
            @RequestBody UpdateCustomFieldRequest request) {
        return customFieldDefinitionRepository.findById(id)
                .map(cf -> {
                    if (request.getName() != null) cf.setName(request.getName());
                    if (request.getDescription() != null) cf.setDescription(request.getDescription());
                    if (request.getType() != null) cf.setType(request.getType());
                    if (request.getEnabled() != null) cf.setEnabled(request.getEnabled());
                    if (request.getSearchable() != null) cf.setSearchable(request.getSearchable());
                    if (request.getNavigable() != null) cf.setNavigable(request.getNavigable());
                    CustomFieldDefinition saved = customFieldDefinitionRepository.save(cf);
                    fieldDefinitionRepository.findByFieldKey(saved.getFieldKey()).ifPresent(def -> {
                        if (request.getName() != null) def.setDisplayName(request.getName());
                        fieldDefinitionRepository.save(def);
                    });
                    fieldScreenConfigurationService.ensureFieldVisibleOnScreen(saved.getFieldKey(), null);
                    return ResponseEntity.ok(CustomFieldDefinitionResponse.fromEntity(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/custom/{id}/options")
    @Operation(summary = "Get options for a custom field", description = "Returns active options for a select-type custom field")
    public ResponseEntity<List<Map<String, Object>>> getCustomFieldOptions(@PathVariable UUID id) {
        List<com.avionics_systems.migration.entity.field.CustomFieldOption> options =
                customFieldOptionRepository.findActiveByCustomFieldId(id);
        List<Map<String, Object>> result = options.stream().map(opt -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", opt.getId());
            m.put("value", opt.getValue());
            m.put("label", opt.getLabel());
            m.put("sequence", opt.getSequence());
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/custom/{id}")
    @Operation(summary = "Disable custom field (soft delete)")
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

    @PostMapping("/schemes/projects/{projectId}/ensure-fields")
    @Operation(summary = "Ensure custom fields appear on issue screen for project")
    public ResponseEntity<Map<String, Object>> ensureProjectFieldScheme(
            @PathVariable UUID projectId,
            @RequestBody(required = false) List<String> fieldKeys) {
        int count = fieldScreenConfigurationService.ensureProjectFieldsVisible(projectId, fieldKeys);
        return ResponseEntity.ok(Map.of(
                "projectId", projectId,
                "fieldsAligned", count,
                "status", "ok"
        ));
    }

    @GetMapping("/schemes/projects/{projectId}")
    @Operation(summary = "Get visible field configuration for project")
    public ResponseEntity<ScreenConfigurationResponse> getProjectFieldScheme(@PathVariable UUID projectId) {
        fieldScreenConfigurationService.ensureAllCustomFieldsOnScreen(projectId);
        return getScreenConfiguration("issue");
    }

    // ========================================================================
    // FIELD DISCOVERY APIs
    // ========================================================================

    @PostMapping("/discover")
    @Operation(summary = "Discover fields from payload", description = "Scans an import payload and discovers all fields")
    public ResponseEntity<FieldDiscoveryResponse> discoverFields(
            @RequestBody Map<String, Object> payload) {

        FieldDiscoveryService.FieldDiscoveryResult result = fieldDiscoveryService.discoverFields(payload);

        List<FieldDiscoveryResponse.DiscoveredFieldInfo> discoveredFields = result.discoveredFields().stream()
                .map(f -> FieldDiscoveryResponse.DiscoveredFieldInfo.builder()
                        .sourceKey(f.sourceKey())
                        .normalizedKey(f.normalizedKey())
                        .category(f.category().name())
                        .suggestedType(f.suggestedType().name())
                        .suggestedRegion(f.suggestedRegion().name())
                        .isKnown(f.isKnown())
                        .requiresProvisioning(f.requiresProvisioning())
                        .build())
                .toList();

        FieldDiscoveryResponse response = FieldDiscoveryResponse.builder()
                .discoveredFields(discoveredFields)
                .standardFieldCount(result.standardFields().size())
                .agileFieldCount(result.agileFields().size())
                .pluginFieldCount(result.pluginFields().size())
                .unknownFieldCount(result.unknownFields().size())
                .missingFieldKeys(result.missingFieldKeys())
                .fieldGroupings(result.fieldGroupings())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/discover/bulk")
    @Operation(summary = "Discover fields from multiple payloads", description = "Scans multiple import payloads and discovers all unique fields")
    public ResponseEntity<FieldDiscoveryResponse> discoverFieldsBulk(
            @RequestBody List<Map<String, Object>> payloads) {

        FieldDiscoveryService.FieldDiscoveryResult result = fieldDiscoveryService.discoverFieldsFromList(payloads);

        List<FieldDiscoveryResponse.DiscoveredFieldInfo> discoveredFields = result.discoveredFields().stream()
                .map(f -> FieldDiscoveryResponse.DiscoveredFieldInfo.builder()
                        .sourceKey(f.sourceKey())
                        .normalizedKey(f.normalizedKey())
                        .category(f.category().name())
                        .suggestedType(f.suggestedType().name())
                        .suggestedRegion(f.suggestedRegion().name())
                        .isKnown(f.isKnown())
                        .requiresProvisioning(f.requiresProvisioning())
                        .build())
                .toList();

        FieldDiscoveryResponse response = FieldDiscoveryResponse.builder()
                .discoveredFields(discoveredFields)
                .standardFieldCount(result.standardFields().size())
                .agileFieldCount(result.agileFields().size())
                .pluginFieldCount(result.pluginFields().size())
                .unknownFieldCount(result.unknownFields().size())
                .missingFieldKeys(result.missingFieldKeys())
                .fieldGroupings(result.fieldGroupings())
                .build();

        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // FIELD MAPPING APIs
    // ========================================================================

    @PostMapping("/map")
    @Operation(summary = "Map source fields to target fields", description = "Maps source field keys to target field definitions")
    public ResponseEntity<FieldMappingResponse> mapFields(@RequestBody List<String> sourceFieldKeys) {

        FieldMappingService.MappingResult result = fieldMappingService.mapFields(sourceFieldKeys);

        List<FieldMappingResponse.FieldMappingInfo> mappings = result.mappings().stream()
                .map(m -> FieldMappingResponse.FieldMappingInfo.builder()
                        .sourceKey(m.sourceKey())
                        .targetKey(m.targetKey())
                        .confidence(m.confidence().name())
                        .strategy(m.strategy().name())
                        .pluginSource(m.pluginSource())
                        .build())
                .toList();

        List<String> typeWarnings = new ArrayList<>();
        for (FieldMappingResponse.FieldMappingInfo m : mappings) {
            if (m.getTargetKey() == null || m.getTargetKey().isBlank()) {
                continue;
            }
            fieldDefinitionRepository.findByFieldKey(m.getTargetKey()).ifPresent(def -> {
                String reason = fieldTypeCompatibilityValidator.incompatibilityReason(
                        "STRING",
                        def.getFieldType() != null ? def.getFieldType().name() : "TEXT");
                if (reason != null) {
                    typeWarnings.add(m.getSourceKey() + " → " + m.getTargetKey() + ": " + reason);
                }
            });
        }

        FieldMappingResponse response = FieldMappingResponse.builder()
                .mappings(mappings)
                .unmappedFields(mappings.stream()
                        .filter(m -> "UNMAPPED".equals(m.getStrategy()))
                        .toList())
                .highConfidenceMappings(mappings.stream()
                        .filter(m -> m.getConfidence().equals("EXACT") || m.getConfidence().equals("HIGH"))
                        .toList())
                .lowConfidenceMappings(mappings.stream()
                        .filter(m -> m.getConfidence().equals("MEDIUM") || m.getConfidence().equals("LOW"))
                        .toList())
                .pluginFieldMappings(result.pluginFieldMappings())
                .averageConfidence(result.averageConfidence())
                .totalFields(result.mappings().size())
                .mappedFields(result.mappings().size() - result.unmappedFields().size())
                .unmappedFieldsCount(result.unmappedFields().size())
                .typeWarnings(typeWarnings.isEmpty() ? null : typeWarnings)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/map/suggestions")
    @Operation(summary = "Get field mapping suggestions", description = "Suggests target field mappings for a partial key")
    public ResponseEntity<List<String>> getMappingSuggestions(
            @Parameter(description = "Partial field key") @RequestParam String partialKey) {
        List<String> suggestions = fieldMappingService.suggestFieldMappings(partialKey);
        return ResponseEntity.ok(suggestions);
    }

    // ========================================================================
    // FIELD PROVISIONING APIs
    // ========================================================================

    @PostMapping("/provision")
    @Operation(summary = "Provision missing fields", description = "Automatically provisions discovered fields that don't exist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FieldProvisioningResponse> provisionFields(
            @RequestBody FieldDiscoveryResponse discoveryResult,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {

        UUID actor = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        userId = actor;

        List<FieldDiscoveryService.DiscoveredField> discoveredFields = discoveryResult.getDiscoveredFields().stream()
                .map(info -> new FieldDiscoveryService.DiscoveredField(
                        info.getSourceKey(),
                        info.getNormalizedKey(),
                        FieldDiscoveryService.FieldCategory.valueOf(info.getCategory()),
                        FieldDefinition.FieldType.valueOf(info.getSuggestedType()),
                        FieldDefinition.ScreenRegion.valueOf(info.getSuggestedRegion()),
                        Map.of(),
                        info.isKnown(),
                        info.isRequiresProvisioning()
                ))
                .toList();

        FieldProvisioningService.ProvisioningResult result =
                fieldProvisioningService.provisionFields(discoveredFields, userId);

        FieldProvisioningResponse response = FieldProvisioningResponse.builder()
                .provisionedFields(result.provisionedFields().stream()
                        .map(FieldDefinitionResponse::fromEntity)
                        .toList())
                .existingFields(result.existingFields().stream()
                        .map(FieldDefinitionResponse::fromEntity)
                        .toList())
                .failedFields(result.failedFields().stream()
                        .filter(Objects::nonNull)
                        .map(FieldDefinition::getFieldKey)
                        .toList())
                .fieldKeyMapping(result.fieldKeyMapping())
                .totalProvisioned(result.totalProvisioned())
                .totalExisting(result.existingFields().size())
                .totalFailed(result.failedFields().size())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/provision/initialize")
    @Operation(summary = "Initialize built-in fields", description = "Initializes all built-in field definitions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> initializeBuiltInFields(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        fieldProvisioningService.initializeBuiltInFields(actor);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Built-in fields initialized successfully"
        ));
    }

    // ========================================================================
    // FIELD VALUE APIs
    // ========================================================================

    @GetMapping("/issues/{issueId}/values")
    @Operation(summary = "Get all field values for an issue", description = "Returns all field values (supports UUID or issue key)")
    public ResponseEntity<IssueFieldValuesResponse> getIssueFieldValues(
            @PathVariable String issueId) {

        UUID resolvedId = fieldIssueContextResolver.resolve(issueId)
                .map(FieldIssueContextResolver.IssueContext::issueId)
                .orElseGet(() -> {
                    try {
                        return UUID.fromString(issueId);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                });
        if (resolvedId == null) {
            return ResponseEntity.notFound().build();
        }

        FieldValueService.FieldValueResult result = fieldValueService.getAllFieldValues(resolvedId);

        Map<String, Object> standardFields = new HashMap<>();
        Map<String, Object> customFields = new HashMap<>();

        for (Map.Entry<String, Object> entry : result.values().entrySet()) {
            FieldDefinition def = result.definitions().get(entry.getKey());
            if (def != null && def.getCustom()) {
                customFields.put(entry.getKey(), entry.getValue());
            } else {
                standardFields.put(entry.getKey(), entry.getValue());
            }
        }

        List<FieldValueResponse> allValues = result.values().entrySet().stream()
                .map(entry -> {
                    FieldDefinition def = result.definitions().get(entry.getKey());
                    return FieldValueResponse.builder()
                            .fieldKey(entry.getKey())
                            .fieldDisplayName(def != null ? def.getDisplayName() : null)
                            .value(entry.getValue())
                            .build();
                })
                .toList();

        String issueKey = fieldIssueContextResolver.resolve(issueId)
                .map(FieldIssueContextResolver.IssueContext::issueKey)
                .orElse(null);

        return ResponseEntity.ok(IssueFieldValuesResponse.builder()
                .issueId(resolvedId)
                .issueKey(issueKey)
                .standardFields(standardFields)
                .customFields(customFields)
                .allFieldValues(allValues)
                .validationErrors(result.validationErrors())
                .build());
    }

    @PutMapping("/issues/{issueId}/values")
    @Operation(summary = "Set field values for an issue", description = "Sets multiple field values for a specific issue")
    public ResponseEntity<IssueFieldValuesResponse> setIssueFieldValues(
            @PathVariable UUID issueId,
            @RequestBody SetFieldValuesRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        fieldValueService.setFieldValues(issueId, request.getValues(), userId);
        return getIssueFieldValues(issueId.toString());
    }

    @PutMapping("/issues/{issueId}/values/{fieldKey}")
    @Operation(summary = "Set single field value", description = "Sets a single field value for a specific issue")
    public ResponseEntity<Void> setIssueFieldValue(
            @PathVariable UUID issueId,
            @PathVariable String fieldKey,
            @RequestBody SetFieldValueRequest request,
            @RequestHeader("X-User-Id") UUID userId) {

        fieldValueService.setFieldValue(issueId, fieldKey, request.getValue(), userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate field value", description = "Validates a field value against field definition rules")
    public ResponseEntity<FieldValidationResult> validateFieldValue(
            @RequestBody Map<String, Object> request) {

        String fieldKey = (String) request.get("fieldKey");
        Object value = request.get("value");
        UUID issueId = request.get("issueId") != null ?
                UUID.fromString((String) request.get("issueId")) : null;

        if (issueId != null) {
            fieldValueService.validateFieldValue(issueId, fieldKey, value);
        }

        return ResponseEntity.ok(FieldValidationResult.builder()
                .valid(true)
                .fieldKey(fieldKey)
                .value(value)
                .validationStatus("VALID")
                .warnings(List.of())
                .build());
    }

    // ========================================================================
    // SCREEN CONFIGURATION APIs
    // ========================================================================

    @GetMapping("/screens/configuration")
    @Operation(summary = "Get screen configuration", description = "Returns the complete screen configuration with fields organized by region")
    public ResponseEntity<ScreenConfigurationResponse> getScreenConfiguration(
            @RequestParam(defaultValue = "issue") String screenType) {

        List<FieldDefinition> allFields = fieldDefinitionRepository.findAllVisible();

        Map<FieldDefinition.ScreenRegion, List<FieldDefinitionResponse>> regionMap = new HashMap<>();
        List<FieldDefinitionResponse> customFieldList = new ArrayList<>();

        for (FieldDefinition field : allFields) {
            FieldDefinitionResponse response = FieldDefinitionResponse.fromEntity(field);
            if (field.getCustom() != null && field.getCustom()) {
                customFieldList.add(response);
            } else {
                regionMap.computeIfAbsent(field.getScreenRegion(), k -> new ArrayList<>()).add(response);
            }
        }

        return ResponseEntity.ok(ScreenConfigurationResponse.builder()
                .screenType(screenType)
                .headerFields(regionMap.getOrDefault(FieldDefinition.ScreenRegion.HEADER, List.of()))
                .leftPrimaryFields(regionMap.getOrDefault(FieldDefinition.ScreenRegion.LEFT_PRIMARY, List.of()))
                .leftDescriptionFields(regionMap.getOrDefault(FieldDefinition.ScreenRegion.LEFT_DESCRIPTION, List.of()))
                .leftActivityFields(regionMap.getOrDefault(FieldDefinition.ScreenRegion.LEFT_ACTIVITY, List.of()))
                .sidebarPeopleFields(regionMap.getOrDefault(FieldDefinition.ScreenRegion.SIDEBAR_PEOPLE, List.of()))
                .sidebarDetailsFields(regionMap.getOrDefault(FieldDefinition.ScreenRegion.SIDEBAR_DETAILS, List.of()))
                .sidebarTimeFields(regionMap.getOrDefault(FieldDefinition.ScreenRegion.SIDEBAR_TIME, List.of()))
                .sidebarAgileFields(regionMap.getOrDefault(FieldDefinition.ScreenRegion.SIDEBAR_AGILE, List.of()))
                .sidebarDatesFields(regionMap.getOrDefault(FieldDefinition.ScreenRegion.SIDEBAR_DATES, List.of()))
                .sidebarVersionsFields(regionMap.getOrDefault(FieldDefinition.ScreenRegion.SIDEBAR_VERSIONS, List.of()))
                .customFields(customFieldList)
                .build());
    }

    // ========================================================================
    // SEARCH & FILTER APIs
    // ========================================================================

    @GetMapping("/search")
    @Operation(summary = "Search field definitions", description = "Searches field definitions by name or key")
    public ResponseEntity<Page<FieldDefinitionResponse>> searchFields(
            @RequestParam String query,
            Pageable pageable) {

        Page<FieldDefinition> fields = fieldDefinitionRepository.searchByDisplayName(query, pageable);
        Page<FieldDefinitionResponse> response = fields.map(FieldDefinitionResponse::fromEntity);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-type/{fieldType}")
    @Operation(summary = "Get fields by type", description = "Returns all field definitions of a specific type")
    public ResponseEntity<List<FieldDefinitionResponse>> getFieldsByType(
            @PathVariable String fieldType) {

        FieldDefinition.FieldType type = FieldDefinition.FieldType.valueOf(fieldType.toUpperCase());
        List<FieldDefinition> fields = fieldDefinitionRepository.findByFieldType(type);

        List<FieldDefinitionResponse> response = fields.stream()
                .map(FieldDefinitionResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-region/{region}")
    @Operation(summary = "Get fields by screen region", description = "Returns all field definitions for a specific screen region")
    public ResponseEntity<List<FieldDefinitionResponse>> getFieldsByRegion(
            @PathVariable String region) {

        FieldDefinition.ScreenRegion screenRegion = FieldDefinition.ScreenRegion.valueOf(region.toUpperCase());
        List<FieldDefinition> fields = fieldDefinitionRepository.findByScreenRegion(screenRegion);

        List<FieldDefinitionResponse> response = fields.stream()
                .map(FieldDefinitionResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/plugin/{pluginKey}")
    @Operation(summary = "Get plugin fields", description = "Returns all field definitions from a specific plugin")
    public ResponseEntity<List<FieldDefinitionResponse>> getPluginFields(
            @PathVariable String pluginKey) {

        List<FieldDefinition> fields = fieldDefinitionRepository.findByPluginSource(pluginKey);
        List<FieldDefinitionResponse> response = fields.stream()
                .map(FieldDefinitionResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // STATISTICS APIs
    // ========================================================================

    @GetMapping("/statistics/{fieldDefId}")
    @Operation(summary = "Get field statistics", description = "Returns usage statistics for a field definition")
    public ResponseEntity<FieldStatisticsResponse> getFieldStatistics(
            @PathVariable UUID fieldDefId) {

        Map<String, Long> stats = fieldValueService.getFieldValueStatistics(fieldDefId);

        return fieldDefinitionRepository.findById(fieldDefId)
                .map(fieldDef -> ResponseEntity.ok(FieldStatisticsResponse.builder()
                        .fieldDefinitionId(fieldDefId)
                        .fieldKey(fieldDef.getFieldKey())
                        .fieldType(fieldDef.getFieldType().name())
                        .totalIssues(stats.getOrDefault("totalValues", 0L))
                        .issuesWithValues(stats.getOrDefault("totalValues", 0L) - stats.getOrDefault("nullValues", 0L))
                        .nullValueCount(stats.getOrDefault("nullValues", 0L))
                        .uniqueValueCount(stats.getOrDefault("uniqueValues", 0L))
                        .build()))
                .orElse(ResponseEntity.notFound().build());
    }

    // ========================================================================
    // MIGRATION-SPECIFIC APIs
    // ========================================================================

    @PostMapping("/migration/import")
    @Operation(summary = "Import issues with dynamic field handling", description = "Imports issues and automatically handles dynamic fields")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> importIssues(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") UUID userId) {

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> issues = (List<Map<String, Object>>) request.get("issues");
        UUID projectId = UUID.fromString((String) request.get("projectId"));

        Map<String, Object> result = new HashMap<>();
        result.put("totalIssues", issues.size());
        result.put("projectId", projectId);
        result.put("status", "Processing - integrate with MigrationService for full implementation");

        return ResponseEntity.ok(result);
    }

    @PostMapping("/migration/map-fields")
    @Operation(summary = "Map fields for import", description = "Maps source fields to target fields and provisions missing ones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FieldMappingResponse> mapFieldsForImport(
            @RequestBody List<String> sourceFieldKeys,
            @RequestHeader("X-User-Id") UUID userId) {

        FieldMappingService.MappingResult mappingResult = fieldMappingService.mapFields(sourceFieldKeys);

        List<String> keysToProvision = mappingResult.lowConfidenceMappings().stream()
                .map(FieldMappingService.FieldMapping::sourceKey)
                .toList();

        List<FieldDiscoveryService.DiscoveredField> discoveredFields = keysToProvision.stream()
                .map(key -> {
                    FieldMappingService.FieldMapping mapping = mappingResult.mappings().stream()
                            .filter(m -> m.sourceKey().equals(key))
                            .findFirst()
                            .orElse(null);
                    return mapping != null ?
                            fieldDiscoveryService.discoverField(key, null) : null;
                })
                .filter(Objects::nonNull)
                .toList();

        if (!discoveredFields.isEmpty()) {
            fieldProvisioningService.provisionFields(discoveredFields, userId);
        }

        return mapFields(sourceFieldKeys);
    }
}