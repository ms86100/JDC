package com.avionics_systems.issue.service;

import com.avionics_systems.issue.dto.*;
import com.avionics_systems.issue.entity.Issue;
import com.avionics_systems.issue.entity.IssueType;
import com.avionics_systems.issue.exception.InvalidTransitionException;
import com.avionics_systems.issue.exception.ResourceNotFoundException;
import com.avionics_systems.issue.repository.IssueRepository;
import com.avionics_systems.issue.repository.IssueTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Issue Hierarchy Service - Manages parent-child relationships and subtasks.
 * Supports Epic -> Story -> Subtask hierarchy like Avionics Systems DC.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IssueHierarchyService {

    private final IssueRepository issueRepository;
    private final IssueTypeRepository issueTypeRepository;

    @Value("${app.hierarchy.max-depth:10}")
    private int maxHierarchyDepth;

    /**
     * Set parent issue for an issue.
     */
    @Transactional
    public IssueResponse setParentIssue(UUID issueId, UUID parentIssueId, UUID userId) {
        log.info("Setting parent {} for issue {} by user {}", parentIssueId, issueId, userId);

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        Issue parentIssue = issueRepository.findById(parentIssueId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent Issue", "id", parentIssueId));

        // Validate parent is not a subtask
        if (parentIssue.getIssueType() != null && Boolean.TRUE.equals(parentIssue.getIssueType().getIsSubtask())) {
            throw new InvalidTransitionException("Cannot set a subtask as parent. Subtasks cannot have children.");
        }

        // Prevent circular hierarchy
        if (isCircularHierarchy(issueId, parentIssueId)) {
            throw new InvalidTransitionException("Setting this parent would create a circular hierarchy.");
        }

        // Validate depth limit
        int currentDepth = getHierarchyDepth(issueId);
        int parentDepth = getHierarchyDepth(parentIssueId);
        if (currentDepth + parentDepth + 1 > maxHierarchyDepth) {
            throw new InvalidTransitionException("Hierarchy depth limit exceeded. Maximum depth is " + maxHierarchyDepth);
        }

        issue.setParentIssueId(parentIssueId);
        issue = issueRepository.save(issue);

        log.info("Parent set for issue {}: {}", issue.getIssueKey(), parentIssueId);
        return mapToHierarchyResponse(issue);
    }

    /**
     * Remove parent from an issue (make it standalone).
     */
    @Transactional
    public IssueResponse removeParentIssue(UUID issueId, UUID userId) {
        log.info("Removing parent from issue {} by user {}", issueId, userId);

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        issue.setParentIssueId(null);
        issue = issueRepository.save(issue);

        log.info("Parent removed from issue {}", issue.getIssueKey());
        return mapToHierarchyResponse(issue);
    }

    /**
     * Get all subtasks of an issue.
     */
    @Transactional(readOnly = true)
    public List<IssueResponse> getSubtasks(UUID parentIssueId) {
        List<Issue> subtasks = issueRepository.findByParentIssueId(parentIssueId);
        return subtasks.stream()
                .map(this::mapToHierarchyResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get subtask count for an issue.
     */
    @Transactional(readOnly = true)
    public int getSubtaskCount(UUID issueId) {
        return issueRepository.findByParentIssueId(issueId).size();
    }

    /**
     * Get the parent issue of a given issue.
     */
    @Transactional(readOnly = true)
    public IssueResponse getParentIssue(UUID issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        if (issue.getParentIssueId() == null) {
            return null;
        }

        Issue parentIssue = issueRepository.findById(issue.getParentIssueId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent Issue", "id", issue.getParentIssueId()));

        return mapToHierarchyResponse(parentIssue);
    }

    /**
     * Get the full hierarchy path from root to the issue.
     */
    @Transactional(readOnly = true)
    public List<IssueResponse> getHierarchyPath(UUID issueId) {
        List<IssueResponse> path = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();

        UUID currentId = issueId;
        while (currentId != null) {
            if (visited.contains(currentId)) {
                // Circular reference detected, break to prevent infinite loop
                log.warn("Circular hierarchy detected starting from {}", issueId);
                break;
            }
            visited.add(currentId);

            final UUID visitId = currentId;
            Issue issue = issueRepository.findById(visitId)
                    .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", visitId));

            path.add(0, mapToHierarchyResponse(issue)); // Add to beginning to build path from root

            // Get next parent - use separate final variable for lambda
            UUID nextParent = issue.getParentIssueId();
            currentId = nextParent;
        }

        return path;
    }

    /**
     * Get all descendants of an issue (recursive subtasks).
     */
    @Transactional(readOnly = true)
    public List<IssueResponse> getAllDescendants(UUID issueId) {
        List<IssueResponse> descendants = new ArrayList<>();
        collectDescendants(issueId, descendants, new HashSet<>());
        return descendants;
    }

    private void collectDescendants(UUID parentId, List<IssueResponse> descendants, Set<UUID> visited) {
        if (visited.contains(parentId)) {
            return; // Prevent infinite loops
        }
        visited.add(parentId);

        List<Issue> children = issueRepository.findByParentIssueId(parentId);
        for (Issue child : children) {
            descendants.add(mapToHierarchyResponse(child));
            collectDescendants(child.getId(), descendants, visited);
        }
    }

    /**
     * Convert an issue to a subtask of a parent.
     */
    @Transactional
    public IssueResponse convertToSubtask(UUID issueId, UUID parentIssueId, UUID userId) {
        log.info("Converting issue {} to subtask of {}", issueId, parentIssueId);

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        Issue parentIssue = issueRepository.findById(parentIssueId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent Issue", "id", parentIssueId));

        // Find or create a subtask issue type
        IssueType subtaskType = findOrCreateSubtaskType();

        // Update the issue type to subtask
        issue.setIssueType(subtaskType);
        issue.setParentIssueId(parentIssueId);

        // If issue has children, they need to be re-parented
        List<Issue> currentChildren = issueRepository.findByParentIssueId(issueId);
        if (!currentChildren.isEmpty()) {
            throw new InvalidTransitionException(
                    "Cannot convert issue to subtask because it has " + currentChildren.size() + " child issues. Move or remove children first.");
        }

        issue = issueRepository.save(issue);
        log.info("Issue {} converted to subtask of {}", issue.getIssueKey(), parentIssueId);

        return mapToHierarchyResponse(issue);
    }

    /**
     * Convert a subtask back to a regular issue.
     */
    @Transactional
    public IssueResponse convertFromSubtask(UUID issueId, UUID userId) {
        log.info("Converting subtask {} to regular issue", issueId);

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        if (issue.getIssueType() == null || !Boolean.TRUE.equals(issue.getIssueType().getIsSubtask())) {
            throw new InvalidTransitionException("Issue is not a subtask.");
        }

        // Find or create a task issue type
        IssueType taskType = findOrCreateTaskType();

        issue.setIssueType(taskType);
        issue.setParentIssueId(null); // Remove parent

        issue = issueRepository.save(issue);
        log.info("Subtask {} converted to regular issue", issue.getIssueKey());

        return mapToHierarchyResponse(issue);
    }

    /**
     * Move a subtask to a different parent.
     */
    @Transactional
    public IssueResponse moveSubtaskToParent(UUID issueId, UUID newParentId, UUID userId) {
        log.info("Moving issue {} to new parent {}", issueId, newParentId);

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        Issue newParent = issueRepository.findById(newParentId)
                .orElseThrow(() -> new ResourceNotFoundException("New Parent", "id", newParentId));

        // Validate new parent is not a subtask
        if (newParent.getIssueType() != null && Boolean.TRUE.equals(newParent.getIssueType().getIsSubtask())) {
            throw new InvalidTransitionException("Cannot set a subtask as parent. Subtasks cannot have children.");
        }

        // Prevent circular hierarchy
        if (isCircularHierarchy(issueId, newParentId)) {
            throw new InvalidTransitionException("Moving to this parent would create a circular hierarchy.");
        }

        issue.setParentIssueId(newParentId);
        issue = issueRepository.save(issue);

        log.info("Issue {} moved to new parent {}", issue.getIssueKey(), newParentId);
        return mapToHierarchyResponse(issue);
    }

    /**
     * Get hierarchy statistics for an issue.
     */
    @Transactional(readOnly = true)
    public HierarchyStatsResponse getHierarchyStats(UUID issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", "id", issueId));

        List<IssueResponse> descendants = getAllDescendants(issueId);
        int directSubtaskCount = issueRepository.findByParentIssueId(issueId).size();
        int totalDescendantCount = descendants.size();
        int depth = getHierarchyDepth(issueId);

        return HierarchyStatsResponse.builder()
                .issueId(issueId)
                .issueKey(issue.getIssueKey())
                .directSubtaskCount(directSubtaskCount)
                .totalDescendantCount(totalDescendantCount)
                .hierarchyDepth(depth)
                .hasParent(issue.getParentIssueId() != null)
                .parentIssueId(issue.getParentIssueId())
                .build();
    }

    /**
     * Check if setting a parent would create a circular hierarchy.
     */
    private boolean isCircularHierarchy(UUID issueId, UUID newParentId) {
        Set<UUID> ancestors = new HashSet<>();
        UUID current = newParentId;

        while (current != null) {
            if (current.equals(issueId) || ancestors.contains(current)) {
                return true; // Circular reference detected
            }
            ancestors.add(current);
            Issue ancestor = issueRepository.findById(current).orElse(null);
            current = ancestor != null ? ancestor.getParentIssueId() : null;
        }

        return false;
    }

    /**
     * Get the depth of an issue in the hierarchy (0 = root).
     */
    private int getHierarchyDepth(UUID issueId) {
        int depth = 0;
        Set<UUID> visited = new HashSet<>();
        UUID current = issueId;

        while (current != null) {
            if (visited.contains(current)) {
                break; // Circular reference, stop counting
            }
            visited.add(current);

            Issue issue = issueRepository.findById(current).orElse(null);
            if (issue == null || issue.getParentIssueId() == null) {
                break;
            }
            depth++;
            current = issue.getParentIssueId();
        }

        return depth;
    }

    private IssueType findOrCreateSubtaskType() {
        return issueTypeRepository.findByNameIgnoreCase("subtask")
                .orElseGet(() -> {
                    IssueType subtask = IssueType.builder()
                            .name("Subtask")
                            .issueTypeKey("subtask")
                            .description("A subtask issue")
                            .isSubtask(true)
                            .sequence(99)
                            .build();
                    return issueTypeRepository.save(subtask);
                });
    }

    private IssueType findOrCreateTaskType() {
        return issueTypeRepository.findByNameIgnoreCase("task")
                .orElseGet(() -> {
                    IssueType task = IssueType.builder()
                            .name("Task")
                            .issueTypeKey("task")
                            .description("A task issue")
                            .isSubtask(false)
                            .sequence(10)
                            .build();
                    return issueTypeRepository.save(task);
                });
    }

    private IssueResponse mapToHierarchyResponse(Issue issue) {
        IssueResponse.IssueResponseBuilder builder = IssueResponse.builder()
                .id(issue.getId())
                .projectId(issue.getProjectId())
                .issueKey(issue.getIssueKey())
                .title(issue.getTitle())
                .parentIssueId(issue.getParentIssueId())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt());

        if (issue.getIssueType() != null) {
            builder.issueTypeId(issue.getIssueType().getId())
                    .issueTypeName(issue.getIssueType().getName())
                    .issueTypeIcon(issue.getIssueType().getIcon());
        }

        if (issue.getStatus() != null) {
            builder.statusId(issue.getStatus().getId())
                    .statusName(issue.getStatus().getName());
        }

        // Include subtask count
        int subtaskCount = issueRepository.findByParentIssueId(issue.getId()).size();
        builder.subTaskCount(subtaskCount);

        return builder.build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HierarchyStatsResponse {
        private UUID issueId;
        private String issueKey;
        private int directSubtaskCount;
        private int totalDescendantCount;
        private int hierarchyDepth;
        private boolean hasParent;
        private UUID parentIssueId;
    }
}