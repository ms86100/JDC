package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.TestProjectSettingsRequest;
import com.avionics_systems.test.dto.TestProjectSettingsResponse;
import com.avionics_systems.test.service.TestSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/test-settings")
@RequiredArgsConstructor
@Tag(name = "Test Project Settings", description = "APIs for managing per-project test configuration settings")
public class TestSettingsController {

    private final TestSettingsService testSettingsService;

    @GetMapping
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get test project settings")
    public ResponseEntity<TestProjectSettingsResponse> getSettings(@RequestParam UUID projectId) {
        TestProjectSettingsResponse settings = testSettingsService.getSettingsByProject(projectId);
        return ResponseEntity.ok(settings);
    }

    @PutMapping
    @PreAuthorize("@projectSecurity.isProjectAdmin(authentication, #request.projectId)")
    @Operation(summary = "Save test project settings")
    public ResponseEntity<TestProjectSettingsResponse> saveSettings(@Valid @RequestBody TestProjectSettingsRequest request) {
        TestProjectSettingsResponse settings = testSettingsService.saveSettings(request);
        return ResponseEntity.ok(settings);
    }
}
