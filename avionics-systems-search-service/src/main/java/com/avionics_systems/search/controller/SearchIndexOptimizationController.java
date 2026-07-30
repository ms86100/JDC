package com.avionics_systems.search.controller;

import com.avionics_systems.search.service.SearchIndexOptimizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for index optimization operations.
 * Provides endpoints for index maintenance and performance monitoring.
 */
@RestController
@RequestMapping("/api/search/index")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Search Index Optimization", description = "Index maintenance and optimization endpoints")
public class SearchIndexOptimizationController {

    private final SearchIndexOptimizationService indexOptimizationService;

    @PostMapping("/rebuild")
    @Operation(summary = "Rebuild search index", description = "Rebuild all search vectors in the index")
    public ResponseEntity<Map<String, Object>> rebuildIndex() {
        log.info("Index rebuild requested");
        return ResponseEntity.ok(indexOptimizationService.rebuildSearchIndex());
    }

    @PostMapping("/optimize")
    @Operation(summary = "Optimize index", description = "Optimize the search index by updating statistics")
    public ResponseEntity<Map<String, Object>> optimizeIndex() {
        log.info("Index optimization requested");
        return ResponseEntity.ok(indexOptimizationService.optimizeIndex());
    }

    @PostMapping("/vacuum")
    @Operation(summary = "Vacuum index", description = "Run VACUUM to reclaim space")
    public ResponseEntity<Map<String, Object>> vacuumIndex() {
        log.info("Index vacuum requested");
        return ResponseEntity.ok(indexOptimizationService.vacuumIndex());
    }

    @GetMapping("/stats")
    @Operation(summary = "Get index stats", description = "Get current index statistics and health")
    public ResponseEntity<Map<String, Object>> getIndexStats() {
        return ResponseEntity.ok(indexOptimizationService.getIndexStats());
    }

    @GetMapping("/analyze-slow-queries")
    @Operation(summary = "Analyze slow queries", description = "Analyze and report on slow search queries")
    public ResponseEntity<Map<String, Object>> analyzeSlowQueries(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(indexOptimizationService.analyzeSlowQueries(limit));
    }
}