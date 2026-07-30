package com.avionics_systems.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Index optimization service for search performance.
 * Provides utilities for index maintenance and optimization.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIndexOptimizationService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Rebuild all search vectors in the index.
     * Useful after bulk imports or data migrations.
     */
    @Transactional
    public Map<String, Object> rebuildSearchIndex() {
        log.info("Starting search index rebuild...");
        Instant start = Instant.now();

        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT jira_search.rebuild_search_index()",
                    Integer.class
            );

            long elapsed = Duration.between(start, Instant.now()).toMillis();

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("records_processed", count);
            result.put("elapsed_ms", elapsed);

            log.info("Search index rebuild completed: {} records in {}ms", count, elapsed);
            return result;

        } catch (Exception e) {
            log.error("Search index rebuild failed: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "failed");
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Optimize the search index by running ANALYZE.
     * Updates statistics for query planner.
     */
    @Transactional
    public Map<String, Object> optimizeIndex() {
        log.info("Optimizing search index...");

        try {
            // Run ANALYZE on the search index table
            jdbcTemplate.execute("ANALYZE jira_search.search_index");

            // Get index statistics
            Map<String, Object> stats = getIndexStats();

            log.info("Index optimization completed");
            return stats;

        } catch (Exception e) {
            log.error("Index optimization failed: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "failed");
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Get current index statistics.
     */
    public Map<String, Object> getIndexStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Get table size
            Long tableSize = jdbcTemplate.queryForObject(
                    "SELECT pg_total_relation_size('jira_search.search_index')",
                    Long.class
            );
            stats.put("table_size_bytes", tableSize);
            stats.put("table_size_mb", tableSize / 1024.0 / 1024.0);

            // Get row count
            Long rowCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM jira_search.search_index",
                    Long.class
            );
            stats.put("total_records", rowCount);

            // Get index sizes
            Map<String, Long> indexSizes = new HashMap<>();
            jdbcTemplate.query(
                    "SELECT indexname, pg_relation_size(indexrelid) as size " +
                            "FROM pg_stat_user_indexes " +
                            "WHERE schemaname = 'jira_search' AND tablename = 'search_index'",
                    (rs, rowNum) -> {
                        indexSizes.put(rs.getString("indexname"), rs.getLong("size"));
                        return null;
                    }
            );
            stats.put("index_sizes", indexSizes);

            // Get index hit ratio
            Double hitRatio = jdbcTemplate.queryForObject(
                    "SELECT " +
                            "(sum(idx_tup_read) - sum(idx_tup_fetch)) * 100.0 / nullif(sum(idx_tup_read), 0) " +
                            "FROM pg_stat_user_indexes " +
                            "WHERE schemaname = 'jira_search' AND indexname LIKE 'idx_%'",
                    Double.class
            );
            stats.put("index_hit_ratio", hitRatio != null ? hitRatio : 100.0);

            stats.put("status", "ok");

        } catch (Exception e) {
            log.error("Failed to get index stats: {}", e.getMessage());
            stats.put("status", "error");
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    /**
     * Analyze and log slow queries (if query logging is enabled).
     */
    public Map<String, Object> analyzeSlowQueries(int limit) {
        log.info("Analyzing slow search queries...");

        Map<String, Object> result = new HashMap<>();

        try {
            // Get slow queries from the log
            String sql = "SELECT query_text, execution_time_ms, result_count, created_at " +
                    "FROM jira_search.search_query_log " +
                    "WHERE execution_time_ms > 100 " +
                    "ORDER BY execution_time_ms DESC " +
                    "LIMIT ?";

            jdbcTemplate.query(sql, new Object[]{limit},
                    (rs, rowNum) -> {
                        log.debug("Slow query: {} ({}ms)",
                                rs.getString("query_text"),
                                rs.getInt("execution_time_ms"));
                        return null;
                    }
            );

            result.put("status", "ok");
            result.put("analyzed_queries", limit);

        } catch (Exception e) {
            log.warn("Query analysis failed (table may not exist): {}", e.getMessage());
            result.put("status", "skipped");
            result.put("reason", "Query log table not available");
        }

        return result;
    }

    /**
     * Vacuum the search index to reclaim space.
     * Note: No @Transactional - PostgreSQL forbids VACUUM inside a transaction.
     */
    public Map<String, Object> vacuumIndex() {
        log.info("Running VACUUM on search index...");

        try {
            jdbcTemplate.execute("VACUUM ANALYZE jira_search.search_index");

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "VACUUM completed successfully");

            return result;

        } catch (Exception e) {
            log.error("VACUUM failed: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "failed");
            result.put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Log a search query for analytics.
     */
    public void logSearchQuery(String query, String entityType, int resultCount, long executionTimeMs) {
        try {
            String queryHash = UUID.nameUUIDFromBytes(query.getBytes()).toString();

            jdbcTemplate.update(
                    "INSERT INTO jira_search.search_query_log (query_text, query_hash, entity_type, result_count, execution_time_ms) " +
                            "VALUES (?, ?, ?, ?, ?)",
                    query, queryHash, entityType, resultCount, executionTimeMs
            );
        } catch (Exception e) {
            log.debug("Failed to log search query: {}", e.getMessage());
        }
    }
}