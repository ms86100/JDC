package com.avionics_systems.test.controller;

import com.avionics_systems.test.dto.CreateHlvvoRequest;
import com.avionics_systems.test.dto.HlvvoResponse;
import com.avionics_systems.test.dto.VvoResponse;
import com.avionics_systems.test.service.VvoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hlvvo")
@RequiredArgsConstructor
@Tag(name = "HLVVO Management", description = "High Level V&V Objectives")
public class HlvvoController {

    private final VvoService vvoService;

    @PostMapping
    @Operation(summary = "Create HLVVO")
    public ResponseEntity<HlvvoResponse> createHlvvo(@Valid @RequestBody CreateHlvvoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vvoService.createHlvvo(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get HLVVO by ID")
    public ResponseEntity<HlvvoResponse> getHlvvo(@PathVariable UUID id) {
        return ResponseEntity.ok(vvoService.getHlvvo(id));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "List HLVVOs by project")
    public ResponseEntity<List<HlvvoResponse>> getHlvvosByProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(vvoService.getHlvvosByProject(projectId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update HLVVO")
    public ResponseEntity<HlvvoResponse> updateHlvvo(
            @PathVariable UUID id,
            @Valid @RequestBody CreateHlvvoRequest request) {
        return ResponseEntity.ok(vvoService.updateHlvvo(id, request));
    }

    @GetMapping("/{id}/child-vvos")
    @Operation(summary = "Get child VVOs for HLVVO")
    public ResponseEntity<List<VvoResponse>> getChildVvos(@PathVariable UUID id) {
        return ResponseEntity.ok(vvoService.getVvosByHlvvo(id));
    }
}
