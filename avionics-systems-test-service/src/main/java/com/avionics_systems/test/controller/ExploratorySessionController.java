package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.ExploratorySessionRequest;
import com.avionics_systems.test.dto.ExploratorySessionResponse;
import com.avionics_systems.test.service.ExploratorySessionService;
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

@RestController
@RequestMapping("/api/exploratory-sessions")
@RequiredArgsConstructor
@Tag(name = "Exploratory Sessions", description = "APIs for managing exploratory testing sessions")
public class ExploratorySessionController {

    private final ExploratorySessionService exploratorySessionService;

    @PostMapping
    @PreAuthorize("@projectSecurity.canCreateTests(authentication, #request.projectId)")
    @Operation(summary = "Create a new exploratory session")
    public ResponseEntity<ExploratorySessionResponse> createSession(@Valid @RequestBody ExploratorySessionRequest request) {
        ExploratorySessionResponse session = exploratorySessionService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an exploratory session by ID")
    public ResponseEntity<ExploratorySessionResponse> getSession(@PathVariable UUID id, @RequestParam UUID projectId) {
        ExploratorySessionResponse session = exploratorySessionService.getSession(id);
        return ResponseEntity.ok(session);
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("@projectSecurity.hasProjectAccess(authentication, #projectId)")
    @Operation(summary = "Get all exploratory sessions for a project")
    public ResponseEntity<List<ExploratorySessionResponse>> getSessionsByProject(@PathVariable UUID projectId) {
        List<ExploratorySessionResponse> sessions = exploratorySessionService.getSessionsByProject(projectId);
        return ResponseEntity.ok(sessions);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an exploratory session")
    public ResponseEntity<ExploratorySessionResponse> updateSession(
            @PathVariable UUID id,
            @RequestParam UUID projectId,
            @Valid @RequestBody ExploratorySessionRequest request) {
        ExploratorySessionResponse session = exploratorySessionService.updateSession(id, request);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start an exploratory session")
    public ResponseEntity<ExploratorySessionResponse> startSession(@PathVariable UUID id, @RequestParam UUID projectId) {
        ExploratorySessionResponse session = exploratorySessionService.startSession(id);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete an exploratory session")
    public ResponseEntity<ExploratorySessionResponse> completeSession(@PathVariable UUID id, @RequestParam UUID projectId) {
        ExploratorySessionResponse session = exploratorySessionService.completeSession(id);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{id}/abandon")
    @Operation(summary = "Abandon an exploratory session")
    public ResponseEntity<ExploratorySessionResponse> abandonSession(@PathVariable UUID id, @RequestParam UUID projectId) {
        ExploratorySessionResponse session = exploratorySessionService.abandonSession(id);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{id}/notes")
    @Operation(summary = "Add notes to an exploratory session")
    public ResponseEntity<ExploratorySessionResponse> addNotes(
            @PathVariable UUID id,
            @RequestParam UUID projectId,
            @RequestBody Map<String, String> body) {
        ExploratorySessionResponse session = exploratorySessionService.addNotes(id, body.get("content"));
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{id}/bugs")
    @Operation(summary = "Add a bug to an exploratory session")
    public ResponseEntity<ExploratorySessionResponse> addBug(
            @PathVariable UUID id,
            @RequestParam UUID projectId,
            @RequestBody Map<String, String> body) {
        ExploratorySessionResponse session = exploratorySessionService.addBug(id, body.get("content"));
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{id}/ideas")
    @Operation(summary = "Add an idea to an exploratory session")
    public ResponseEntity<ExploratorySessionResponse> addIdea(
            @PathVariable UUID id,
            @RequestParam UUID projectId,
            @RequestBody Map<String, String> body) {
        ExploratorySessionResponse session = exploratorySessionService.addIdea(id, body.get("content"));
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{id}/questions")
    @Operation(summary = "Add a question to an exploratory session")
    public ResponseEntity<ExploratorySessionResponse> addQuestion(
            @PathVariable UUID id,
            @RequestParam UUID projectId,
            @RequestBody Map<String, String> body) {
        ExploratorySessionResponse session = exploratorySessionService.addQuestion(id, body.get("content"));
        return ResponseEntity.ok(session);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@projectSecurity.canDeleteTests(authentication, #projectId)")
    @Operation(summary = "Delete an exploratory session")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID id, @RequestParam UUID projectId) {
        exploratorySessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
