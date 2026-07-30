package com.avionics_systems.search.service;

import com.avionics_systems.search.dto.IndexRequest;
import com.avionics_systems.search.dto.IndexResponse;
import com.avionics_systems.search.dto.SearchResponse;
import com.avionics_systems.search.entity.SearchIndex;
import com.avionics_systems.search.exception.ResourceNotFoundException;
import com.avionics_systems.search.repository.SearchIndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final SearchIndexRepository searchIndexRepository;

    @Value("${app.messages.entity-indexed:Entity indexed successfully}")
    private String entityIndexedMessage;

    @Transactional
    public IndexResponse indexEntity(IndexRequest request) {
        log.info("Indexing entity: {} {} ", request.getEntityType(), request.getEntityId());

        // Check if already exists
        SearchIndex existingIndex = searchIndexRepository
                .findByEntityTypeAndEntityId(request.getEntityType(), request.getEntityId())
                .orElse(null);

        SearchIndex searchIndex;
        if (existingIndex != null) {
            // Update existing
            existingIndex.setTitle(request.getTitle());
            existingIndex.setContent(request.getContent());
            searchIndex = searchIndexRepository.save(existingIndex);
            log.info("Updated existing index entry: {}", searchIndex.getId());
        } else {
            // Create new
            searchIndex = SearchIndex.builder()
                    .entityType(request.getEntityType())
                    .entityId(request.getEntityId())
                    .title(request.getTitle())
                    .content(request.getContent())
                    .build();
            searchIndex = searchIndexRepository.save(searchIndex);
            log.info("Created new index entry: {}", searchIndex.getId());
        }

        return IndexResponse.builder()
                .id(searchIndex.getId())
                .entityType(searchIndex.getEntityType())
                .entityId(searchIndex.getEntityId())
                .message(entityIndexedMessage)
                .build();
    }

    @Transactional
    public void removeFromIndex(String entityType, UUID entityId) {
        log.info("Removing from index: {} {}", entityType, entityId);

        int deleted = searchIndexRepository.deleteByEntityTypeAndEntityId(entityType, entityId);

        if (deleted == 0) {
            throw new ResourceNotFoundException("Index entry not found for: " + entityType + " " + entityId);
        }

        log.info("Removed {} index entries", deleted);
    }

    @Transactional(readOnly = true)
    public SearchResponse search(String query, String entityType, int page, int size) {
        log.info("Searching for: {} (entityType: {}, page: {}, size: {})", query, entityType, page, size);

        Pageable pageable = PageRequest.of(page, size);

        // Process the query for PostgreSQL tsquery
        String processedQuery = processQueryForTsQuery(query);

        Page<SearchIndex> results = searchIndexRepository.fullTextSearch(processedQuery, entityType, pageable);

        List<SearchResponse.SearchResult> searchResults = results.getContent().stream()
                .map(index -> SearchResponse.SearchResult.builder()
                        .id(index.getId())
                        .entityType(index.getEntityType())
                        .entityId(index.getEntityId())
                        .title(index.getTitle())
                        .content(index.getContent())
                        .relevance(1.0f) // Default relevance since ts_rank not directly mapped
                        .build())
                .collect(Collectors.toList());

        return SearchResponse.builder()
                .results(searchResults)
                .totalCount(results.getTotalElements())
                .page(page)
                .size(size)
                .build();
    }

    private String processQueryForTsQuery(String query) {
        // Convert simple search terms to tsquery format
        // Replace spaces with & for AND logic, or use | for OR
        String[] terms = query.trim().split("\\s+");
        return String.join(" & ", terms);
    }
}