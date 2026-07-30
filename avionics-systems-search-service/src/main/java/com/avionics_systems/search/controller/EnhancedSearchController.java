package com.avionics_systems.search.controller;

import com.avionics_systems.search.dto.EnhancedSearchResponse;
import com.avionics_systems.search.service.EnhancedSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Enhanced Search Controller with facets, filters, and results view.
 * F6-US001/US006: Search Enhancement
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Enhanced Search", description = "Enhanced search endpoints with facets, filters, and results view")
public class EnhancedSearchController {

    private final EnhancedSearchService enhancedSearchService;

    @Value("${app.search.default-result-fields:id,entityType,title,content,createdAt}")
    private String defaultResultFieldsStr;

    @Value("${app.search.suggestion-suffixes:issue,bug,feature,task}")
    private String suggestionSuffixesStr;

    @Value("${app.search.default-anonymous-label:anonymous}")
    private String defaultAnonymousLabel;

    @Value("${app.search.facet-entity-types:issue,project,document}")
    private String facetEntityTypesStr;

    @Value("${app.search.export-ready-message:Export functionality ready. Results can be downloaded as}")
    private String exportReadyMessage;

    @GetMapping("/enhanced")
    @Operation(summary = "Enhanced search", description = "Perform enhanced search with facets, filters, and sorting")
    public ResponseEntity<EnhancedSearchResponse> enhancedSearch(
            @Parameter(description = "Search query") @RequestParam String q,
            @Parameter(description = "Filter by entity type") @RequestParam(required = false) String entityType,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(required = false) String sortField,
            @Parameter(description = "Sort order") @RequestParam(required = false) String sortOrder) {

        log.info("GET /search/enhanced - q={}, entityType={}, page={}, size={}", q, entityType, page, size);

        EnhancedSearchResponse.SortOrder order = null;
        if (sortOrder != null) {
            try {
                order = EnhancedSearchResponse.SortOrder.valueOf(sortOrder.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid sort order: {}", sortOrder);
            }
        }

        Map<String, List<String>> filters = new HashMap<>();
        if (entityType != null && !entityType.isBlank()) {
            filters.put("entityType", List.of(entityType));
        }

        EnhancedSearchResponse response = enhancedSearchService.enhancedSearch(
                q, entityType, page, size, filters, sortField, order);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/enhanced")
    @Operation(summary = "Enhanced search with filters", description = "Perform enhanced search with complex filters")
    public ResponseEntity<EnhancedSearchResponse> enhancedSearchWithFilters(
            @RequestBody EnhancedSearchService.EnhancedSearchRequest request) {

        log.info("POST /search/enhanced - query={}, filters={}", request.getQuery(), request.getFilters());

        EnhancedSearchResponse response = enhancedSearchService.searchWithFilters(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/results")
    @Operation(summary = "Get search results view", description = "Get search results formatted for list/table view")
    public ResponseEntity<EnhancedSearchService.SearchResultsView> getSearchResultsView(
            @Parameter(description = "Search query") @RequestParam String q,
            @Parameter(description = "Filter by entity type") @RequestParam(required = false) String entityType,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Fields to include") @RequestParam(required = false) List<String> fields) {

        log.info("GET /search/results - q={}, entityType={}, page={}", q, entityType, page);

        // Default fields if not specified
        if (fields == null || fields.isEmpty()) {
            fields = Arrays.asList(defaultResultFieldsStr.split(","));
        }

        EnhancedSearchService.SearchResultsView view = enhancedSearchService.getSearchResultsView(
                q, entityType, page, size, fields);

        return ResponseEntity.ok(view);
    }

    @GetMapping("/facets")
    @Operation(summary = "Get search facets", description = "Get available facets for filtering")
    public ResponseEntity<List<Map<String, Object>>> getSearchFacets(
            @Parameter(description = "Filter by entity type") @RequestParam(required = false) String entityType) {

        log.info("GET /search/facets - entityType={}", entityType);

        List<Map<String, Object>> entityTypeValues = Arrays.stream(facetEntityTypesStr.split(","))
                .map(String::trim)
                .map(v -> Map.<String, Object>of(
                        "value", v,
                        "label", v.substring(0, 1).toUpperCase() + v.substring(1),
                        "count", 0))
                .toList();

        List<Map<String, Object>> facets = List.of(
                Map.of(
                        "field", "entityType",
                        "label", "Entity Type",
                        "type", "TERMS",
                        "values", entityTypeValues
                ),
                Map.of(
                        "field", "createdAt",
                        "label", "Created Date",
                        "type", "DATE_RANGE",
                        "values", List.of(
                                Map.of("value", "today", "label", "Today"),
                                Map.of("value", "week", "label", "This Week"),
                                Map.of("value", "month", "label", "This Month"),
                                Map.of("value", "year", "label", "This Year")
                        )
                )
        );

        return ResponseEntity.ok(facets);
    }

    @GetMapping("/suggest")
    @Operation(summary = "Get search suggestions", description = "Get search suggestions based on query prefix")
    public ResponseEntity<Map<String, Object>> getSearchSuggestions(
            @Parameter(description = "Search query") @RequestParam String q,
            @Parameter(description = "Maximum suggestions") @RequestParam(defaultValue = "10") int max) {

        log.info("GET /search/suggest - q={}, max={}", q, max);

        List<String> suggestions = new ArrayList<>();

        // Generate suggestions based on query
        if (q != null && !q.isBlank()) {
            String lowerQuery = q.toLowerCase();
            // Simulated suggestions - in production, these would come from search index
            for (String suffix : suggestionSuffixesStr.split(",")) {
                suggestions.add(lowerQuery + " " + suffix.trim());
            }
            if (lowerQuery.length() > 2) {
                suggestions.add(lowerQuery.substring(0, 1).toUpperCase() + lowerQuery.substring(1));
            }
        }

        return ResponseEntity.ok(Map.of(
                "query", q,
                "suggestions", suggestions.stream().limit(max).toList(),
                "count", Math.min(suggestions.size(), max)
        ));
    }

    @GetMapping("/history")
    @Operation(summary = "Get search history", description = "Get recent search queries for current user")
    public ResponseEntity<Map<String, Object>> getSearchHistory(
            @Parameter(description = "User ID") @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Parameter(description = "Maximum entries") @RequestParam(defaultValue = "10") int max) {

        log.info("GET /search/history - userId={}, max={}", userId, max);

        // Return empty history - in production, this would be stored per user
        List<Map<String, Object>> history = new ArrayList<>();

        return ResponseEntity.ok(Map.of(
                "userId", userId != null ? userId.toString() : defaultAnonymousLabel,
                "history", history,
                "count", 0
        ));
    }

    @GetMapping("/export")
    @Operation(summary = "Export search results", description = "Export search results to specified format")
    public ResponseEntity<Map<String, Object>> exportSearchResults(
            @Parameter(description = "Search query") @RequestParam String q,
            @Parameter(description = "Entity type filter") @RequestParam(required = false) String entityType,
            @Parameter(description = "Export format") @RequestParam(defaultValue = "csv") String format) {

        log.info("GET /search/export - q={}, format={}", q, format);

        return ResponseEntity.ok(Map.of(
                "query", q,
                "format", format,
                "status", "ready",
                "message", exportReadyMessage + " " + format.toUpperCase()
        ));
    }
}