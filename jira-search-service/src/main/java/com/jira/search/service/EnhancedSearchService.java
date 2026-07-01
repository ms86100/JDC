package com.jira.search.service;

import com.jira.search.dto.EnhancedSearchResponse;
import com.jira.search.dto.SearchResponse;
import com.jira.search.entity.SearchIndex;
import com.jira.search.repository.SearchIndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced Search Service with facets, filters, and suggestions.
 * F6-US001/US006: Search Enhancement
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedSearchService {

    private final SearchIndexRepository searchIndexRepository;

    @Transactional(readOnly = true)
    public EnhancedSearchResponse enhancedSearch(
            String query,
            String entityType,
            int page,
            int size,
            Map<String, List<String>> filters,
            String sortField,
            EnhancedSearchResponse.SortOrder sortOrder) {

        long startTime = System.currentTimeMillis();
        log.info("Enhanced search: query={}, entityType={}, page={}, size={}", query, entityType, page, size);

        Pageable pageable = PageRequest.of(page, size);
        String processedQuery = processQueryForTsQuery(query);

        Page<SearchIndex> results = searchIndexRepository.fullTextSearch(processedQuery, entityType, pageable);

        // Build enhanced results with highlights
        List<EnhancedSearchResponse.EnhancedSearchResult> enhancedResults = results.getContent().stream()
                .map(this::mapToEnhancedResult)
                .collect(Collectors.toList());

        // Generate search suggestions
        List<String> suggestions = generateSuggestions(query, enhancedResults);

        // Build facets based on results
        List<EnhancedSearchResponse.SearchFacet> facets = buildFacets(results.getContent());

        // Sort info
        EnhancedSearchResponse.SortInfo sortInfo = EnhancedSearchResponse.SortInfo.builder()
                .field(sortField != null ? sortField : "relevance")
                .order(sortOrder != null ? sortOrder : EnhancedSearchResponse.SortOrder.DESC)
                .build();

        long searchTime = System.currentTimeMillis() - startTime;

        return EnhancedSearchResponse.builder()
                .results(enhancedResults)
                .totalCount(results.getTotalElements())
                .page(page)
                .size(size)
                .totalPages(results.getTotalPages())
                .query(query)
                .originalQuery(query)
                .searchTimeMs(searchTime)
                .facets(facets)
                .suggestions(suggestions)
                .filters(filters)
                .sort(sortInfo)
                .build();
    }

    @Transactional(readOnly = true)
    public EnhancedSearchResponse searchWithFilters(
            EnhancedSearchRequest request) {

        Map<String, List<String>> filters = request.getFilters() != null ? request.getFilters() : new HashMap<>();
        String sortField = request.getSortField() != null ? request.getSortField() : "relevance";
        EnhancedSearchResponse.SortOrder sortOrder = request.getSortOrder() != null ?
                request.getSortOrder() : EnhancedSearchResponse.SortOrder.DESC;

        return enhancedSearch(
                request.getQuery(),
                request.getEntityType(),
                request.getPage(),
                request.getSize(),
                filters,
                sortField,
                sortOrder
        );
    }

    @Transactional(readOnly = true)
    public SearchResultsView getSearchResultsView(
            String query,
            String entityType,
            int page,
            int size,
            List<String> fields) {

        long startTime = System.currentTimeMillis();
        log.info("Getting search results view: query={}, entityType={}", query, entityType);

        Pageable pageable = PageRequest.of(page, size);
        String processedQuery = processQueryForTsQuery(query);

        Page<SearchIndex> results = searchIndexRepository.fullTextSearch(processedQuery, entityType, pageable);

        List<SearchResultsView.SearchResultRow> rows = results.getContent().stream()
                .map(index -> mapToResultRow(index, fields))
                .collect(Collectors.toList());

        long searchTime = System.currentTimeMillis() - startTime;

        return SearchResultsView.builder()
                .results(rows)
                .totalCount(results.getTotalElements())
                .page(page)
                .size(size)
                .totalPages(results.getTotalPages())
                .query(query)
                .searchTimeMs(searchTime)
                .columns(fields)
                .build();
    }

    private EnhancedSearchResponse.EnhancedSearchResult mapToEnhancedResult(SearchIndex index) {
        // Simple highlight extraction
        Map<String, Object> highlights = new HashMap<>();
        if (index.getContent() != null && index.getContent().length() > 200) {
            highlights.put("content", index.getContent().substring(0, 200) + "...");
        }
        if (index.getTitle() != null) {
            highlights.put("title", index.getTitle());
        }

        // Matched fields
        List<String> matchedFields = new ArrayList<>();
        matchedFields.add("title");
        if (index.getContent() != null) {
            matchedFields.add("content");
        }

        return EnhancedSearchResponse.EnhancedSearchResult.builder()
                .id(index.getId())
                .entityType(index.getEntityType())
                .entityId(index.getEntityId())
                .title(index.getTitle())
                .content(index.getContent())
                .relevance(1.0f)
                .highlights(highlights)
                .matchedFields(matchedFields)
                .build();
    }

    private SearchResultsView.SearchResultRow mapToResultRow(SearchIndex index, List<String> fields) {
        Map<String, Object> rowData = new LinkedHashMap<>();

        for (String field : fields) {
            switch (field.toLowerCase()) {
                case "id":
                    rowData.put("id", index.getId());
                    break;
                case "entitytype":
                case "entity_type":
                    rowData.put("entityType", index.getEntityType());
                    break;
                case "title":
                    rowData.put("title", index.getTitle());
                    break;
                case "content":
                    rowData.put("content", index.getContent());
                    break;
                case "createdat":
                case "created_at":
                    rowData.put("createdAt", index.getCreatedAt());
                    break;
                case "updatedat":
                case "updated_at":
                    rowData.put("updatedAt", index.getUpdatedAt());
                    break;
                default:
                    rowData.put(field, null);
            }
        }

        return SearchResultsView.SearchResultRow.builder()
                .id(index.getId())
                .entityType(index.getEntityType())
                .entityId(index.getEntityId())
                .data(rowData)
                .build();
    }

    private List<EnhancedSearchResponse.SearchFacet> buildFacets(List<SearchIndex> results) {
        List<EnhancedSearchResponse.SearchFacet> facets = new ArrayList<>();

        // Entity type facet
        Map<String, Long> entityTypeCounts = results.stream()
                .collect(Collectors.groupingBy(SearchIndex::getEntityType, Collectors.counting()));

        List<EnhancedSearchResponse.FacetBucket> entityTypeBuckets = entityTypeCounts.entrySet().stream()
                .map(e -> EnhancedSearchResponse.FacetBucket.builder()
                        .value(e.getKey())
                        .label(e.getKey())
                        .count(e.getValue())
                        .selected(false)
                        .build())
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());

        facets.add(EnhancedSearchResponse.SearchFacet.builder()
                .field("entityType")
                .label("Entity Type")
                .type(EnhancedSearchResponse.FacetType.TERMS)
                .buckets(entityTypeBuckets)
                .build());

        return facets;
    }

    private List<String> generateSuggestions(String query, List<EnhancedSearchResponse.EnhancedSearchResult> results) {
        List<String> suggestions = new ArrayList<>();

        if (results.isEmpty()) {
            suggestions.add("Try a more general search term");
            suggestions.add("Check the spelling of your search terms");
        }

        // Add spelling suggestions based on title matches
        Set<String> seenTitles = new HashSet<>();
        for (EnhancedSearchResponse.EnhancedSearchResult result : results) {
            if (result.getTitle() != null) {
                String lowerTitle = result.getTitle().toLowerCase();
                if (!seenTitles.contains(lowerTitle)) {
                    seenTitles.add(lowerTitle);
                }
            }
        }

        return suggestions;
    }

    private String processQueryForTsQuery(String query) {
        String[] terms = query.trim().split("\\s+");
        return String.join(" & ", terms);
    }

    /**
     * Inner class for search results view
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SearchResultsView {
        private List<SearchResultRow> results;
        private long totalCount;
        private int page;
        private int size;
        private int totalPages;
        private String query;
        private Long searchTimeMs;
        private List<String> columns;

        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class SearchResultRow {
            private UUID id;
            private String entityType;
            private UUID entityId;
            private Map<String, Object> data;
        }
    }

    /**
     * Request object for enhanced search with filters
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EnhancedSearchRequest {
        private String query;
        private String entityType;
        private int page;
        private int size;
        private Map<String, List<String>> filters;
        private String sortField;
        private EnhancedSearchResponse.SortOrder sortOrder;
    }
}