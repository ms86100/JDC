package com.jira.migration.controller;

import com.jira.migration.dto.IssueVisibleFieldsResponse;
import com.jira.migration.entity.field.CustomFieldContext;
import com.jira.migration.entity.field.FieldScreenMapping.FieldScreenType;
import com.jira.migration.repository.field.CustomFieldContextRepository;
import com.jira.migration.service.field.FieldIssueContextResolver;
import com.jira.migration.service.field.FieldSearchService;
import com.jira.migration.service.field.FieldVisibilityEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/fields")
@RequiredArgsConstructor
@Tag(name = "Field Visibility", description = "Jira DC-style custom field visibility resolution")
public class FieldVisibilityController {

    private final FieldVisibilityEngine fieldVisibilityEngine;
    private final FieldIssueContextResolver fieldIssueContextResolver;
    private final FieldSearchService fieldSearchService;
    private final CustomFieldContextRepository customFieldContextRepository;

    @GetMapping("/issues/{issueIdOrKey}/visible")
    @Operation(summary = "Visible custom fields for an issue (supports UUID or issue key e.g. PXX-6)")
    public ResponseEntity<IssueVisibleFieldsResponse> getVisibleFields(
            @PathVariable String issueIdOrKey,
            @RequestParam(defaultValue = "VIEW") String screen,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) UUID issueTypeId) {

        return fieldIssueContextResolver.resolve(issueIdOrKey)
                .map(ctx -> {
                    UUID pid = projectId != null ? projectId : ctx.projectId();
                    UUID tid = issueTypeId != null ? issueTypeId : ctx.issueTypeId();
                    FieldScreenType screenType = parseScreen(screen);
                    IssueVisibleFieldsResponse body = fieldVisibilityEngine.resolveVisibleFieldsForIssue(
                            ctx.issueId(), ctx.issueKey(), pid, tid, screenType);
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search/custom")
    @Operation(summary = "Search issues by custom field value")
    public ResponseEntity<Map<String, Object>> searchByCustomField(
            @RequestParam UUID projectId,
            @RequestParam String fieldKey,
            @RequestParam String q) {
        List<UUID> issueIds = fieldSearchService.searchIssuesByCustomField(projectId, fieldKey, q);
        return ResponseEntity.ok(Map.of(
                "projectId", projectId,
                "fieldKey", fieldKey,
                "query", q,
                "issueIds", issueIds,
                "total", issueIds.size()));
    }

    @GetMapping("/search/autocomplete")
    @Operation(summary = "Autocomplete searchable custom field keys")
    public ResponseEntity<List<Map<String, Object>>> autocomplete(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) String prefix) {
        return ResponseEntity.ok(fieldSearchService.autocompleteFieldKeys(projectId, prefix));
    }

    @GetMapping("/contexts")
    @Operation(summary = "List custom field contexts")
    public ResponseEntity<List<CustomFieldContext>> listContexts(
            @RequestParam(required = false) UUID customFieldId) {
        if (customFieldId != null) {
            return ResponseEntity.ok(customFieldContextRepository.findByCustomFieldId(customFieldId));
        }
        return ResponseEntity.ok(customFieldContextRepository.findAllEnabled());
    }

    private FieldScreenType parseScreen(String screen) {
        try {
            return FieldScreenType.valueOf(screen.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return FieldScreenType.VIEW;
        }
    }
}
