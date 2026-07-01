package com.jira.workflow.service;

import com.jira.workflow.entity.*;
import com.jira.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Syncs persisted diagram edges with workflow_transitions (separate tables).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowLayoutEdgeSyncService {

    private final WorkflowLayoutRepository workflowLayoutRepository;
    private final WorkflowLayoutNodeRepository workflowLayoutNodeRepository;
    private final WorkflowLayoutEdgeRepository workflowLayoutEdgeRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;

    @Transactional
    public void syncLayoutEdges(UUID workflowId) {
        Optional<WorkflowLayout> layoutOpt = workflowLayoutRepository
                .findTopByWorkflowIdOrderByLayoutVersionDesc(workflowId);
        if (layoutOpt.isEmpty()) {
            return;
        }

        UUID layoutId = layoutOpt.get().getId();
        List<WorkflowLayoutNode> nodes = workflowLayoutNodeRepository.findByLayoutId(layoutId);
        if (nodes.isEmpty()) {
            return;
        }

        Map<UUID, WorkflowLayoutNode> nodeByStatusId = new HashMap<>();
        for (WorkflowLayoutNode node : nodes) {
            if (node.getStatusId() != null) {
                nodeByStatusId.putIfAbsent(node.getStatusId(), node);
            }
        }

        List<WorkflowTransition> transitions = workflowTransitionRepository.findByWorkflowId(workflowId);
        Set<UUID> activeTransitionIds = transitions.stream()
                .map(WorkflowTransition::getId)
                .collect(Collectors.toSet());

        List<WorkflowLayoutEdge> existingEdges = new ArrayList<>(
                workflowLayoutEdgeRepository.findByLayoutId(layoutId));

        Set<UUID> linkedTransitionIds = new HashSet<>();
        List<UUID> edgeIdsToRemove = new ArrayList<>();
        for (WorkflowLayoutEdge edge : existingEdges) {
            UUID transitionId = edge.getTransitionId();
            if (transitionId == null || !activeTransitionIds.contains(transitionId)) {
                edgeIdsToRemove.add(edge.getId());
                continue;
            }
            linkedTransitionIds.add(transitionId);
        }
        if (!edgeIdsToRemove.isEmpty()) {
            workflowLayoutEdgeRepository.deleteAllById(edgeIdsToRemove);
        }

        int edgeOrder = (int) workflowLayoutEdgeRepository.countByLayoutId(layoutId);
        for (WorkflowTransition transition : transitions) {
            if (linkedTransitionIds.contains(transition.getId())) {
                continue;
            }
            WorkflowLayoutNode fromNode = nodeByStatusId.get(transition.getFromStatusId());
            WorkflowLayoutNode toNode = nodeByStatusId.get(transition.getToStatusId());
            if (fromNode == null || toNode == null) {
                log.warn(
                        "Skipping layout edge for transition {} — missing layout node (from={}, to={})",
                        transition.getId(),
                        transition.getFromStatusId(),
                        transition.getToStatusId());
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
            linkedTransitionIds.add(transition.getId());
        }
    }
}
