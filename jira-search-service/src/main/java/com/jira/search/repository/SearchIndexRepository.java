package com.jira.search.repository;

import com.jira.search.entity.SearchIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, UUID> {

    Optional<SearchIndex> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    @Modifying
    @Query("DELETE FROM SearchIndex s WHERE s.entityType = :entityType AND s.entityId = :entityId")
    int deleteByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") UUID entityId);

    @Query(value = "SELECT s.*, ts_rank(s.search_vector, to_tsquery('english', :query)) AS rank " +
            "FROM jira_search.search_index s " +
            "WHERE s.search_vector @@ to_tsquery('english', :query) " +
            "AND (:entityType IS NULL OR s.entity_type = :entityType) " +
            "ORDER BY rank DESC",
            countQuery = "SELECT count(*) FROM jira_search.search_index s " +
                    "WHERE s.search_vector @@ to_tsquery('english', :query) " +
                    "AND (:entityType IS NULL OR s.entity_type = :entityType)",
            nativeQuery = true)
    Page<SearchIndex> fullTextSearch(
            @Param("query") String query,
            @Param("entityType") String entityType,
            Pageable pageable
    );
}