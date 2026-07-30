package com.avionics_systems.search.controller;

import com.avionics_systems.search.dto.JQLSearchRequest;
import com.avionics_systems.search.dto.JQLSearchResponse;
import com.avionics_systems.search.entity.JQLQuery;
import com.avionics_systems.search.service.JQLParser;
import com.avionics_systems.search.service.JQLSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * JQL Search Controller - REST endpoints for JQL search
 * Matches Legacy DC REST API: /rest/api/2/search
 */
@RestController
@RequestMapping("/api/jql")
@RequiredArgsConstructor
@Slf4j
public class JQLController {

    private final JQLSearchService jqlSearchService;
    private final JQLParser jqlParser;

    /**
     * Execute JQL search
     * Matches: POST /rest/api/2/search
     */
    @PostMapping("/search")
    public ResponseEntity<JQLSearchResponse> search(@RequestBody JQLSearchRequest request) {
        log.info("JQL Search request: {}", request.getJql());
        JQLSearchResponse response = jqlSearchService.search(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Parse and validate JQL query
     * GET /api/jql/parse?jql=...
     */
    @GetMapping("/parse")
    public ResponseEntity<JQLQuery> parseQuery(@RequestParam String jql) {
        log.info("Parsing JQL: {}", jql);
        JQLQuery query = jqlSearchService.parseQuery(jql);
        return ResponseEntity.ok(query);
    }

    /**
     * Validate JQL query
     * GET /api/jql/validate?jql=...
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateQuery(@RequestParam String jql) {
        log.info("Validating JQL: {}", jql);
        JQLQuery query = jqlSearchService.parseQuery(jql);
        List<String> errors = jqlSearchService.validateQuery(query);

        boolean valid = errors.isEmpty();
        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "errors", errors,
                "query", query
        ));
    }

    /**
     * Get field autocomplete suggestions
     * GET /api/jql/fields/suggest?fieldName=...
     */
    @GetMapping("/fields/suggest")
    public ResponseEntity<List<String>> getFieldSuggestions(@RequestParam String fieldName) {
        List<String> suggestions = jqlSearchService.getFieldSuggestions(fieldName);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Get operator suggestions for a field
     * GET /api/jql/operators/suggest?fieldName=...
     */
    @GetMapping("/operators/suggest")
    public ResponseEntity<List<String>> getOperatorSuggestions(@RequestParam String fieldName) {
        List<String> suggestions = jqlSearchService.getOperatorSuggestions(fieldName);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Get value suggestions for a field
     * GET /api/jql/values/suggest?fieldName=...&prefix=...
     */
    @GetMapping("/values/suggest")
    public ResponseEntity<List<String>> getValueSuggestions(
            @RequestParam String fieldName,
            @RequestParam(required = false, defaultValue = "") String prefix) {
        List<String> suggestions = jqlSearchService.getValueSuggestions(fieldName, prefix);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Get available JQL fields
     * GET /api/jql/fields
     */
    @GetMapping("/fields")
    public ResponseEntity<List<Map<String, String>>> getAvailableFields() {
        List<Map<String, String>> fields = List.of(
                Map.of("id", "issuekey", "name", "Issue", "type", "text"),
                Map.of("id", "summary", "name", "Summary", "type", "text"),
                Map.of("id", "description", "name", "Description", "type", "text"),
                Map.of("id", "project", "name", "Project", "type", "project"),
                Map.of("id", "status", "name", "Status", "type", "status"),
                Map.of("id", "issuetype", "name", "Issue Type", "type", "issuetype"),
                Map.of("id", "priority", "name", "Priority", "type", "priority"),
                Map.of("id", "assignee", "name", "Assignee", "type", "user"),
                Map.of("id", "reporter", "name", "Reporter", "type", "user"),
                Map.of("id", "creator", "name", "Creator", "type", "user"),
                Map.of("id", "created", "name", "Created", "type", "datetime"),
                Map.of("id", "updated", "name", "Updated", "type", "datetime"),
                Map.of("id", "duedate", "name", "Due Date", "type", "date"),
                Map.of("id", "resolved", "name", "Resolved", "type", "datetime"),
                Map.of("id", "resolution", "name", "Resolution", "type", "resolution"),
                Map.of("id", "labels", "name", "Labels", "type", "labels"),
                Map.of("id", "component", "name", "Component", "type", "component"),
                Map.of("id", "fixversion", "name", "Fix Version", "type", "version"),
                Map.of("id", "affectsversion", "name", "Affects Version", "type", "version"),
                Map.of("id", "sprint", "name", "Sprint", "type", "sprint"),
                Map.of("id", "epic", "name", "Epic", "type", "epic"),
                Map.of("id", "parent", "name", "Parent", "type", "issue"),
                Map.of("id", "votes", "name", "Votes", "type", "number"),
                Map.of("id", "watchers", "name", "Watchers", "type", "number"),
                Map.of("id", "timeestimate", "name", "Time Estimate", "type", "duration"),
                Map.of("id", "timespent", "name", "Time Spent", "type", "duration"),
                Map.of("id", "originalestimate", "name", "Original Estimate", "type", "duration"),
                Map.of("id", "remainingestimate", "name", "Remaining Estimate", "type", "duration"),
                Map.of("id", "workratio", "name", "Work Ratio", "type", "number"),
                Map.of("id", "security", "name", "Security Level", "type", "security"),
                Map.of("id", "linkedissue", "name", "Linked Issue", "type", "issue")
        );
        return ResponseEntity.ok(fields);
    }
}