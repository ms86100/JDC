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

    private static final Map<String, String> KNOWN_STATUS_NAMES = Map.ofEntries(
            Map.entry("00000000-0000-0000-0001-000000000001", "Backlog"),
            Map.entry("00000000-0000-0000-0001-000000000002", "To Do"),
            Map.entry("00000000-0000-0000-0001-000000000003", "In Progress"),
            Map.entry("00000000-0000-0000-0001-000000000004", "In Review"),
            Map.entry("00000000-0000-0000-0001-000000000005", "Done"),
            Map.entry("00000000-0000-0000-0001-000000000006", "Open"),
            Map.entry("00000000-0000-0000-0001-000000000007", "Resolved"),
            Map.entry("00000000-0000-0000-0001-000000000008", "Closed"),
            Map.entry("00000000-0000-0000-0001-000000000009", "Defined")
    );

    private final WorkflowLayoutRepository workflowLayoutRepository;
    private final WorkflowLayoutNodeRepository workflowLayoutNodeRepository;
    private final WorkflowLayoutEdgeRepository workflowLayoutEdgeRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowStatusRepository workflowStatusRepository;
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
            WorkflowLayoutResponse response = mapToResponse(existing.get());
            if (response.getNodes() != null && !response.getNodes().isEmpty()) {
                return response;
            }
        }
        return autoLayout(workflowId, userId);
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
            workflowLayoutNodeRepository.deleteByLayoutId(layout.getId());
            workflowLayoutEdgeRepository.deleteByLayoutId(layout.getId());
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
        double startX = 100;
        double startY = 100;
        double spacingX = 220;
        double spacingY = 120;

        int col = 0;
        int row = 0;
        for (WorkflowStatus status : statuses) {
            String nodeType = row == 0 && col == 0
                    ? WorkflowLayoutNode.NODE_TYPE_INITIAL
                    : WorkflowLayoutNode.NODE_TYPE_STANDARD;

            WorkflowLayoutNode node = WorkflowLayoutNode.builder()
                    .layoutId(layoutId)
                    .statusId(status.getStatusId())
                    .nodeType(nodeType)
                    .positionX(startX + (col * spacingX))
                    .positionY(startY + (row * spacingY))
                    .width(140.0)
                    .height(56.0)
                    .label(resolveStatusLabel(status.getStatusId(), status.getSequence()))
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

        layout = workflowLayoutRepository.save(layout);
        return mapToResponse(layout);
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
        workflowLayoutNodeRepository.deleteByLayoutId(layoutId);
        workflowLayoutEdgeRepository.deleteByLayoutId(layoutId);
        workflowLayoutRepository.deleteById(layoutId);
    }

    private WorkflowLayoutResponse mapToResponse(WorkflowLayout layout) {
        List<WorkflowLayoutNode> nodes = workflowLayoutNodeRepository.findByLayoutId(layout.getId());
        List<WorkflowLayoutEdge> edges = workflowLayoutEdgeRepository.findByLayoutId(layout.getId());

        List<WorkflowLayoutNodeResponse> nodeResponses = nodes.stream()
                .map(n -> WorkflowLayoutNodeResponse.builder()
                        .id(n.getId())
                        .statusId(n.getStatusId())
                        .statusName(resolveStatusLabel(n.getStatusId(), n.getSortOrder()))
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

    private String resolveStatusLabel(UUID statusId, Integer sequence) {
        if (statusId != null) {
            String known = KNOWN_STATUS_NAMES.get(statusId.toString());
            if (known != null) {
                return known;
            }
        }
        return "Status " + (sequence != null ? sequence + 1 : 1);
    }
}