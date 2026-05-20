package com.jira.plan.service;

import com.jira.plan.entity.IssueDependency;
import com.jira.plan.entity.PlanItem;
import com.jira.plan.entity.WorkingDays;
import com.jira.plan.repository.IssueDependencyRepository;
import com.jira.plan.repository.PlanItemRepository;
import com.jira.plan.repository.WorkingDaysRepository;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleEngine {

    private final PlanItemRepository planItemRepository;
    private final IssueDependencyRepository dependencyRepository;
    private final WorkingDaysService workingDaysService;
    private final WorkingDaysRepository workingDaysRepository;

    public ScheduleResult calculateForwardSchedule(UUID planId, LocalDate projectStartDate) {
        log.info("Calculating forward schedule for plan: {} starting from {}", planId, projectStartDate);

        List<PlanItem> items = planItemRepository.findByPlanIdAndIsActiveTrue(planId);
        List<IssueDependency> dependencies = dependencyRepository.findByPlanId(planId);

        if (items.isEmpty()) {
            return ScheduleResult.builder()
                    .success(true)
                    .message("No items to schedule")
                    .scheduleDates(Map.of())
                    .build();
        }

        Map<UUID, PlanItem> itemMap = items.stream()
                .collect(Collectors.toMap(PlanItem::getId, item -> item));

        Map<UUID, List<UUID>> blockingByItem = buildBlockingMap(dependencies);
        Map<UUID, List<UUID>> blockedByItem = buildBlockedByMap(dependencies);

        Set<UUID> scheduled = new HashSet<>();
        Map<UUID, ScheduledDate> scheduleDates = new HashMap<>();
        Queue<UUID> readyToSchedule = new LinkedList<>();

        for (PlanItem item : items) {
            if (!blockingByItem.containsKey(item.getId()) || blockingByItem.get(item.getId()).isEmpty()) {
                readyToSchedule.offer(item.getId());
            }
        }

        while (!readyToSchedule.isEmpty()) {
            UUID itemId = readyToSchedule.poll();

            if (scheduled.contains(itemId)) {
                continue;
            }

            PlanItem item = itemMap.get(itemId);
            LocalDate startDate = projectStartDate;

            List<UUID> blockers = blockingByItem.get(itemId);
            if (blockers != null && !blockers.isEmpty()) {
                LocalDate maxEndDate = LocalDate.MIN;
                for (UUID blockerId : blockers) {
                    ScheduledDate blockerSchedule = scheduleDates.get(blockerId);
                    if (blockerSchedule != null) {
                        maxEndDate = maxEndDate.isBefore(blockerSchedule.getEndDate())
                                ? blockerSchedule.getEndDate()
                                : maxEndDate;
                    }
                }
                if (!maxEndDate.isEqual(LocalDate.MIN)) {
                    startDate = workingDaysService.addWorkingDays(maxEndDate, 1, getDefaultWorkingDaysConfig());
                }
            }

            int duration = getDurationDays(item);
            LocalDate endDate = workingDaysService.addWorkingDays(startDate, duration - 1, getDefaultWorkingDaysConfig());

            ScheduledDate scheduledDate = new ScheduledDate(startDate, endDate, duration, false);
            scheduleDates.put(itemId, scheduledDate);
            scheduled.add(itemId);

            List<UUID> blocked = blockedByItem.get(itemId);
            if (blocked != null) {
                for (UUID blockedId : blocked) {
                    if (canSchedule(blockedId, blockingByItem, scheduleDates)) {
                        readyToSchedule.offer(blockedId);
                    }
                }
            }
        }

        return ScheduleResult.builder()
                .success(true)
                .scheduleDates(scheduleDates.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> Map.of(
                                        "startDate", e.getValue().getStartDate(),
                                        "endDate", e.getValue().getEndDate(),
                                        "durationDays", e.getValue().getDurationDays()
                                )
                        )))
                .criticalPath(extractCriticalPath(dependencies, scheduleDates))
                .build();
    }

    public ScheduleResult calculateBackwardSchedule(UUID planId, LocalDate projectEndDate) {
        log.info("Calculating backward schedule for plan: {} with end date {}", planId, projectEndDate);

        List<PlanItem> items = planItemRepository.findByPlanIdAndIsActiveTrue(planId);
        List<IssueDependency> dependencies = dependencyRepository.findByPlanId(planId);

        if (items.isEmpty()) {
            return ScheduleResult.builder()
                    .success(true)
                    .message("No items to schedule")
                    .scheduleDates(Map.of())
                    .build();
        }

        Map<UUID, PlanItem> itemMap = items.stream()
                .collect(Collectors.toMap(PlanItem::getId, item -> item));

        Map<UUID, List<UUID>> blockingByItem = buildBlockingMap(dependencies);
        Map<UUID, List<UUID>> blockedByItem = buildBlockedByMap(dependencies);

        Set<UUID> scheduled = new HashSet<>();
        Map<UUID, ScheduledDate> scheduleDates = new HashMap<>();
        Queue<UUID> readyToSchedule = new LinkedList<>();

        for (PlanItem item : items) {
            if (!blockedByItem.containsKey(item.getId()) || blockedByItem.get(item.getId()).isEmpty()) {
                readyToSchedule.offer(item.getId());
            }
        }

        while (!readyToSchedule.isEmpty()) {
            UUID itemId = readyToSchedule.poll();

            if (scheduled.contains(itemId)) {
                continue;
            }

            PlanItem item = itemMap.get(itemId);
            LocalDate endDate = projectEndDate;

            List<UUID> blocked = blockedByItem.get(itemId);
            if (blocked != null && !blocked.isEmpty()) {
                LocalDate minStartDate = LocalDate.MAX;
                for (UUID blockedId : blocked) {
                    ScheduledDate blockedSchedule = scheduleDates.get(blockedId);
                    if (blockedSchedule != null) {
                        minStartDate = minStartDate.isAfter(blockedSchedule.getStartDate())
                                ? blockedSchedule.getStartDate()
                                : minStartDate;
                    }
                }
                if (!minStartDate.isEqual(LocalDate.MAX)) {
                    endDate = workingDaysService.addWorkingDays(minStartDate, -1, getDefaultWorkingDaysConfig());
                }
            }

            int duration = getDurationDays(item);
            LocalDate startDate = workingDaysService.addWorkingDays(endDate, -(duration - 1), getDefaultWorkingDaysConfig());

            ScheduledDate scheduledDate = new ScheduledDate(startDate, endDate, duration, false);
            scheduleDates.put(itemId, scheduledDate);
            scheduled.add(itemId);

            List<UUID> blockers = blockingByItem.get(itemId);
            if (blockers != null) {
                for (UUID blockerId : blockers) {
                    if (canScheduleBackward(blockerId, blockedByItem, scheduleDates)) {
                        readyToSchedule.offer(blockerId);
                    }
                }
            }
        }

        return ScheduleResult.builder()
                .success(true)
                .scheduleDates(scheduleDates.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> Map.of(
                                        "startDate", e.getValue().getStartDate(),
                                        "endDate", e.getValue().getEndDate(),
                                        "durationDays", e.getValue().getDurationDays()
                                )
                        )))
                .criticalPath(extractCriticalPath(dependencies, scheduleDates))
                .build();
    }

    public ScheduleResult propagateScheduleChanges(UUID planId, UUID changedItemId, int additionalDays) {
        log.info("Propagating schedule change: item {} shifted by {} days", changedItemId, additionalDays);

        List<IssueDependency> dependencies = dependencyRepository.findByPlanId(planId);
        Map<UUID, List<UUID>> blockingByItem = buildBlockingMap(dependencies);
        Map<UUID, List<UUID>> blockedByItem = buildBlockedByMap(dependencies);

        Set<UUID> affectedItems = new HashSet<>();
        Queue<UUID> toProcess = new LinkedList<>();
        toProcess.offer(changedItemId);

        while (!toProcess.isEmpty()) {
            UUID itemId = toProcess.poll();
            if (affectedItems.contains(itemId)) {
                continue;
            }
            affectedItems.add(itemId);

            List<UUID> blocked = blockedByItem.get(itemId);
            if (blocked != null) {
                for (UUID blockedId : blocked) {
                    if (!affectedItems.contains(blockedId)) {
                        toProcess.offer(blockedId);
                    }
                }
            }
        }

        List<PlanItem> items = planItemRepository.findByPlanIdAndIsActiveTrue(planId);
        Map<UUID, PlanItem> itemMap = items.stream()
                .collect(Collectors.toMap(PlanItem::getId, item -> item));

        Map<UUID, ScheduledDate> newScheduleDates = new HashMap<>();
        ScheduledDate changedItemSchedule = new ScheduledDate(null, null, 0, true);
        newScheduleDates.put(changedItemId, changedItemSchedule);

        for (UUID affectedId : affectedItems) {
            if (!affectedId.equals(changedItemId)) {
                PlanItem item = itemMap.get(affectedId);
                LocalDate targetDate = item.getTargetDate();
                if (targetDate != null) {
                    newScheduleDates.put(affectedId, new ScheduledDate(
                            targetDate,
                            targetDate.plusDays(additionalDays),
                            0,
                            true
                    ));
                }
            }
        }

        return ScheduleResult.builder()
                .success(true)
                .message("Schedule propagated to " + affectedItems.size() + " items")
                .scheduleDates(newScheduleDates.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> Map.of(
                                        "affected", e.getValue().isAffected(),
                                        "startDate", e.getValue().getStartDate(),
                                        "endDate", e.getValue().getEndDate()
                                )
                        )))
                .affectedItemIds(new ArrayList<>(affectedItems))
                .build();
    }

    private int getDurationDays(PlanItem item) {
        if (item.getTargetDate() != null) {
            return 7;
        }

        Integer storyPoints = item.getStoryPoints();
        if (storyPoints == null) {
            return 5;
        }

        if (storyPoints <= 3) return 2;
        if (storyPoints <= 8) return 5;
        if (storyPoints <= 13) return 10;
        if (storyPoints <= 21) return 15;
        return 20;
    }

    private Map<UUID, List<UUID>> buildBlockingMap(List<IssueDependency> dependencies) {
        Map<UUID, List<UUID>> map = new HashMap<>();
        for (IssueDependency dep : dependencies) {
            map.computeIfAbsent(dep.getBlockedIssueId(), k -> new ArrayList<>()).add(dep.getBlockingIssueId());
        }
        return map;
    }

    private Map<UUID, List<UUID>> buildBlockedByMap(List<IssueDependency> dependencies) {
        Map<UUID, List<UUID>> map = new HashMap<>();
        for (IssueDependency dep : dependencies) {
            map.computeIfAbsent(dep.getBlockingIssueId(), k -> new ArrayList<>()).add(dep.getBlockedIssueId());
        }
        return map;
    }

    private boolean canSchedule(UUID itemId, Map<UUID, List<UUID>> blockingByItem, Map<UUID, ScheduledDate> scheduleDates) {
        List<UUID> blockers = blockingByItem.get(itemId);
        if (blockers == null || blockers.isEmpty()) {
            return true;
        }
        for (UUID blockerId : blockers) {
            if (!scheduleDates.containsKey(blockerId)) {
                return false;
            }
        }
        return true;
    }

    private boolean canScheduleBackward(UUID itemId, Map<UUID, List<UUID>> blockedByItem, Map<UUID, ScheduledDate> scheduleDates) {
        List<UUID> blocked = blockedByItem.get(itemId);
        if (blocked == null || blocked.isEmpty()) {
            return true;
        }
        for (UUID blockedId : blocked) {
            if (!scheduleDates.containsKey(blockedId)) {
                return false;
            }
        }
        return true;
    }

    private List<UUID> extractCriticalPath(List<IssueDependency> dependencies, Map<UUID, ScheduledDate> scheduleDates) {
        if (dependencies.isEmpty() || scheduleDates.isEmpty()) {
            return List.of();
        }

        List<IssueDependency> sorted = dependencies.stream()
                .sorted(Comparator.comparing(d ->
                        scheduleDates.getOrDefault(d.getBlockedIssueId(), new ScheduledDate(null, null, 0, false))
                                .getEndDate() != null
                                ? scheduleDates.get(d.getBlockedIssueId()).getEndDate()
                                : LocalDate.MAX))
                .toList();

        List<UUID> path = new ArrayList<>();
        UUID lastItem = null;
        for (IssueDependency dep : sorted) {
            if (lastItem == null || dep.getBlockingIssueId().equals(lastItem)) {
                path.add(dep.getBlockedIssueId());
                lastItem = dep.getBlockedIssueId();
            }
        }

        return path;
    }

    private WorkingDays getDefaultWorkingDaysConfig() {
        return workingDaysRepository.findByIsDefaultTrue()
                .orElseGet(() -> WorkingDays.builder()
                        .name("Default")
                        .monday(true)
                        .tuesday(true)
                        .wednesday(true)
                        .thursday(true)
                        .friday(true)
                        .saturday(false)
                        .sunday(false)
                        .hoursPerDay(java.math.BigDecimal.valueOf(8))
                        .isDefault(true)
                        .build());
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ScheduledDate {
        private LocalDate startDate;
        private LocalDate endDate;
        private int durationDays;
        @lombok.Builder.Default
        private boolean affected = false;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ScheduleResult {
        @Builder.Default
        private boolean success = true;
        private String message;
        private Map<UUID, Map<String, Object>> scheduleDates;
        private List<UUID> criticalPath;
        @Builder.Default
        private List<UUID> affectedItemIds = List.of();
    }
}