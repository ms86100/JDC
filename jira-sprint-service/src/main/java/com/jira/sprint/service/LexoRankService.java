package com.jira.sprint.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * LexoRank implementation for Jira-style issue ranking.
 * Provides string-based ranking that allows efficient insertion between any two ranks.
 *
 * LexoRank format: "rank|0000000000" - prefix + 10-digit zero-padded number
 * This allows ~10 billion rankings per bucket, with midpoints for insertion.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LexoRankService {

    private static final String RANK_PREFIX = "rank|";
    private static final long INITIAL_RANK = 5000000000L; // Start at middle
    private static final long MIN_RANK = 0;
    private static final long MAX_RANK = 9999999999L;
    private static final long BUCKET_SIZE = 10000000000L;

    /**
     * Generate initial rank for a new issue at the end of a list.
     */
    public String generateInitialRank() {
        return RANK_PREFIX + String.format("%010d", INITIAL_RANK);
    }

    /**
     * Generate rank for a new issue at the beginning of a list.
     */
    public String generateRankBefore(String currentFirstRank) {
        if (currentFirstRank == null || currentFirstRank.isBlank()) {
            return generateInitialRank();
        }

        long currentValue = parseRankValue(currentFirstRank);
        long newValue = currentValue / 2; // Go halfway to start

        if (newValue <= MIN_RANK) {
            // Need to re-index - return a rank that indicates rebalancing needed
            return RANK_PREFIX + String.format("%010d", 1L);
        }

        return RANK_PREFIX + String.format("%010d", newValue);
    }

    /**
     * Generate rank to insert between two existing ranks.
     */
    public String generateRankBetween(String rankBefore, String rankAfter) {
        long beforeValue = rankBefore != null ? parseRankValue(rankBefore) : MIN_RANK;
        long afterValue = rankAfter != null ? parseRankValue(rankAfter) : MAX_RANK;

        // If ranks are equal or too close, return midpoint
        long midpoint = (beforeValue + afterValue) / 2;

        if (midpoint == beforeValue || midpoint == afterValue) {
            // Cannot insert between - need rebalancing
            // Return a value just after beforeValue
            midpoint = beforeValue + 1;
            if (midpoint >= afterValue) {
                log.warn("LexoRank overflow - rebalancing needed between {} and {}", rankBefore, rankAfter);
                return RANK_PREFIX + String.format("%010d", beforeValue + 1000000);
            }
        }

        return RANK_PREFIX + String.format("%010d", midpoint);
    }

    /**
     * Generate rank for issue at the end of the list.
     */
    public String generateRankAfter(String currentLastRank) {
        if (currentLastRank == null || currentLastRank.isBlank()) {
            return generateInitialRank();
        }

        long currentValue = parseRankValue(currentLastRank);
        long newValue = currentValue + 100000000; // Add one bucket worth

        if (newValue > MAX_RANK) {
            log.warn("LexoRank overflow - rebalancing needed for rank {}", currentLastRank);
            return RANK_PREFIX + String.format("%010d", MAX_RANK - 100);
        }

        return RANK_PREFIX + String.format("%010d", newValue);
    }

    /**
     * Compare two ranks. Returns negative if rank1 < rank2, positive if rank1 > rank2, 0 if equal.
     */
    public int compare(String rank1, String rank2) {
        if (rank1 == null && rank2 == null) return 0;
        if (rank1 == null) return -1;
        if (rank2 == null) return 1;

        long value1 = parseRankValue(rank1);
        long value2 = parseRankValue(rank2);

        return Long.compare(value1, value2);
    }

    /**
     * Check if ranking needs rebalancing (values getting too close).
     */
    public boolean needsRebalancing(String rank1, String rank2) {
        if (rank1 == null || rank2 == null) return false;

        long value1 = parseRankValue(rank1);
        long value2 = parseRankValue(rank2);

        return Math.abs(value2 - value1) < 1000; // Less than 1000 difference
    }

    /**
     * Generate a batch of evenly spaced ranks for rebalancing.
     */
    public List<String> generateRebalancedRanks(int count) {
        List<String> ranks = new ArrayList<>();
        long step = BUCKET_SIZE / (count + 1);

        for (int i = 1; i <= count; i++) {
            long value = step * i;
            ranks.add(RANK_PREFIX + String.format("%010d", value));
        }

        return ranks;
    }

    /**
     * Extract the numeric value from a rank string.
     */
    private long parseRankValue(String rank) {
        if (rank == null || !rank.startsWith(RANK_PREFIX)) {
            return INITIAL_RANK; // Default to middle
        }

        try {
            return Long.parseLong(rank.substring(RANK_PREFIX.length()));
        } catch (NumberFormatException e) {
            log.warn("Invalid rank format: {}, using default", rank);
            return INITIAL_RANK;
        }
    }

    /**
     * Validate a rank string format.
     */
    public boolean isValidRank(String rank) {
        if (rank == null || !rank.startsWith(RANK_PREFIX)) {
            return false;
        }

        try {
            long value = Long.parseLong(rank.substring(RANK_PREFIX.length()));
            return value >= MIN_RANK && value <= MAX_RANK;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Get the minimum rank value.
     */
    public String getMinRank() {
        return RANK_PREFIX + String.format("%010d", MIN_RANK);
    }

    /**
     * Get the maximum rank value.
     */
    public String getMaxRank() {
        return RANK_PREFIX + String.format("%010d", MAX_RANK);
    }
}