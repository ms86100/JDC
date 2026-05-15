package com.jira.plan.controller;

import com.jira.plan.dto.request.CreateProgramRequest;
import com.jira.plan.dto.request.UpdateProgramRequest;
import com.jira.plan.dto.response.ProgramResponse;
import com.jira.plan.service.ProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/plans/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;

    @GetMapping
    public ResponseEntity<List<ProgramResponse>> getAllPrograms() {
        return ResponseEntity.ok(programService.getAllPrograms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProgramResponse> getProgramById(@PathVariable UUID id) {
        return ResponseEntity.ok(programService.getProgramById(id));
    }

    @PostMapping
    public ResponseEntity<ProgramResponse> createProgram(@Valid @RequestBody CreateProgramRequest request) {
        ProgramResponse response = programService.createProgram(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProgramResponse> updateProgram(
            @PathVariable UUID id,
            @RequestBody UpdateProgramRequest request) {
        return ResponseEntity.ok(programService.updateProgram(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProgram(@PathVariable UUID id) {
        programService.deleteProgram(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/plans/{planId}")
    public ResponseEntity<Void> linkPlanToProgram(
            @PathVariable UUID id,
            @PathVariable UUID planId) {
        programService.linkPlanToProgram(id, planId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/plans/{planId}")
    public ResponseEntity<Void> unlinkPlanFromProgram(
            @PathVariable UUID id,
            @PathVariable UUID planId) {
        programService.unlinkPlanFromProgram(id, planId);
        return ResponseEntity.noContent().build();
    }
}
