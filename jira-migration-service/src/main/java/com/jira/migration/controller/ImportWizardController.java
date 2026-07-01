package com.jira.migration.controller;

import com.jira.migration.dto.MigrationJobResponse;
import com.jira.migration.dto.ValidationResult;
import com.jira.migration.dto.wizard.*;
import com.jira.migration.entity.FieldMapping;
import com.jira.migration.entity.UserMapping;
import com.jira.migration.repository.FieldMappingRepository;
import com.jira.migration.repository.UserMappingRepository;
import com.jira.migration.dto.FieldDiscoveryResponse;
import com.jira.migration.dto.FieldDefinitionResponse;
import com.jira.migration.dto.FieldProvisioningResponse;
import com.jira.migration.entity.field.FieldDefinition;
import com.jira.migration.service.ImportWizardSessionService;
import com.jira.migration.service.ValidationReportService;
import com.jira.migration.service.field.FieldDiscoveryService;
import com.jira.migration.service.field.FieldProvisioningService;
import com.jira.migration.service.field.FieldScreenConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Import Wizard API — persisted multi-step sessions (Phase 2).
 */
@RestController
@RequestMapping("/api/migration/wizard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Import Wizard", description = "Persisted migration import wizard")
public class ImportWizardController {

    private final ImportWizardSessionService wizardSessionService;
    private final ValidationReportService validationReportService;
    private final FieldMappingRepository fieldMappingRepository;
    private final UserMappingRepository userMappingRepository;
    private final FieldDiscoveryService fieldDiscoveryService;
    private final FieldProvisioningService fieldProvisioningService;
    private final FieldScreenConfigurationService fieldScreenConfigurationService;

    @PostMapping("/sessions")
    @Operation(summary = "Create wizard session")
    public ResponseEntity<WizardSessionDto> createSession(
            @RequestBody CreateWizardSessionRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        return ResponseEntity.ok(wizardSessionService.createSession(request, actor));
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get wizard session state")
    public ResponseEntity<WizardSessionDto> getSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(wizardSessionService.getSession(sessionId));
    }

    @PatchMapping("/sessions/{sessionId}")
    @Operation(summary = "Update wizard session (step, mappings, options)")
    public ResponseEntity<WizardSessionDto> updateSession(
            @PathVariable UUID sessionId,
            @RequestBody UpdateWizardSessionRequest request) {
        return ResponseEntity.ok(wizardSessionService.updateSession(sessionId, request));
    }

    @PostMapping("/sessions/{sessionId}/upload")
    @Operation(summary = "Upload file to wizard session")
    public ResponseEntity<WizardUploadResultDto> uploadFile(
            @PathVariable UUID sessionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String importType,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        return ResponseEntity.ok(wizardSessionService.uploadFile(sessionId, file, importType, actor));
    }

    @GetMapping("/sessions/{sessionId}/preview")
    @Operation(summary = "Preview parsed rows from uploaded file")
    public ResponseEntity<WizardSessionDto> getPreview(
            @PathVariable UUID sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(wizardSessionService.getPreview(sessionId, page, size));
    }

    @PostMapping("/sessions/{sessionId}/validate")
    @Operation(summary = "Run validation on uploaded file")
    public ResponseEntity<ValidationResult> validateSession(
            @PathVariable UUID sessionId,
            @RequestParam(required = false) String entityType) {
        ValidationResult result = wizardSessionService.validateSession(sessionId, entityType);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sessions/{sessionId}/field-mappings")
    public ResponseEntity<List<FieldMapping>> getFieldMappings(
            @PathVariable UUID sessionId,
            @RequestParam(defaultValue = "CSV") String sourceType) {
        return ResponseEntity.ok(fieldMappingRepository.findBySourceTypeAndTargetType(sourceType, "JIRA_PLATFORM"));
    }

    @PostMapping("/sessions/{sessionId}/fields/discover")
    @Operation(summary = "Discover CSV/XML headers against platform fields (Jira DC field mapping step)")
    public ResponseEntity<FieldDiscoveryResponse> discoverSessionFields(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(toDiscoveryResponse(discoverHeadersForSession(sessionId)));
    }

    @PostMapping("/sessions/{sessionId}/fields/provision-missing")
    @Operation(summary = "Auto-create custom fields for unmapped CSV columns (Jira DC External Import behavior)")
    public ResponseEntity<FieldProvisioningResponse> provisionMissingSessionFields(
            @PathVariable UUID sessionId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        fieldProvisioningService.initializeBuiltInFields(actor);
        FieldDiscoveryService.FieldDiscoveryResult discovery = discoverHeadersForSession(sessionId);
        List<FieldDiscoveryService.DiscoveredField> toProvision = discovery.discoveredFields().stream()
                .filter(f -> f.requiresProvisioning() && !f.isKnown())
                .toList();
        if (toProvision.isEmpty()) {
            return ResponseEntity.ok(FieldProvisioningResponse.builder()
                    .provisionedFields(List.of())
                    .existingFields(List.of())
                    .failedFields(List.of())
                    .fieldKeyMapping(Map.of())
                    .totalProvisioned(0)
                    .totalExisting(0)
                    .totalFailed(0)
                    .build());
        }
        FieldProvisioningService.ProvisioningResult result =
                fieldProvisioningService.provisionFields(toProvision, actor);
        WizardSessionDto session = wizardSessionService.getSession(sessionId);
        UUID projectId = session.getTargetProjectId();
        if (projectId != null) {
            fieldScreenConfigurationService.ensureAllCustomFieldsOnScreen(projectId);
        } else {
            fieldScreenConfigurationService.ensureAllCustomFieldsOnScreen(null);
        }
        return ResponseEntity.ok(toProvisioningResponse(result));
    }

    private FieldDiscoveryService.FieldDiscoveryResult discoverHeadersForSession(UUID sessionId) {
        WizardSessionDto session = wizardSessionService.getSession(sessionId);
        List<String> headers = session.getDetectedHeaders() != null ? session.getDetectedHeaders() : List.of();
        Map<String, Object> payload = new LinkedHashMap<>();
        for (String header : headers) {
            payload.put(header, "");
        }
        return fieldDiscoveryService.discoverFields(payload);
    }

    private FieldDiscoveryResponse toDiscoveryResponse(FieldDiscoveryService.FieldDiscoveryResult result) {
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
        return FieldDiscoveryResponse.builder()
                .discoveredFields(discoveredFields)
                .standardFieldCount(result.standardFields().size())
                .agileFieldCount(result.agileFields().size())
                .pluginFieldCount(result.pluginFields().size())
                .unknownFieldCount(result.unknownFields().size())
                .missingFieldKeys(result.missingFieldKeys())
                .fieldGroupings(result.fieldGroupings())
                .build();
    }

    private FieldProvisioningResponse toProvisioningResponse(FieldProvisioningService.ProvisioningResult result) {
        return FieldProvisioningResponse.builder()
                .provisionedFields(result.provisionedFields().stream()
                        .map(FieldDefinitionResponse::fromEntity)
                        .toList())
                .existingFields(result.existingFields().stream()
                        .map(FieldDefinitionResponse::fromEntity)
                        .toList())
                .failedFields(result.failedFields().stream()
                        .filter(Objects::nonNull)
                        .map(FieldDefinition::getFieldKey)
                        .collect(Collectors.toList()))
                .fieldKeyMapping(result.fieldKeyMapping())
                .totalProvisioned(result.totalProvisioned())
                .totalExisting(result.existingFields().size())
                .totalFailed(result.failedFields().size())
                .build();
    }

    @PatchMapping("/sessions/{sessionId}/field-mappings")
    @Operation(summary = "Save wizard field mappings to session")
    public ResponseEntity<WizardSessionDto> saveWizardFieldMappings(
            @PathVariable UUID sessionId,
            @RequestBody List<Map<String, Object>> mappings) {
        UpdateWizardSessionRequest update = UpdateWizardSessionRequest.builder()
                .fieldMappings(mappings)
                .step("MAP_FIELDS")
                .build();
        return ResponseEntity.ok(wizardSessionService.updateSession(sessionId, update));
    }

    @GetMapping("/sessions/{sessionId}/user-mappings")
    public ResponseEntity<List<UserMapping>> getUserMappings(@PathVariable UUID sessionId) {
        WizardSessionDto session = wizardSessionService.getSession(sessionId);
        if (session.getMigrationJobId() != null) {
            return ResponseEntity.ok(userMappingRepository.findByJobId(session.getMigrationJobId()));
        }
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/sessions/{sessionId}/validation-report")
    @Operation(summary = "Download persisted dry-run validation CSV")
    public ResponseEntity<String> downloadSessionValidationReport(@PathVariable UUID sessionId) {
        String csv = validationReportService.buildValidationReportCsv(null, sessionId);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"validation-report-" + sessionId + ".csv\"")
                .body(csv);
    }

    @PostMapping("/sessions/{sessionId}/execute")
    @Operation(summary = "Execute import from wizard session")
    public ResponseEntity<MigrationJobResponse> executeImport(
            @PathVariable UUID sessionId,
            @RequestBody(required = false) WizardExecuteImportRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        UUID actor = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        WizardExecuteImportRequest req = request != null ? request : new WizardExecuteImportRequest();
        MigrationJobResponse job = wizardSessionService.executeImport(sessionId, req, actor);
        return ResponseEntity.accepted().body(job);
    }
}
