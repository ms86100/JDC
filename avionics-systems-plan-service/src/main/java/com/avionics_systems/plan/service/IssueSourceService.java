package com.avionics_systems.plan.service;

import com.avionics_systems.plan.entity.Plan;
import com.avionics_systems.plan.entity.PlanIssueSource;
import com.avionics_systems.plan.entity.PlanItem;
import com.avionics_systems.plan.exception.ResourceNotFoundException;
import com.avionics_systems.plan.repository.PlanIssueSourceRepository;
import com.avionics_systems.plan.repository.PlanItemRepository;
import com.avionics_systems.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueSourceService {

    private final PlanIssueSourceRepository issueSourceRepository;
    private final PlanItemRepository planItemRepository;
    private final PlanRepository planRepository;

    @Transactional
    public PlanIssueSource addIssueSource(UUID planId, PlanIssueSource.SourceType sourceType,
                                         UUID sourceId, String sourceName) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", planId));

        if (issueSourceRepository.existsByPlanIdAndSourceIdAndSourceType(planId, sourceId, sourceType)) {
            throw new IllegalArgumentException(
                    "Source already exists: " + sourceType + " - " + sourceName);
        }

        PlanIssueSource source = PlanIssueSource.builder()
                .plan(plan)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .sourceName(sourceName)
                .isActive(true)
                .build();

        return issueSourceRepository.save(source);
    }

    @Transactional
    public void removeIssueSource(UUID planId, UUID sourceId, PlanIssueSource.SourceType sourceType) {
        PlanIssueSource source = issueSourceRepository
                .findByPlanIdAndSourceIdAndSourceType(planId, sourceId, sourceType)
                .orElseThrow(() -> new ResourceNotFoundException("IssueSource", "id", sourceId));

        source.setIsActive(false);
        issueSourceRepository.save(source);
    }

    @Transactional(readOnly = true)
    public List<PlanIssueSource> getActiveSources(UUID planId) {
        return issueSourceRepository.findByPlanIdAndIsActiveTrue(planId);
    }

    @Transactional(readOnly = true)
    public List<PlanItem> getAggregatedIssues(UUID planId) {
        List<PlanIssueSource> sources = issueSourceRepository.findByPlanIdAndIsActiveTrue(planId);

        Set<String> aggregatedIssueKeys = new HashSet<>();
        List<PlanItem> allItems = new ArrayList<>();

        for (PlanIssueSource source : sources) {
            List<PlanItem> items = fetchIssuesFromSource(source);
            for (PlanItem item : items) {
                if (aggregatedIssueKeys.add(item.getIssueKey())) {
                    allItems.add(item);
                }
            }
        }

        return allItems;
    }

    private List<PlanItem> fetchIssuesFromSource(PlanIssueSource source) {
        return switch (source.getSourceType()) {
            case BOARD -> fetchFromBoard(source);
            case PROJECT -> fetchFromProject(source);
            case FILTER -> fetchFromFilter(source);
        };
    }

    private List<PlanItem> fetchFromBoard(PlanIssueSource source) {
        log.info("Fetching issues from board: {}", source.getSourceName());
        return planItemRepository.findBySourceInfo("BOARD", source.getSourceId().toString());
    }

    private List<PlanItem> fetchFromProject(PlanIssueSource source) {
        log.info("Fetching issues from project: {}", source.getSourceName());
        return planItemRepository.findBySourceInfo("PROJECT", source.getSourceId().toString());
    }

    private List<PlanItem> fetchFromFilter(PlanIssueSource source) {
        log.info("Fetching issues from filter: {}", source.getSourceName());
        return planItemRepository.findBySourceInfo("FILTER", source.getSourceId().toString());
    }

    @Transactional
    public void syncSource(UUID sourceId) {
        PlanIssueSource source = issueSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("IssueSource", "id", sourceId));

        try {
            List<PlanItem> items = fetchIssuesFromSource(source);
            int issueCount = items.size();
            source.markSyncSuccess(issueCount);
            issueSourceRepository.save(source);
            log.info("Successfully synced source {} with {} issues", source.getSourceName(), issueCount);
        } catch (Exception e) {
            source.markSyncError(e.getMessage());
            issueSourceRepository.save(source);
            log.error("Failed to sync source {}: {}", source.getSourceName(), e.getMessage());
        }
    }

    @Transactional
    public void syncAllSources(UUID planId) {
        List<PlanIssueSource> sources = issueSourceRepository.findByPlanIdAndIsActiveTrue(planId);
        for (PlanIssueSource source : sources) {
            syncSource(source.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<PlanIssueSource> getStaleSources(int hoursThreshold) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(hoursThreshold);
        return issueSourceRepository.findStaleSources(threshold);
    }

    @Transactional(readOnly = true)
    public SourceAggregationSummary getAggregationSummary(UUID planId) {
        List<PlanIssueSource> sources = issueSourceRepository.findByPlanIdAndIsActiveTrue(planId);

        int totalIssues = 0;
        int boardCount = 0;
        int projectCount = 0;
        int filterCount = 0;
        List<String> errors = new ArrayList<>();
        LocalDateTime lastSync = null;

        for (PlanIssueSource src : sources) {
            totalIssues += src.getIssueCount() != null ? src.getIssueCount() : 0;

            switch (src.getSourceType()) {
                case BOARD -> boardCount++;
                case PROJECT -> projectCount++;
                case FILTER -> filterCount++;
            }

            if (src.getSyncError() != null) {
                errors.add(src.getSourceName() + ": " + src.getSyncError());
            }

            if (src.getUpdatedAt() != null && (lastSync == null || src.getUpdatedAt().isAfter(lastSync))) {
                lastSync = src.getUpdatedAt();
            }
        }

        return SourceAggregationSummary.builder()
                .planId(planId)
                .totalSources(sources.size())
                .boardCount(boardCount)
                .projectCount(projectCount)
                .filterCount(filterCount)
                .totalIssues(totalIssues)
                .errors(errors)
                .lastSync(lastSync)
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class SourceAggregationSummary {
        private UUID planId;
        private int totalSources;
        private int boardCount;
        private int projectCount;
        private int filterCount;
        private int totalIssues;
        private List<String> errors;
        private LocalDateTime lastSync;
    }
}