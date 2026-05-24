package com.jira.search.service;

import com.jira.search.entity.SearchIndex;
import com.jira.search.repository.SearchIndexRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Index Optimization Service
 * Phase 7 - Polish & Performance
 * Manages PostgreSQL full-text search indexes for optimal query performance
 */
@Service
@Slf4j
public class IndexOptimizationService {

    @PersistenceContext
    private EntityManager entityManager;

    private final SearchIndexRepository searchIndexRepository;

    public IndexOptimizationService(SearchIndexRepository searchIndexRepository) {
        this.searchIndexRepository = searchIndexRepository;
    }

    /**
     * Create optimized GIN index for full-text search
     * This index significantly improves search performance for large datasets
     */
    @Transactional
    public void createSearchIndex() {
        log.info("Creating GIN index for full-text search optimization");

        entityManager.createNativeQuery("""
            CREATE INDEX IF NOT EXISTS idx_search_index_fts
            ON jira_search.search_index
            USING GIN (search_vector gin_trgm_ops)
        """).executeUpdate();

        log.info("GIN index created successfully");
    }

    /**
     * Create B-tree index on entity_type for faster filtering
     */
    @Transactional
    public void createEntityTypeIndex() {
        log.info("Creating B-tree index on entity_type");

        entityManager.createNativeQuery("""
            CREATE INDEX IF NOT EXISTS idx_search_index_entity_type
            ON jira_search.search_index (entity_type)
        """).executeUpdate();

        log.info("Entity type index created successfully");
    }

    /**
     * Create composite index for common query patterns
     */
    @Transactional
    public void createCompositeIndex() {
        log.info("Creating composite index on (entity_type, created_at)");

        entityManager.createNativeQuery("""
            CREATE INDEX IF NOT EXISTS idx_search_index_entity_created
            ON jira_search.search_index (entity_type, created_at DESC)
        """).executeUpdate();

        log.info("Composite index created successfully");
    }

    /**
     * Add trigger to automatically update search_vector on insert/update
     */
    @Transactional
    public void createSearchVectorTrigger() {
        log.info("Creating trigger for automatic search_vector updates");

        // Create trigger function
        entityManager.createNativeQuery("""
            CREATE OR REPLACE FUNCTION jira_search.update_search_vector()
            RETURNS TRIGGER AS $$
            BEGIN
                NEW.search_vector :=
                    setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
                    setweight(to_tsvector('english', COALESCE(NEW.content, '')), 'B');
                RETURN NEW;
            END;
            $$ LANGUAGE plpgsql;
        """).executeUpdate();

        // Create trigger
        entityManager.createNativeQuery("""
            DROP TRIGGER IF EXISTS search_vector_update_trigger
            ON jira_search.search_index;

            CREATE TRIGGER search_vector_update_trigger
            BEFORE INSERT OR UPDATE OF title, content
            ON jira_search.search_index
            FOR EACH ROW
            EXECUTE FUNCTION jira_search.update_search_vector();
        """).executeUpdate();

        log.info("Search vector trigger created successfully");
    }

    /**
     * Rebuild all indexes
     */
    @Transactional
    public void reindexAll() {
        log.info("Starting full reindex");

        entityManager.createNativeQuery("""
            REINDEX INDEX jira_search.idx_search_index_fts;
            REINDEX INDEX jira_search.idx_search_index_entity_type;
            REINDEX INDEX jira_search.idx_search_index_entity_created;
        """).executeUpdate();

        log.info("Full reindex completed");
    }

    /**
     * Analyze tables for query optimization
     */
    @Transactional
    public void analyzeTable() {
        log.info("Analyzing search_index table");

        entityManager.createNativeQuery("""
            ANALYZE jira_search.search_index;
        """).executeUpdate();

        log.info("Table analysis completed");
    }

    /**
     * Get index statistics
     */
    @Transactional(readOnly = true)
    public IndexStats getIndexStats() {
        Long totalRecords = searchIndexRepository.count();

        // Get index sizes
        Object[] idxSize = (Object[]) entityManager.createNativeQuery("""
            SELECT pg_size_pretty(pg_relation_size('jira_search.idx_search_index_fts'))
        """).getSingleResult();

        Object[] tableSize = (Object[]) entityManager.createNativeQuery("""
            SELECT pg_size_pretty(pg_relation_size('jira_search.search_index'))
        """).getSingleResult();

        return new IndexStats(
                totalRecords,
                idxSize != null ? (String) idxSize[0] : "N/A",
                tableSize != null ? (String) tableSize[0] : "N/A"
        );
    }

    /**
     * Optimize all indexes - run maintenance routine
     */
    @Transactional
    public String optimizeAll() {
        log.info("Starting index optimization");

        createSearchIndex();
        createEntityTypeIndex();
        createCompositeIndex();
        createSearchVectorTrigger();
        reindexAll();
        analyzeTable();

        log.info("Index optimization completed");
        return "All indexes optimized successfully";
    }

    public record IndexStats(
            Long totalRecords,
            String indexSize,
            String tableSize
    ) {}
}