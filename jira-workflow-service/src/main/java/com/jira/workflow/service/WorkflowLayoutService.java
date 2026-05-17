package com.jira.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.workflow.dto.*;
import com.jira.workflow.entity.*;
import com.jira.workflow.exception.ResourceNotFoundException;
import com.jira.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowLayoutService {

    private final WorkflowLayoutRepository workflowLayoutRepository;
    private final WorkflowLayoutNodeRepository workflowLayoutNodeRepository;
    private final WorkflowLayoutEdgeRepository workflowLayoutEdgeRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public WorkflowLayoutResponse createOrUpdateLayout(UUID workflowId, String layoutData, UUID userId) {
        log.info("Creating/updating layout for workflow: {}", workflowId);

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));

        Optional<WorkflowLayout> existingLayout = workflowLayoutRepository
                .findTopByWorkflowIdOrderByLayoutVersionDesc(workflowId);

        WorkflowLayout layout;
        if (existingLayout.isPresent() && existingLayout.get().getIsLocked() &&
                !userId.equals(existingLayout.get().getLockedBy())) {
            throw new IllegalStateException("Layout is locked by another user");
        }

        if (existingLayout.isPresent()) {
            layout = existingLayout.get();
            layout.setLayoutData(layoutData);
            layout.setLayoutVersion(layout.getLayoutVersion() + 1);
        } else {
            layout = WorkflowLayout.builder()
                    .workflowId(workflowId)
                    .layoutData(layoutData)
                    .layoutVersion(1)
                    .isLocked(false)
                    .build();
        }

        layout = workflowLayoutRepository.save(layout);
        log.info("Layout saved: {} (version {})", layout.getId(), layout.getLayoutVersion());

        return mapToResponse(layout);
    }

    @Transactional(readOnly = true)
    public WorkflowLayoutResponse getLayout(UUID workflowId) {
        WorkflowLayout layout = workflowLayoutRepository
                .findTopByWorkflowIdOrderByLayoutVersionDesc(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("No layout found for workflow: " + workflowId));

        return mapToResponse(layout);
    }

    @Transactional(readOnly = true)
    public List<WorkflowLayoutResponse> getAllLayouts(UUID workflowId) {
        return workflowLayoutRepository.findByWorkflowId(workflowId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkflowLayoutResponse lockLayout(UUID workflowId, UUID userId) {
        log.info("Locking layout for workflow: {} by user: {}", workflowId, userId);

        Optional<WorkflowLayout> lockedLayout = workflowLayoutRepository
                .findByWorkflowIdAndIsLockedTrue(workflowId);

        if (lockedLayout.isPresent() && !userId.equals(lockedLayout.get().getLockedBy())) {
            throw new IllegalStateException("Layout is already locked by another user");
        }

        WorkflowLayout layout = workflowLayoutRepository
                .findTopByWorkflowIdOrderByLayoutVersionDesc(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("No layout found for workflow: " + workflowId));

        layout.lock(userId);
        layout = workflowLayoutRepository.save(layout);

        return mapToResponse(layout);
    }

    @Transactional
    public WorkflowLayoutResponse unlockLayout(UUID workflowId, UUID userId) {
        log.info("Unlocking layout for workflow: {} by user: {}", workflowId, userId);

        WorkflowLayout layout = workflowLayoutRepository
                .findTopByWorkflowIdOrderByLayoutVersionDesc(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("No layout found for workflow: " + workflowId));

        if (layout.getIsLocked() && !userId.equals(layout.getLockedBy())) {
            throw new IllegalStateException("Cannot unlock layout locked by another user");
        }

        layout.unlock();
        layout = workflowLayoutRepository.save(layout);

        return mapToResponse(layout);
    }

    @Transactional
    public WorkflowLayoutResponse autoLayout(UUID workflowId, UUID userId) {
        log.info("Auto-layouting workflow: {}", workflowId);

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));

        List<WorkflowStatus> statuses = workflow.getStatuses();
        Map<UUID, WorkflowLayoutNode> nodeMap = new HashMap<>();
        double startX = 100;
        double startY = 100;
        double spacingX = 200;
        double spacingY = 150;

        int col = 0;
        int row = 0;
        for (WorkflowStatus status : statuses) {
            WorkflowLayoutNode node = WorkflowLayoutNode.builder()
                    .layoutId(null)
                    .statusId(status.getStatusId())
                    .nodeType(WorkflowLayoutNode.NODE_TYPE_STANDARD)
                    .positionX(startX + (col * spacingX))
                    .positionY(startY + (row * spacingY))
                    .width(120.0)
                    .height(60.0)
                    .sortOrder(status.getSequence())
                    .build();
            node = workflowLayoutNodeRepository.save(node);
            nodeMap.put(status.getId(), node);

            row++;
            if (row >= 5) {
                row = 0;
                col++;
            }
        }

        List<WorkflowTransition> transitions = workflowTransitionRepository.findByWorkflowId(workflowId);
        for (WorkflowTransition transition : transitions) {
            WorkflowLayoutNode fromNode = findNodeByStatusId(nodeMap, transition.getFromStatusId());
            WorkflowLayoutNode toNode = findNodeByStatusId(nodeMap, transition.getToStatusId());

            WorkflowLayoutEdge edge = WorkflowLayoutEdge.builder()
                    .layoutId(null)
                    .transitionId(transition.getId())
                    .fromNodeId(fromNode != null ? fromNode.getId() : null)
                    .toNodeId(toNode != null ? toNode.getId() : null)
                    .edgeType(WorkflowLayoutEdge.EDGE_TYPE_CURVED)
                    .sortOrder(transition.getDisplayOrder())
                    .build();
            workflowLayoutEdgeRepository.save(edge);
        }

        WorkflowLayout layout = workflowLayoutRepository
                .findTopByWorkflowIdOrderByLayoutVersionDesc(workflowId)
                .orElse(null);

        if (layout != null) {
            layout.unlock();
            workflowLayoutRepository.save(layout);
        }

        return getLayout(workflowId);
    }

    @Transactional
    public void deleteLayout(UUID layoutId) {
        workflowLayoutNodeRepository.deleteByLayoutId(layoutId);
        workflowLayoutEdgeRepository.deleteByLayoutId(layoutId);
        workflowLayoutRepository.deleteById(layoutId);
    }

    private WorkflowLayoutNode findNodeByStatusId(Map<UUID, WorkflowLayoutNode> nodeMap, UUID statusId) {
        return nodeMap.values().stream()
                .filter(n -> n.getStatusId().equals(statusId))
                .findFirst()
                .orElse(null);
    }

    private WorkflowLayoutResponse mapToResponse(WorkflowLayout layout) {
        List<WorkflowLayoutNode> nodes = workflowLayoutNodeRepository.findByLayoutId(layout.getId());
        List<WorkflowLayoutEdge> edges = workflowLayoutEdgeRepository.findByLayoutId(layout.getId());

        List<WorkflowLayoutNodeResponse> nodeResponses = nodes.stream()
                .map(n -> WorkflowLayoutNodeResponse.builder()
                        .id(n.getId())
                        .statusId(n.getStatusId())
                        .nodeType(n.getNodeType())
                        .positionX(n.getPositionX())
                        .positionY(n.getPositionY())
                        .width(n.getWidth())
                        .height(n.getHeight())
                        .color(n.getColor())
                        .isExpanded(n.getIsExpanded())
                        .label(n.getLabel())
                        .sortOrder(n.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        List<WorkflowLayoutEdgeResponse> edgeResponses = edges.stream()
                .map(e -> WorkflowLayoutEdgeResponse.builder()
                        .id(e.getId())
                        .transitionId(e.getTransitionId())
                        .fromNodeId(e.getFromNodeId())
                        .toNodeId(e.getToNodeId())
                        .edgeType(e.getEdgeType())
                        .pathPoints(e.getPathPoints())
                        .labelOffsetX(e.getLabelOffsetX())
                        .labelOffsetY(e.getLabelOffsetY())
                        .isLooped(e.getIsLooped())
                        .sortOrder(e.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        return WorkflowLayoutResponse.builder()
                .id(layout.getId())
                .workflowId(layout.getWorkflowId())
                .layoutData(layout.getLayoutData())
                .layoutVersion(layout.getLayoutVersion())
                .isLocked(layout.getIsLocked())
                .lockedBy(layout.getLockedBy())
                .lockedAt(layout.getLockedAt())
                .nodes(nodeResponses)
                .edges(edgeResponses)
                .createdAt(layout.getCreatedAt())
                .updatedAt(layout.getUpdatedAt())
                .build();
    }
}