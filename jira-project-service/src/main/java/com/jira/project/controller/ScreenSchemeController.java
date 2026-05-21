package com.jira.project.controller;

import com.jira.project.dto.UpsertScreenIssueTypeOverrideRequest;
import com.jira.project.entity.ScreenSchemeIssueTypeScreen;
import com.jira.project.repository.ScreenSchemeIssueTypeScreenRepository;
import com.jira.project.repository.ScreenSchemeRepository;
import com.jira.project.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/screen-schemes")
@RequiredArgsConstructor
@Tag(name = "Screen schemes", description = "Screen scheme configuration including issue-type overrides")
public class ScreenSchemeController {

    private final ScreenSchemeRepository screenSchemeRepository;
    private final ScreenSchemeIssueTypeScreenRepository issueTypeScreenRepository;

    @GetMapping("/{schemeId}/issue-type-screens")
    @Operation(summary = "List issue-type screen overrides")
    public ResponseEntity<List<ScreenSchemeIssueTypeScreen>> listIssueTypeOverrides(@PathVariable UUID schemeId) {
        ensureSchemeExists(schemeId);
        return ResponseEntity.ok(issueTypeScreenRepository.findBySchemeId(schemeId));
    }

    @PutMapping("/{schemeId}/issue-type-screens")
    @Transactional
    @Operation(summary = "Upsert issue-type screen override", description = "Maps CREATE/EDIT/VIEW screen for a specific issue type")
    public ResponseEntity<ScreenSchemeIssueTypeScreen> upsertIssueTypeOverride(
            @PathVariable UUID schemeId,
            @Valid @RequestBody UpsertScreenIssueTypeOverrideRequest request) {
        ensureSchemeExists(schemeId);
        String screenType = request.getScreenType().trim().toUpperCase(Locale.ROOT);

        ScreenSchemeIssueTypeScreen row = ScreenSchemeIssueTypeScreen.builder()
                .schemeId(schemeId)
                .issueTypeId(request.getIssueTypeId())
                .screenType(screenType)
                .screenId(request.getScreenId())
                .build();
        return ResponseEntity.ok(issueTypeScreenRepository.save(row));
    }

    @DeleteMapping("/{schemeId}/issue-type-screens")
    @Transactional
    @Operation(summary = "Remove issue-type screen override")
    public ResponseEntity<Void> deleteIssueTypeOverride(
            @PathVariable UUID schemeId,
            @RequestParam UUID issueTypeId,
            @RequestParam String screenType) {
        ensureSchemeExists(schemeId);
        String type = screenType.trim().toUpperCase(Locale.ROOT);
        issueTypeScreenRepository.deleteById(new ScreenSchemeIssueTypeScreen.IdClass(schemeId, issueTypeId, type));
        return ResponseEntity.noContent().build();
    }

    private void ensureSchemeExists(UUID schemeId) {
        if (!screenSchemeRepository.existsById(schemeId)) {
            throw new ResourceNotFoundException("ScreenScheme", "id", schemeId);
        }
    }
}
