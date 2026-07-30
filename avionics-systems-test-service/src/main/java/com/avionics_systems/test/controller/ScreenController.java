package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.*;
import com.avionics_systems.test.entity.ScreenSchemeScreen;
import com.avionics_systems.test.service.CustomFieldService;
import com.avionics_systems.test.service.FieldTypeRegistry;
import com.avionics_systems.test.service.FieldValidationService;
import com.avionics_systems.test.service.ScreenSchemeService;
import com.avionics_systems.test.service.ScreenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Screen & Field Configuration", description = "APIs for managing screens, screen schemes, and custom fields")
public class ScreenController {

    private final ScreenService screenService;
    private final ScreenSchemeService screenSchemeService;
    private final CustomFieldService customFieldService;
    private final FieldTypeRegistry fieldTypeRegistry;
    private final FieldValidationService fieldValidationService;

    // ==================== Screen Endpoints ====================

    @PostMapping("/screens")
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #request.screenType)")
    @Operation(summary = "Create a new screen")
    public ResponseEntity<ScreenResponse> createScreen(@Valid @RequestBody CreateScreenRequest request) {
        ScreenResponse screen = screenService.createScreen(request.getName(), request.getScreenType(), request.getPosition());
        return ResponseEntity.status(HttpStatus.CREATED).body(screen);
    }

    @GetMapping("/screens")
    @Operation(summary = "List all screens")
    public ResponseEntity<List<ScreenResponse>> listScreens(
            @RequestParam(required = false) String screenType) {
        List<ScreenResponse> screens;
        if (screenType != null) {
            screens = screenService.listScreensByType(screenType);
        } else {
            screens = screenService.listScreens();
        }
        return ResponseEntity.ok(screens);
    }

    @GetMapping("/screens/{screenId}")
    @Operation(summary = "Get a screen by ID")
    public ResponseEntity<ScreenResponse> getScreen(@PathVariable UUID screenId) {
        ScreenResponse screen = screenService.getScreen(screenId);
        return ResponseEntity.ok(screen);
    }

    @PutMapping("/screens/{screenId}")
    @Operation(summary = "Update a screen")
    public ResponseEntity<ScreenResponse> updateScreen(
            @PathVariable UUID screenId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer position) {
        ScreenResponse screen = screenService.updateScreen(screenId, name, position);
        return ResponseEntity.ok(screen);
    }

    @DeleteMapping("/screens/{screenId}")
    @Operation(summary = "Delete a screen")
    public ResponseEntity<Void> deleteScreen(@PathVariable UUID screenId) {
        screenService.deleteScreen(screenId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Screen Field Endpoints ====================

    @PostMapping("/screens/{screenId}/fields")
    @Operation(summary = "Add a field to a screen")
    public ResponseEntity<ScreenFieldResponse> addField(
            @PathVariable UUID screenId,
            @Valid @RequestBody ScreenFieldRequest request) {
        ScreenFieldResponse field = screenService.addField(
                screenId,
                request.getFieldId(),
                request.getPosition(),
                request.getIsRequired()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(field);
    }

    @PutMapping("/screens/{screenId}/fields")
    @Operation(summary = "Update fields on a screen (add/update multiple)")
    public ResponseEntity<List<ScreenFieldResponse>> updateFields(
            @PathVariable UUID screenId,
            @RequestBody List<ScreenFieldRequest> requests) {
        List<ScreenFieldResponse> responses = new java.util.ArrayList<>();
        for (ScreenFieldRequest request : requests) {
            if (screenService.getFieldsForScreen(screenId).stream()
                    .anyMatch(f -> f.getFieldId().equals(request.getFieldId()))) {
                responses.add(screenService.updateField(
                        screenId,
                        request.getFieldId(),
                        request.getIsRequired(),
                        request.getIsEditable(),
                        request.getIsVisible()
                ));
            } else {
                responses.add(screenService.addField(
                        screenId,
                        request.getFieldId(),
                        request.getPosition(),
                        request.getIsRequired()
                ));
            }
        }
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/screens/{screenId}/fields")
    @Operation(summary = "Get all fields for a screen")
    public ResponseEntity<List<ScreenFieldResponse>> getFieldsForScreen(@PathVariable UUID screenId) {
        List<ScreenFieldResponse> fields = screenService.getFieldsForScreen(screenId);
        return ResponseEntity.ok(fields);
    }

    @DeleteMapping("/screens/{screenId}/fields/{fieldId}")
    @Operation(summary = "Remove a field from a screen")
    public ResponseEntity<Void> removeField(
            @PathVariable UUID screenId,
            @PathVariable UUID fieldId) {
        screenService.removeField(screenId, fieldId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/screens/{screenId}/fields/order")
    @Operation(summary = "Reorder fields on a screen")
    public ResponseEntity<List<ScreenFieldResponse>> reorderFields(
            @PathVariable UUID screenId,
            @Valid @RequestBody FieldOrderRequest request) {
        List<ScreenFieldResponse> fields = screenService.reorderFields(screenId, request.getFieldOrder());
        return ResponseEntity.ok(fields);
    }

    // ==================== Screen Scheme Endpoints ====================

    @PostMapping("/screen-schemes")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #request.projectId)")
    @Operation(summary = "Create a new screen scheme")
    public ResponseEntity<ScreenSchemeResponse> createScreenScheme(
            @Valid @RequestBody CreateScreenSchemeRequest request) {
        ScreenSchemeResponse scheme = screenSchemeService.createScreenScheme(
                request.getProjectId(),
                request.getName(),
                request.getDescription(),
                request.getIsDefault()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(scheme);
    }

    @GetMapping("/screen-schemes/{schemeId}")
    @Operation(summary = "Get a screen scheme by ID")
    public ResponseEntity<ScreenSchemeResponse> getScreenScheme(@PathVariable UUID schemeId) {
        ScreenSchemeResponse scheme = screenSchemeService.getScreenScheme(schemeId);
        return ResponseEntity.ok(scheme);
    }

    @GetMapping("/screen-schemes/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "List all screen schemes for a project")
    public ResponseEntity<List<ScreenSchemeResponse>> listScreenSchemes(@PathVariable UUID projectId) {
        List<ScreenSchemeResponse> schemes = screenSchemeService.listScreenSchemes(projectId);
        return ResponseEntity.ok(schemes);
    }

    @PutMapping("/screen-schemes/{schemeId}")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #scheme.projectId)")
    @Operation(summary = "Update a screen scheme")
    public ResponseEntity<ScreenSchemeResponse> updateScreenScheme(
            @PathVariable UUID schemeId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description) {
        ScreenSchemeResponse scheme = screenSchemeService.updateScreenScheme(schemeId, name, description);
        return ResponseEntity.ok(scheme);
    }

    @DeleteMapping("/screen-schemes/{schemeId}")
    @PreAuthorize("@projectSecurity.canDeleteTests(authentication, #scheme.projectId)")
    @Operation(summary = "Delete a screen scheme")
    public ResponseEntity<Void> deleteScreenScheme(@PathVariable UUID schemeId) {
        screenSchemeService.deleteScreenScheme(schemeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/screen-schemes/{schemeId}/screens")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #scheme.projectId)")
    @Operation(summary = "Add a screen to a scheme")
    public ResponseEntity<ScreenSchemeResponse> addScreenToScheme(
            @PathVariable UUID schemeId,
            @Valid @RequestBody AddScreenToSchemeRequest request) {
        ScreenSchemeResponse scheme = screenSchemeService.addScreenToScheme(
                schemeId,
                request.getScreenId(),
                request.getScreenType()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(scheme);
    }

    @DeleteMapping("/screen-schemes/{schemeId}/screens/{screenId}")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #scheme.projectId)")
    @Operation(summary = "Remove a screen from a scheme")
    public ResponseEntity<Void> removeScreenFromScheme(
            @PathVariable UUID schemeId,
            @PathVariable UUID screenId) {
        screenSchemeService.removeScreenFromScheme(schemeId, screenId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/screen-schemes/{schemeId}/default")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #scheme.projectId)")
    @Operation(summary = "Set a screen scheme as default")
    public ResponseEntity<ScreenSchemeResponse> setDefaultScheme(@PathVariable UUID schemeId) {
        ScreenSchemeResponse scheme = screenSchemeService.setDefaultScheme(schemeId);
        return ResponseEntity.ok(scheme);
    }

    // ==================== Custom Field Endpoints ====================

    @PostMapping("/custom-fields")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #request.projectId)")
    @Operation(summary = "Create a new custom field")
    public ResponseEntity<CustomFieldResponse> createCustomField(
            @Valid @RequestBody CreateCustomFieldRequest request) {
        CustomFieldResponse field = customFieldService.createCustomField(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(field);
    }

    @GetMapping("/custom-fields/{fieldId}")
    @Operation(summary = "Get a custom field by ID")
    public ResponseEntity<CustomFieldResponse> getCustomField(@PathVariable UUID fieldId) {
        CustomFieldResponse field = customFieldService.getField(fieldId);
        return ResponseEntity.ok(field);
    }

    @GetMapping("/custom-fields")
    @Operation(summary = "List all custom fields")
    public ResponseEntity<List<CustomFieldResponse>> listCustomFields() {
        List<CustomFieldResponse> fields = customFieldService.listFields();
        return ResponseEntity.ok(fields);
    }

    @GetMapping("/custom-fields/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "List all custom fields for a project")
    public ResponseEntity<List<CustomFieldResponse>> listProjectCustomFields(@PathVariable UUID projectId) {
        List<CustomFieldResponse> fields = customFieldService.listFields(projectId);
        return ResponseEntity.ok(fields);
    }

    @PutMapping("/custom-fields/{fieldId}")
    @Operation(summary = "Update a custom field")
    public ResponseEntity<CustomFieldResponse> updateCustomField(
            @PathVariable UUID fieldId,
            @Valid @RequestBody UpdateCustomFieldRequest request) {
        CustomFieldResponse field = customFieldService.updateField(fieldId, request);
        return ResponseEntity.ok(field);
    }

    @DeleteMapping("/custom-fields/{fieldId}")
    @Operation(summary = "Delete a custom field")
    public ResponseEntity<Void> deleteCustomField(@PathVariable UUID fieldId) {
        customFieldService.deleteField(fieldId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/custom-fields/validate")
    @Operation(summary = "Validate a custom field value")
    public ResponseEntity<FieldValidationResult> validateFieldValue(
            @RequestParam UUID fieldId,
            @RequestParam(required = false) String value) {
        FieldValidationResult result = customFieldService.validateValue(fieldId, value);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/custom-fields/validate/batch")
    @Operation(summary = "Validate multiple custom field values")
    public ResponseEntity<List<FieldValidationResult>> validateFieldValues(
            @RequestBody Map<UUID, String> fieldValues) {
        List<FieldValidationResult> results = customFieldService.validateValues(fieldValues);
        return ResponseEntity.ok(results);
    }

    // ==================== Field Type Registry Endpoints ====================

    @GetMapping("/field-types")
    @Operation(summary = "Get all registered field types")
    public ResponseEntity<List<FieldTypeRegistry.FieldTypeConfig>> getAllFieldTypes() {
        return ResponseEntity.ok(fieldTypeRegistry.getAllTypes());
    }

    @GetMapping("/field-types/{type}")
    @Operation(summary = "Get field type configuration")
    public ResponseEntity<FieldTypeRegistry.FieldTypeConfig> getFieldType(
            @PathVariable String type) {
        try {
            com.avionics_systems.test.entity.CustomField.FieldType fieldType =
                    com.avionics_systems.test.entity.CustomField.FieldType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(fieldTypeRegistry.getRequiredTypeConfig(fieldType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/field-types/{type}/editor")
    @Operation(summary = "Get editor component for field type")
    public ResponseEntity<Map<String, String>> getEditorComponent(@PathVariable String type) {
        try {
            com.avionics_systems.test.entity.CustomField.FieldType fieldType =
                    com.avionics_systems.test.entity.CustomField.FieldType.valueOf(type.toUpperCase());
            return ResponseEntity.ok(Map.of("component", fieldTypeRegistry.getEditorComponent(fieldType)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== Field Validation Service Endpoints ====================

    @PostMapping("/fields/validate")
    @Operation(summary = "Validate a field value using advanced validation")
    public ResponseEntity<FieldValidationService.FieldValidationResponse> validateFieldValueAdvanced(
            @RequestParam UUID fieldId,
            @RequestParam(required = false) String value) {
        FieldValidationService.FieldValidationResponse response =
                fieldValidationService.validateField(fieldId, value);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/fields/validate/batch")
    @Operation(summary = "Validate multiple field values in batch")
    public ResponseEntity<FieldValidationService.BatchValidationResponse> validateFieldValuesBatch(
            @RequestBody Map<UUID, String> fieldValues) {
        FieldValidationService.BatchValidationResponse response =
                fieldValidationService.validateBatch(fieldValues);
        return ResponseEntity.ok(response);
    }

    // ==================== Screen Scheme Advanced Endpoints ====================

    @GetMapping("/screen-schemes/{schemeId}/screens")
    @Operation(summary = "Get all screens in a scheme")
    public ResponseEntity<List<ScreenSchemeScreenResponse>> getSchemeScreens(@PathVariable UUID schemeId) {
        List<ScreenSchemeScreenResponse> screens = screenSchemeService.getSchemeScreens(schemeId);
        return ResponseEntity.ok(screens);
    }

    @GetMapping("/screen-schemes/{schemeId}/screens/{screenType}")
    @Operation(summary = "Get screen for specific type in a scheme")
    public ResponseEntity<ScreenSchemeScreenResponse> getScreenForType(
            @PathVariable UUID schemeId,
            @PathVariable String screenType) {
        ScreenSchemeScreenResponse screen = screenSchemeService.getScreenForType(schemeId, screenType);
        return ResponseEntity.ok(screen);
    }

    @GetMapping("/screen-schemes/{schemeId}/validate")
    @Operation(summary = "Validate screen scheme configuration")
    public ResponseEntity<ScreenSchemeValidationResult> validateScheme(@PathVariable UUID schemeId) {
        ScreenSchemeValidationResult result = screenSchemeService.validateSchemeConfiguration(schemeId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/screen-schemes/{sourceId}/clone")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #sourceId)")
    @Operation(summary = "Clone a screen scheme")
    public ResponseEntity<ScreenSchemeResponse> cloneScreenScheme(
            @PathVariable UUID sourceId,
            @RequestParam String newName,
            @RequestParam(required = false) UUID targetProjectId) {
        ScreenSchemeResponse scheme = screenSchemeService.cloneScheme(sourceId, newName, targetProjectId);
        return ResponseEntity.status(HttpStatus.CREATED).body(scheme);
    }

    @PostMapping("/screen-schemes/{sourceId}/clone/bulk")
    @Operation(summary = "Clone multiple screen schemes to a project")
    public ResponseEntity<List<ScreenSchemeResponse>> cloneScreenSchemesBulk(
            @RequestBody List<UUID> sourceIds,
            @RequestParam UUID targetProjectId) {
        List<ScreenSchemeResponse> schemes = screenSchemeService.cloneSchemesBulk(sourceIds, targetProjectId);
        return ResponseEntity.status(HttpStatus.CREATED).body(schemes);
    }

    @PutMapping("/screen-schemes/{schemeId}/screens")
    @PreAuthorize("@projectSecurity.canUpdateTests(authentication, #schemeId)")
    @Operation(summary = "Update scheme screens configuration")
    public ResponseEntity<List<ScreenSchemeScreenResponse>> updateSchemeScreens(
            @PathVariable UUID schemeId,
            @RequestBody List<ScreenSchemeScreenUpdate> updates) {
        List<ScreenSchemeScreen> updated = screenSchemeService.updateSchemeScreens(schemeId, updates);
        return ResponseEntity.ok(updated.stream()
                .map(m -> ScreenSchemeScreenResponse.builder()
                        .id(m.getId())
                        .screenId(m.getScreenId())
                        .screenType(m.getScreenType().name())
                        .build())
                .collect(Collectors.toList()));
    }

    @GetMapping("/screen-schemes/{schemeId}/usage")
    @Operation(summary = "Get screen scheme usage information")
    public ResponseEntity<ScreenSchemeUsageReport> getSchemeUsage(@PathVariable UUID schemeId) {
        ScreenSchemeUsageReport report = screenSchemeService.getSchemeUsage(schemeId);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/screen-schemes/project/{projectId}/create-defaults")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Create default screen schemes for a project")
    public ResponseEntity<List<ScreenSchemeResponse>> createDefaultSchemes(@PathVariable UUID projectId) {
        List<ScreenSchemeResponse> schemes = screenSchemeService.createDefaultSchemesForProject(projectId);
        return ResponseEntity.status(HttpStatus.CREATED).body(schemes);
    }

    // ==================== Screen Advanced Endpoints ====================

    @GetMapping("/screens/search")
    @Operation(summary = "Search screens by name")
    public ResponseEntity<List<ScreenResponse>> searchScreens(@RequestParam String term) {
        List<ScreenResponse> screens = screenService.searchScreens(term);
        return ResponseEntity.ok(screens);
    }

    @GetMapping("/screens/{screenId}/preview")
    @Operation(summary = "Get screen configuration preview")
    public ResponseEntity<ScreenConfigurationPreview> getScreenPreview(@PathVariable UUID screenId) {
        ScreenConfigurationPreview preview = screenService.getScreenPreview(screenId);
        return ResponseEntity.ok(preview);
    }

    @GetMapping("/screens/{screenId}/validate")
    @Operation(summary = "Validate screen configuration")
    public ResponseEntity<ScreenValidationResult> validateScreen(@PathVariable UUID screenId) {
        ScreenValidationResult result = screenService.validateScreen(screenId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/screens/{screenId}/fields/{fieldId}/position")
    @Operation(summary = "Update field position on screen")
    public ResponseEntity<ScreenFieldResponse> updateFieldPosition(
            @PathVariable UUID screenId,
            @PathVariable UUID fieldId,
            @RequestParam Integer position) {
        ScreenFieldResponse field = screenService.updateFieldPosition(screenId, fieldId, position);
        return ResponseEntity.ok(field);
    }

    @PostMapping("/screens/{screenId}/fields/bulk")
    @Operation(summary = "Bulk add fields to screen")
    public ResponseEntity<List<ScreenFieldResponse>> bulkAddFields(
            @PathVariable UUID screenId,
            @RequestBody List<UUID> fieldIds) {
        List<ScreenFieldResponse> fields = screenService.bulkAddFields(screenId, fieldIds);
        return ResponseEntity.ok(fields);
    }

    @DeleteMapping("/screens/{screenId}/fields/bulk")
    @Operation(summary = "Bulk remove fields from screen")
    public ResponseEntity<Map<String, Integer>> bulkRemoveFields(
            @PathVariable UUID screenId,
            @RequestBody List<UUID> fieldIds) {
        int removedCount = screenService.bulkRemoveFields(screenId, fieldIds);
        return ResponseEntity.ok(Map.of("removedCount", removedCount));
    }

    // ==================== Custom Field Advanced Endpoints ====================

    @GetMapping("/custom-fields/search")
    @Operation(summary = "Search custom fields by name")
    public ResponseEntity<List<CustomFieldResponse>> searchCustomFields(@RequestParam String term) {
        List<CustomFieldResponse> fields = customFieldService.searchFields(term);
        return ResponseEntity.ok(fields);
    }

    @GetMapping("/custom-fields/type/{fieldType}")
    @Operation(summary = "List custom fields by type")
    public ResponseEntity<List<CustomFieldResponse>> listFieldsByType(@PathVariable String fieldType) {
        List<CustomFieldResponse> fields = customFieldService.listFieldsByType(fieldType);
        return ResponseEntity.ok(fields);
    }

    @GetMapping("/custom-fields/key/{fieldKey}")
    @Operation(summary = "Get custom field by key")
    public ResponseEntity<CustomFieldResponse> getFieldByKey(@PathVariable String fieldKey) {
        CustomFieldResponse field = customFieldService.getFieldByKey(fieldKey);
        return ResponseEntity.ok(field);
    }

    @GetMapping("/custom-fields/{fieldId}/options")
    @Operation(summary = "Get custom field options")
    public ResponseEntity<CustomFieldOptionsResponse> getFieldOptions(@PathVariable UUID fieldId) {
        CustomFieldOptionsResponse options = customFieldService.getFieldOptions(fieldId);
        return ResponseEntity.ok(options);
    }

    @PutMapping("/custom-fields/{fieldId}/options")
    @Operation(summary = "Update custom field options")
    public ResponseEntity<CustomFieldResponse> updateFieldOptions(
            @PathVariable UUID fieldId,
            @RequestBody List<CustomFieldOptionUpdate> options) {
        CustomFieldResponse field = customFieldService.updateFieldOptions(fieldId, options);
        return ResponseEntity.ok(field);
    }

    @PostMapping("/custom-fields/{fieldId}/options")
    @Operation(summary = "Add option to custom field")
    public ResponseEntity<CustomFieldResponse> addFieldOption(
            @PathVariable UUID fieldId,
            @RequestBody CustomFieldOptionUpdate option) {
        CustomFieldResponse field = customFieldService.addFieldOption(fieldId, option);
        return ResponseEntity.ok(field);
    }

    @DeleteMapping("/custom-fields/{fieldId}/options/{optionValue}")
    @Operation(summary = "Remove option from custom field")
    public ResponseEntity<CustomFieldResponse> removeFieldOption(
            @PathVariable UUID fieldId,
            @PathVariable String optionValue) {
        CustomFieldResponse field = customFieldService.removeFieldOption(fieldId, optionValue);
        return ResponseEntity.ok(field);
    }

    @GetMapping("/custom-fields/{fieldId}/validate-config")
    @Operation(summary = "Validate custom field configuration")
    public ResponseEntity<CustomFieldValidationReport> validateFieldConfiguration(@PathVariable UUID fieldId) {
        CustomFieldValidationReport report = customFieldService.validateFieldConfiguration(fieldId);
        return ResponseEntity.ok(report);
    }

    @PostMapping("/custom-fields/clone/bulk")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #targetProjectId)")
    @Operation(summary = "Clone custom fields to a project")
    public ResponseEntity<List<CustomFieldResponse>> cloneFieldsToProject(
            @RequestBody List<UUID> fieldIds,
            @RequestParam UUID targetProjectId) {
        List<CustomFieldResponse> fields = customFieldService.cloneFieldsToProject(fieldIds, targetProjectId);
        return ResponseEntity.status(HttpStatus.CREATED).body(fields);
    }
}