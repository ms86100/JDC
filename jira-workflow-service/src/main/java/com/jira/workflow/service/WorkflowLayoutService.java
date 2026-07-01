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
    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowLayoutEdgeSyncService workflowLayoutEdgeSyncService;
    private final WorkflowStatusCatalog statusCatalog;
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
        return workflowLayoutRepository
                .findTopByWorkflowIdOrderByLayoutVersionDesc(workflowId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No layout found for workflow: " + workflowId));
    }

    /**
     * Returns persisted layout or builds one from workflow statuses/transitions when missing or empty.
     */
    @Transactional
    public WorkflowLayoutResponse getOrCreateLayout(UUID workflowId, UUID userId) {
        Optional<WorkflowLayout> existing = workflowLayoutRepository
                .findTopByWorkflowIdOrderByLayoutVersionDesc(workflowId);
        if (existing.isPresent()) {
            WorkflowLayout layout = existing.get();
            WorkflowLayoutResponse response = mapToResponse(layout);
            if (response.getNodes() != null && !response.getNodes().isEmpty()) {
                try {
                    workflowLayoutEdgeSyncService.syncLayoutEdges(workflowId);
                } catch (Exception ex) {
                    log.warn(
                            "Layout edge sync failed for workflow {}, returning persisted layout: {}",
                            workflowId,
                            ex.getMessage(),
                            ex);
                }
                return mapToResponse(layout);
            }
        }
        return autoLayout(workflowId, userId);
    }

    public void syncLayoutEdges(UUID workflowId) {
        workflowLayoutEdgeSyncService.syncLayoutEdges(workflowId);
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
                .findByWorkflowIdAndLockedTrue(workflowId);

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

        workflowRepository.findById(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", workflowId));

        WorkflowLayout layout = workflowLayoutRepository
                .findTopByWorkflowIdOrderByLayoutVersionDesc(workflowId)
                .orElse(null);

        if (layout != null) {
            workflowLayoutEdgeRepository.deleteByLayoutId(layout.getId());
            workflowLayoutNodeRepository.deleteByLayoutId(layout.getId());
            layout.setLayoutData("{\"nodes\":[],\"edges\":[]}");
            layout.setLayoutVersion(layout.getLayoutVersion() + 1);
            layout.unlock();
        } else {
            layout = WorkflowLayout.builder()
                    .workflowId(workflowId)
                    .layoutData("{\"nodes\":[],\"edges\":[]}")
                    .layoutVersion(1)
                    .isLocked(false)
                    .build();
            layout = workflowLayoutRepository.save(layout);
        }

        final UUID layoutId = layout.getId();
        List<WorkflowStatus> statuses = workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(workflowId);
        Map<UUID, WorkflowLayoutNode> nodeByStatusId = new HashMap<>();
        Map<String, WorkflowStatusCatalog.StatusMeta> catalog = statusCatalog.loadCatalog();
        double startX = 100;
        double startY = 100;
        double spacingX = 220;
        double spacingY = 120;

        int col = 0;
        int row = 0;
        for (WorkflowStatus status : statuses) {
            WorkflowStatusCatalog.StatusMeta meta = statusCatalog.resolve(status.getStatusId(), catalog);
            String nodeType = (row == 0 && col == 0)
                    ? WorkflowLayoutNode.NODE_TYPE_INITIAL
                    : nodeTypeFromCategory(meta.category());

            WorkflowLayoutNode node = WorkflowLayoutNode.builder()
                    .layoutId(layoutId)
                    .statusId(status.getStatusId())
                    .nodeType(nodeType)
                    .positionX(startX + (col * spacingX))
                    .positionY(startY + (row * spacingY))
                    .width(140.0)
                    .height(56.0)
                    .label(meta.name())
                    .sortOrder(status.getSequence() != null ? status.getSequence() : 0)
                    .build();
            node = workflowLayoutNodeRepository.save(node);
            nodeByStatusId.put(status.getStatusId(), node);

            row++;
            if (row >= 4) {
                row = 0;
                col++;
            }
        }

        createLayoutEdgesForTransitions(layoutId, nodeByStatusId, workflowId);

        layout = workflowLayoutRepository.save(layout);
        return mapToResponse(layout);
    }

    private void createLayoutEdgesForTransitions(
            UUID layoutId,
            Map<UUID, WorkflowLayoutNode> nodeByStatusId,
            UUID workflowId) {
        List<WorkflowTransition> transitions = workflowTransitionRepository.findByWorkflowId(workflowId);
        int edgeOrder = 0;
        for (WorkflowTransition transition : transitions) {
            WorkflowLayoutNode fromNode = nodeByStatusId.get(transition.getFromStatusId());
            WorkflowLayoutNode toNode = nodeByStatusId.get(transition.getToStatusId());
            if (fromNode == null || toNode == null) {
                continue;
            }

            WorkflowLayoutEdge edge = WorkflowLayoutEdge.builder()
                    .layoutId(layoutId)
                    .transitionId(transition.getId())
                    .fromNodeId(fromNode.getId())
                    .toNodeId(toNode.getId())
                    .edgeType(WorkflowLayoutEdge.EDGE_TYPE_CURVED)
                    .sortOrder(transition.getDisplayOrder() != null ? transition.getDisplayOrder() : edgeOrder++)
                    .build();
            workflowLayoutEdgeRepository.save(edge);
        }
    }

    @Transactional
    public WorkflowLayoutResponse syncNodePositions(UUID workflowId, SyncDesignerLayoutRequest request) {
        WorkflowLayout layout = workflowLayoutRepository
                .findTopByWorkflowIdOrderByLayoutVersionDesc(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("No layout found for workflow: " + workflowId));

        for (SyncDesignerLayoutRequest.NodePosition pos : request.getNodes()) {
            if (pos.getNodeId() == null) {
                continue;
            }
            WorkflowLayoutNode node = workflowLayoutNodeRepository.findById(pos.getNodeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Layout node", "id", pos.getNodeId()));
            if (pos.getPositionX() != null) {
                node.setPositionX(pos.getPositionX());
            }
            if (pos.getPositionY() != null) {
                node.setPositionY(pos.getPositionY());
            }
            workflowLayoutNodeRepository.save(node);
        }

        return mapToResponse(layout);
    }

    @Transactional
    public void deleteLayout(UUID layoutId) {
        workflowLayoutEdgeRepository.deleteByLayoutId(layoutId);
        workflowLayoutNodeRepository.deleteByLayoutId(layoutId);
        workflowLayoutRepository.deleteById(layoutId);
    }

    private WorkflowLayoutResponse mapToResponse(WorkflowLayout layout) {
        List<WorkflowLayoutNode> nodes = workflowLayoutNodeRepository.findByLayoutId(layout.getId());
        List<WorkflowLayoutEdge> edges = workflowLayoutEdgeRepository.findByLayoutId(layout.getId());

        // Get current workflow statuses to filter deleted ones
        List<WorkflowStatus> currentStatuses = workflowStatusRepository.findByWorkflowIdOrderBySequenceAsc(layout.getWorkflowId());
        Set<UUID> validStatusIds = currentStatuses.stream()
                .map(WorkflowStatus::getStatusId)
                .collect(Collectors.toSet());

        // Load status catalog for fresh metadata
        Map<String, WorkflowStatusCatalog.StatusMeta> catalog = statusCatalog.loadCatalog();

        // Filter out nodes whose status has been deleted, and resolve fresh metadata
        List<WorkflowLayoutNodeResponse> nodeResponses = nodes.stream()
                .filter(n -> n.getStatusId() != null && validStatusIds.contains(n.getStatusId()))
                .map(n -> {
                    WorkflowStatusCatalog.StatusMeta meta = statusCatalog.resolve(n.getStatusId(), catalog);
                    String nodeType = nodeTypeFromCategory(meta.category());
                    return WorkflowLayoutNodeResponse.builder()
                            .id(n.getId())
                            .statusId(n.getStatusId())
                            .statusName(meta.name())
                            .nodeType(nodeType)
                            .positionX(n.getPositionX())
                            .positionY(n.getPositionY())
                            .width(n.getWidth())
                            .height(n.getHeight())
                            .color(meta.color())
                            .isExpanded(n.getIsExpanded())
                            .label(meta.name())
                            .sortOrder(n.getSortOrder())
                            .build();
                })
                .collect(Collectors.toList());

        // Build set of valid node IDs for edge filtering
        Set<UUID> validNodeIds = nodeResponses.stream()
                .map(WorkflowLayoutNodeResponse::getId)
                .collect(Collectors.toSet());

        // Filter edges to only include those connecting valid nodes
        List<WorkflowLayoutEdgeResponse> edgeResponses = edges.stream()
                .filter(e -> validNodeIds.contains(e.getFromNodeId()) && validNodeIds.contains(e.getToNodeId()))
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

    private String nodeTypeFromCategory(String category) {
        if (category == null) {
            return WorkflowLayoutNode.NODE_TYPE_STANDARD;
        }
        return switch (category.toUpperCase()) {
            case "DONE" -> WorkflowLayoutNode.NODE_TYPE_DONE;
            case "IN_PROGRESS" -> WorkflowLayoutNode.NODE_TYPE_STANDARD;
            default -> WorkflowLayoutNode.NODE_TYPE_STANDARD;
        };
    }
}