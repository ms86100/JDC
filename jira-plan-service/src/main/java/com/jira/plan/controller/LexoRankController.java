package com.jira.plan.controller;

import com.jira.plan.dto.request.RankItemRequest;
import com.jira.plan.dto.response.LexoRankResponse;
import com.jira.plan.entity.LexoRank;
import com.jira.plan.service.LexoRankService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for LexoRank ordering operations.
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class LexoRankController {

    private final LexoRankService lexoRankService;

    /**
     * Rank an item between two existing items.
     */
    @PostMapping("/{planId}/backlog/rank")
    public ResponseEntity<LexoRankResponse> rankItem(
            @PathVariable UUID planId,
            @RequestBody RankItemRequest request) {

        // Calculate new rank between beforeRank and afterRank
        String newRank = lexoRankService.rankBetween(
            request.getBeforeRank(),
            request.getAfterRank()
        );

        // Set the rank for the item
        LexoRank lexoRank = lexoRankService.setRank(
            "PLAN_ITEM",
            request.getItemId(),
            newRank,
            request.getUserId()
        );

        return ResponseEntity.ok(toResponse(lexoRank));
    }

    /**
     * Get rank for an item.
     */
    @GetMapping("/backlog/rank/{entityType}/{entityId}")
    public ResponseEntity<LexoRankResponse> getRank(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {

        return lexoRankService.getRank(entityType, entityId)
            .map(this::toResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lock rank for editing.
     */
    @PutMapping("/backlog/rank/lock")
    public ResponseEntity<LexoRankResponse> lockRank(@RequestBody RankItemRequest request) {
        LexoRank lexoRank = lexoRankService.lockRank(
            "PLAN_ITEM",
            request.getItemId(),
            request.getUserId()
        );
        return ResponseEntity.ok(toResponse(lexoRank));
    }

    /**
     * Unlock rank.
     */
    @PutMapping("/backlog/rank/unlock")
    public ResponseEntity<LexoRankResponse> unlockRank(@RequestBody RankItemRequest request) {
        LexoRank lexoRank = lexoRankService.unlockRank(
            "PLAN_ITEM",
            request.getItemId(),
            request.getUserId()
        );
        return ResponseEntity.ok(toResponse(lexoRank));
    }

    /**
     * Check if rebalancing is needed.
     */
    @GetMapping("/{planId}/backlog/rank/validate")
    public ResponseEntity<Boolean> needsRebalancing(
            @PathVariable UUID planId,
            @RequestParam String rank1,
            @RequestParam String rank2) {
        boolean needsRebalance = lexoRankService.needsRebalancing(rank1, rank2);
        return ResponseEntity.ok(needsRebalance);
    }

    /**
     * Trigger rebalance of a bucket.
     */
    @PostMapping("/{planId}/backlog/rank/rebalance")
    public ResponseEntity<Void> rebalance(@PathVariable UUID planId) {
        lexoRankService.rebalanceBucket(0L);  // Default bucket
        return ResponseEntity.ok().build();
    }

    private LexoRankResponse toResponse(LexoRank lexoRank) {
        return LexoRankResponse.builder()
            .id(lexoRank.getId())
            .entityType(lexoRank.getEntityType())
            .entityId(lexoRank.getEntityId())
            .bucketId(lexoRank.getBucketId())
            .rankValue(lexoRank.getRankValue())
            .locked(lexoRank.getLocked())
            .lockedAt(lexoRank.getLockedAt())
            .lockedBy(lexoRank.getLockedBy())
            .createdAt(lexoRank.getCreatedAt())
            .updatedAt(lexoRank.getUpdatedAt())
            .build();
    }
}