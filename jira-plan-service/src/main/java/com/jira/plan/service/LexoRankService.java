package com.jira.plan.service;

import com.jira.plan.entity.LexoRank;
import com.jira.plan.entity.LexoRankBalancer;
import com.jira.plan.repository.LexoRankBalancerRepository;
import com.jira.plan.repository.LexoRankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * LexoRank service implementing Jira's gap-based ordering algorithm.
 * Provides ranking operations with locking support for concurrent edits.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LexoRankService {

    private static final String CHARSET = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final int MIN_GAP = 1;
    private static final Long DEFAULT_BUCKET = 0L;
    private static final String ENTITY_TYPE_PLAN_ITEM = "PLAN_ITEM";

    private final LexoRankRepository lexoRankRepository;
    private final LexoRankBalancerRepository balancerRepository;

    /**
     * Generate a rank between two existing ranks.
     * If both are null, returns initial middle rank.
     * If rankBefore is null, returns rank before first item.
     * If rankAfter is null, returns rank after last item.
     */
    public String rankBetween(String rankBefore, String rankAfter) {
        if (rankBefore == null && rankAfter == null) {
            return getMiddleRank();
        }
        if (rankBefore == null) {
            return rankBeforeFirst(rankAfter);
        }
        if (rankAfter == null) {
            return rankAfterLast(rankBefore);
        }
        return calculateMidpoint(rankBefore, rankAfter);
    }

    /**
     * Get rank before first item in list.
     */
    public String rankBeforeFirst(String firstRank) {
        if (firstRank == null) {
            return getMiddleRank();
        }
        // Prepend 0 and calculate midpoint with first rank
        return calculateMidpoint(CHARSET.charAt(0) + "", firstRank);
    }

    /**
     * Get rank after last item in list.
     */
    public String rankAfterLast(String lastRank) {
        if (lastRank == null) {
            return getMiddleRank();
        }
        // Append midpoint character
        return lastRank + CHARSET.charAt(CHARSET.length() / 2);
    }

    /**
     * Generate initial middle rank.
     */
    private String getMiddleRank() {
        return String.valueOf(CHARSET.charAt(CHARSET.length() / 2));  // "i"
    }

    /**
     * Calculate midpoint between two rank strings.
     */
    private String calculateMidpoint(String before, String after) {
        int maxLen = Math.max(before.length(), after.length());
        String paddedBefore = padRight(before, maxLen);
        String paddedAfter = padRight(after, maxLen);

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < maxLen; i++) {
            int beforeVal = CHARSET.indexOf(paddedBefore.charAt(i));
            int afterVal = CHARSET.indexOf(paddedAfter.charAt(i));

            if (beforeVal < afterVal - 1) {
                result.append(CHARSET.charAt((beforeVal + afterVal) / 2));
                return result.toString();
            } else {
                result.append(paddedBefore.charAt(i));
            }
        }

        // Ranks are adjacent, append midpoint
        result.append(CHARSET.charAt(CHARSET.length() / 2));
        return result.toString();
    }

    private String padRight(String s, int length) {
        while (s.length() < length) {
            s = s + CHARSET.charAt(0);
        }
        return s;
    }

    /**
     * Check if rebalancing is needed due to tight ranks.
     */
    public boolean needsRebalancing(String rank1, String rank2) {
        if (rank1 == null || rank2 == null) return false;
        if (rank1.length() != rank2.length()) return false;
        // Check if ranks are the same length and adjacent
        return rank1.compareTo(rank2) > 0 && rank1.length() >= 10;
    }

    /**
     * Create or update rank entry for an entity.
     */
    @Transactional
    public LexoRank setRank(String entityType, UUID entityId, String rankValue, UUID userId) {
        Optional<LexoRank> existing = lexoRankRepository.findByEntityTypeAndEntityId(entityType, entityId);

        LexoRank lexoRank;
        if (existing.isPresent()) {
            lexoRank = existing.get();
            String oldRank = lexoRank.getRankValue();
            lexoRank.setRankValue(rankValue);
            lexoRank = lexoRankRepository.save(lexoRank);

            // Log rank change
            logRankChange(entityType, entityId, "RANK", oldRank, rankValue, userId);
        } else {
            lexoRank = LexoRank.builder()
                .entityType(entityType)
                .entityId(entityId)
                .bucketId(DEFAULT_BUCKET)
                .rankValue(rankValue)
                .locked(false)
                .build();
            lexoRank = lexoRankRepository.save(lexoRank);
        }

        // Update balancer's last rank
        updateBalancerLastRank(DEFAULT_BUCKET, rankValue);

        return lexoRank;
    }

    /**
     * Get rank for an entity.
     */
    @Transactional(readOnly = true)
    public Optional<LexoRank> getRank(String entityType, UUID entityId) {
        return lexoRankRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Lock rank for editing by a specific user.
     */
    @Transactional
    public LexoRank lockRank(String entityType, UUID entityId, UUID userId) {
        LexoRank lexoRank = lexoRankRepository.findByEntityTypeAndEntityId(entityType, entityId)
            .orElseThrow(() -> new IllegalArgumentException("Rank not found for entity"));

        if (Boolean.TRUE.equals(lexoRank.getLocked()) && !userId.equals(lexoRank.getLockedBy())) {
            throw new IllegalStateException("Rank is already locked by another user");
        }

        lexoRank.lock(userId);
        lexoRank = lexoRankRepository.save(lexoRank);

        logRankChange(entityType, entityId, "LOCK", null, null, userId);

        return lexoRank;
    }

    /**
     * Unlock rank.
     */
    @Transactional
    public LexoRank unlockRank(String entityType, UUID entityId, UUID userId) {
        LexoRank lexoRank = lexoRankRepository.findByEntityTypeAndEntityId(entityType, entityId)
            .orElseThrow(() -> new IllegalArgumentException("Rank not found for entity"));

        if (!userId.equals(lexoRank.getLockedBy())) {
            throw new IllegalStateException("Cannot unlock rank locked by another user");
        }

        lexoRank.unlock();
        lexoRank = lexoRankRepository.save(lexoRank);

        logRankChange(entityType, entityId, "UNLOCK", null, null, userId);

        return lexoRank;
    }

    /**
     * Rebalance all ranks in a bucket.
     */
    @Transactional
    public void rebalanceBucket(Long bucketId) {
        List<LexoRank> entries = lexoRankRepository.findByBucketIdOrderByRankValueAsc(bucketId);

        String prevRank = null;
        for (LexoRank entry : entries) {
            String newRank = rankBetween(prevRank, null);
            entry.setRankValue(newRank);
            lexoRankRepository.save(entry);
            prevRank = newRank;
        }

        // Update balancer
        if (!entries.isEmpty()) {
            updateBalancerLastRank(bucketId, prevRank);
            LexoRankBalancer balancer = balancerRepository.findByBucketIndex(bucketId.intValue())
                .orElseGet(() -> createBalancer(bucketId.intValue()));
            balancer.setLastBalancedAt(LocalDateTime.now());
            balancerRepository.save(balancer);
        }

        log.info("Rebalanced {} entries in bucket {}", entries.size(), bucketId);
    }

    /**
     * Get all ranks for an entity type, ordered.
     */
    @Transactional(readOnly = true)
    public List<LexoRank> getRanksForEntityType(String entityType) {
        return lexoRankRepository.findByEntityTypeAndBucketIdOrderByRankValueAsc(entityType, DEFAULT_BUCKET);
    }

    /**
     * Check if bucket needs rebalancing.
     */
    @Transactional(readOnly = true)
    public boolean bucketNeedsRebalancing(Long bucketId) {
        Optional<LexoRankBalancer> balancer = balancerRepository.findByBucketIndex(bucketId.intValue());
        if (balancer.isEmpty()) return false;

        List<LexoRank> entries = lexoRankRepository.findByBucketIdOrderByRankValueAsc(bucketId);
        if (entries.isEmpty()) return false;

        // Check if last entry is getting too long
        String lastRank = entries.get(entries.size() - 1).getRankValue();
        return lastRank.length() > balancer.get().getBalanceThreshold() * 2;
    }

    private void updateBalancerLastRank(Long bucketId, String rank) {
        LexoRankBalancer balancer = balancerRepository.findByBucketIndex(bucketId.intValue())
            .orElseGet(() -> createBalancer(bucketId.intValue()));
        balancer.setLastRank(rank);
        balancerRepository.save(balancer);
    }

    private LexoRankBalancer createBalancer(int bucketIndex) {
        return balancerRepository.save(LexoRankBalancer.builder()
            .bucketIndex(bucketIndex)
            .balanceThreshold(5)
            .build());
    }

    private void logRankChange(String entityType, UUID entityId, String operation, String oldRank, String newRank, UUID userId) {
        // Could implement audit logging here
        log.debug("LexoRank {}: {} -> {} for {}:{}", operation, oldRank, newRank, entityType, entityId);
    }
}