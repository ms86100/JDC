package com.jira.search.controller;

import com.jira.search.service.IndexOptimizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Index Optimization Controller
 * Phase 7 - Polish & Performance
 * Provides endpoints for managing search indexes
 */
@RestController
@RequestMapping("/api/search/admin")
@Tag(name = "Search Admin", description = "Search index optimization and management")
public class IndexOptimizationController {

    private final IndexOptimizationService indexOptimizationService;

    public IndexOptimizationController(IndexOptimizationService indexOptimizationService) {
        this.indexOptimizationService = indexOptimizationService;
    }

    @PostMapping("/optimize")
    @Operation(summary = "Optimize all search indexes",
               description = "Creates/rebuilds indexes, triggers, and runs VACUUM ANALYZE")
    public Map<String, Object> optimizeIndexes() {
        String result = indexOptimizationService.optimizeAll();
        IndexOptimizationService.IndexStats stats = indexOptimizationService.getIndexStats();

        return Map.of(
                "status", "success",
                "message", result,
                "statistics", stats
        );
    }

    @PostMapping("/reindex")
    @Operation(summary = "Rebuild all search indexes",
               description = "Forces a complete rebuild of all search indexes")
    public Map<String, String> reindex() {
        indexOptimizationService.reindexAll();
        return Map.of("status", "success", "message", "Reindex completed");
    }

    @PostMapping("/analyze")
    @Operation(summary = "Analyze search table",
               description = "Updates query planner statistics for better execution plans")
    public Map<String, String> analyze() {
        indexOptimizationService.analyzeTable();
        return Map.of("status", "success", "message", "Analysis completed");
    }

    @PostMapping("/create-trigger")
    @Operation(summary = "Create search vector trigger",
               description = "Creates trigger for automatic search_vector updates")
    public Map<String, String> createTrigger() {
        indexOptimizationService.createSearchVectorTrigger();
        return Map.of("status", "success", "message", "Trigger created");
    }
}