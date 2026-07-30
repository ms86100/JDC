package com.avionics_systems.search.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.avionics_systems.search.dto.ReindexStatusResponse;
import com.avionics_systems.search.service.ReindexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Bulk reindex API used by migration-service post-import (paths match SearchServiceClient).
 */
@RestController
@RequestMapping({"/api/search", "/search"})
@RequiredArgsConstructor
@Slf4j
public class ReindexController {

    private final ReindexService reindexService;

    @PostMapping("/reindex")
    public ResponseEntity<ReindexStatusResponse> reindex(@RequestBody(required = false) JsonNode body) {
        log.info("POST reindex entityType payload={}", body);
        return ResponseEntity.ok(reindexService.reindexEntityType(body));
    }

    @PostMapping("/reindex-all")
    public ResponseEntity<ReindexStatusResponse> reindexAll() {
        log.info("POST reindex-all");
        return ResponseEntity.ok(reindexService.reindexAll());
    }

    @GetMapping("/reindex/status/{entityType}")
    public ResponseEntity<ReindexStatusResponse> reindexStatus(@PathVariable String entityType) {
        return ResponseEntity.ok(ReindexStatusResponse.builder()
                .entityType(entityType.toUpperCase())
                .status("COMPLETED")
                .success(true)
                .progressPercentage(100.0)
                .build());
    }

    @GetMapping("/status")
    public ResponseEntity<ReindexStatusResponse> indexStatus() {
        return ResponseEntity.ok(ReindexStatusResponse.builder()
                .entityType("ALL")
                .status("READY")
                .success(true)
                .build());
    }
}
