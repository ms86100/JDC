package com.jira.search.controller;

import com.jira.search.dto.IndexRequest;
import com.jira.search.dto.IndexResponse;
import com.jira.search.dto.SearchResponse;
import com.jira.search.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/index")
    public ResponseEntity<IndexResponse> indexEntity(@Valid @RequestBody IndexRequest request) {
        log.info("POST /search/index - Indexing entity: {} {}", request.getEntityType(), request.getEntityId());
        IndexResponse response = searchService.indexEntity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/index/{entityType}/{entityId}")
    public ResponseEntity<Void> removeFromIndex(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        log.info("DELETE /search/index/{}/{} - Removing from index", entityType, entityId);
        searchService.removeFromIndex(entityType, entityId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam String q,
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /search - Searching for: {} (entityType: {})", q, entityType);
        SearchResponse response = searchService.search(q, entityType, page, size);
        return ResponseEntity.ok(response);
    }
}