package com.jira.workflow.controller;

import com.jira.workflow.dto.WorkflowLayoutResponse;
import com.jira.workflow.dto.WorkflowDiagramResponse;
import com.jira.workflow.entity.WorkflowLayout;
import com.jira.workflow.entity.WorkflowLayoutEdge;
import com.jira.workflow.entity.WorkflowLayoutNode;
import com.jira.workflow.exception.ResourceNotFoundException;
import com.jira.workflow.repository.WorkflowLayoutRepository;
import com.jira.workflow.repository.WorkflowLayoutNodeRepository;
import com.jira.workflow.repository.WorkflowLayoutEdgeRepository;
import com.jira.workflow.repository.WorkflowRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for workflow layout and diagram visualization endpoints.
 * F3-US008: Workflow Visualization
 */
@RestController
@RequestMapping("/api/workflows")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Workflow Layout", description = "Workflow diagram and layout visualization endpoints")
public class WorkflowLayoutController {

    private final WorkflowLayoutRepository workflowLayoutRepository;
    private final WorkflowLayoutNodeRepository workflowLayoutNodeRepository;
    private final WorkflowLayoutEdgeRepository workflowLayoutEdgeRepository;
    private final WorkflowRepository workflowRepository;

    @GetMapping("/{workflowId}/layout")
    @Operation(summary = "Get workflow layout", description = "Returns the layout data for a workflow diagram")
    public ResponseEntity<WorkflowLayoutResponse> getWorkflowLayout(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId) {

        log.info("GET /workflows/{}/layout - Fetching workflow layout", workflowId);

        if (!workflowRepository.existsById(workflowId)) {
            throw new ResourceNotFoundException("Workflow", "id", workflowId);
        }

        WorkflowLayout layout = workflowLayoutRepository.findTopByWorkflowIdOrderByLayoutVersionDesc(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowLayout", "workflowId", workflowId));

        List<WorkflowLayoutNode> nodes = workflowLayoutNodeRepository.findByLayoutIdOrderBySortOrderAsc(layout.getId());
        List<WorkflowLayoutEdge> edges = workflowLayoutEdgeRepository.findByLayoutIdOrderBySortOrderAsc(layout.getId());

        List<com.jira.workflow.dto.WorkflowLayoutNodeResponse> nodeResponses = nodes.stream()
                .map(this::mapToNodeResponse)
                .collect(Collectors.toList());

        List<com.jira.workflow.dto.WorkflowLayoutEdgeResponse> edgeResponses = edges.stream()
                .map(this::mapToEdgeResponse)
                .collect(Collectors.toList());

        WorkflowLayoutResponse response = WorkflowLayoutResponse.builder()
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

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{workflowId}/diagram")
    @Operation(summary = "Get workflow diagram", description = "Returns diagram data optimized for rendering (nodes, edges, metadata)")
    public ResponseEntity<WorkflowDiagramResponse> getWorkflowDiagram(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId,
            @Parameter(description = "Layout version, defaults to latest") @RequestParam(required = false) Integer version) {

        log.info("GET /workflows/{}/diagram - Fetching workflow diagram", workflowId);

        if (!workflowRepository.existsById(workflowId)) {
            throw new ResourceNotFoundException("Workflow", "id", workflowId);
        }

        WorkflowLayout layout;
        if (version != null) {
            layout = workflowLayoutRepository.findByWorkflowId(workflowId).stream()
                    .filter(l -> l.getLayoutVersion().equals(version))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("WorkflowLayout", "version", version));
        } else {
            layout = workflowLayoutRepository.findTopByWorkflowIdOrderByLayoutVersionDesc(workflowId)
                    .orElseThrow(() -> new ResourceNotFoundException("WorkflowLayout", "workflowId", workflowId));
        }

        List<WorkflowLayoutNode> nodes = workflowLayoutNodeRepository.findByLayoutIdOrderBySortOrderAsc(layout.getId());
        List<WorkflowLayoutEdge> edges = workflowLayoutEdgeRepository.findByLayoutIdOrderBySortOrderAsc(layout.getId());

        WorkflowDiagramResponse response = WorkflowDiagramResponse.builder()
                .workflowId(workflowId)
                .layoutId(layout.getId())
                .version(layout.getLayoutVersion())
                .isLocked(layout.getIsLocked())
                .lockedBy(layout.getLockedBy())
                .nodes(nodes.stream()
                        .map(this::mapToDiagramNode)
                        .collect(Collectors.toList()))
                .edges(edges.stream()
                        .map(this::mapToDiagramEdge)
                        .collect(Collectors.toList()))
                .metadata(WorkflowDiagramResponse.DiagramMetadata.builder()
                        .totalNodes(nodes.size())
                        .totalEdges(edges.size())
                        .layoutData(layout.getLayoutData())
                        .build())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{workflowId}/diagram/svg")
    @Operation(summary = "Get workflow diagram as SVG", description = "Returns an SVG representation of the workflow diagram")
    public ResponseEntity<String> getWorkflowDiagramSvg(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId) {

        log.info("GET /workflows/{}/diagram/svg - Generating SVG diagram", workflowId);

        if (!workflowRepository.existsById(workflowId)) {
            throw new ResourceNotFoundException("Workflow", "id", workflowId);
        }

        WorkflowLayout layout = workflowLayoutRepository.findTopByWorkflowIdOrderByLayoutVersionDesc(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowLayout", "workflowId", workflowId));

        List<WorkflowLayoutNode> nodes = workflowLayoutNodeRepository.findByLayoutIdOrderBySortOrderAsc(layout.getId());
        List<WorkflowLayoutEdge> edges = workflowLayoutEdgeRepository.findByLayoutIdOrderBySortOrderAsc(layout.getId());

        String svg = generateSvgDiagram(workflowId, nodes, edges);

        return ResponseEntity.ok()
                .header("Content-Type", "image/svg+xml")
                .body(svg);
    }

    @GetMapping("/{workflowId}/diagram/png")
    @Operation(summary = "Get workflow diagram as PNG", description = "Returns a PNG representation of the workflow diagram")
    public ResponseEntity<byte[]> getWorkflowDiagramPng(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId) {

        log.info("GET /workflows/{}/diagram/png - Generating PNG diagram", workflowId);

        if (!workflowRepository.existsById(workflowId)) {
            throw new ResourceNotFoundException("Workflow", "id", workflowId);
        }

        // For PNG, we generate SVG first then would convert (placeholder for actual conversion)
        // In production, you would use a library like Batik or Apache FOP
        WorkflowLayout layout = workflowLayoutRepository.findTopByWorkflowIdOrderByLayoutVersionDesc(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowLayout", "workflowId", workflowId));

        List<WorkflowLayoutNode> nodes = workflowLayoutNodeRepository.findByLayoutIdOrderBySortOrderAsc(layout.getId());
        List<WorkflowLayoutEdge> edges = workflowLayoutEdgeRepository.findByLayoutIdOrderBySortOrderAsc(layout.getId());

        String svg = generateSvgDiagram(workflowId, nodes, edges);

        // Return SVG as placeholder - actual PNG conversion would require additional dependencies
        return ResponseEntity.ok()
                .header("Content-Type", "image/png")
                .body(svg.getBytes());
    }

    @GetMapping("/{workflowId}/diagram/minimap")
    @Operation(summary = "Get workflow diagram minimap", description = "Returns a simplified minimap of the workflow")
    public ResponseEntity<WorkflowDiagramResponse> getWorkflowDiagramMinimap(
            @Parameter(description = "Workflow ID") @PathVariable UUID workflowId) {

        log.info("GET /workflows/{}/diagram/minimap - Generating minimap", workflowId);

        if (!workflowRepository.existsById(workflowId)) {
            throw new ResourceNotFoundException("Workflow", "id", workflowId);
        }

        WorkflowLayout layout = workflowLayoutRepository.findTopByWorkflowIdOrderByLayoutVersionDesc(workflowId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowLayout", "workflowId", workflowId));

        List<WorkflowLayoutNode> nodes = workflowLayoutNodeRepository.findByLayoutIdOrderBySortOrderAsc(layout.getId());
        List<WorkflowLayoutEdge> edges = workflowLayoutEdgeRepository.findByLayoutIdOrderBySortOrderAsc(layout.getId());

        // Simplified nodes for minimap - just IDs and positions
        List<WorkflowDiagramResponse.DiagramNode> minimapNodes = nodes.stream()
                .map(node -> WorkflowDiagramResponse.DiagramNode.builder()
                        .id(node.getId())
                        .statusId(node.getStatusId())
                        .nodeType(node.getNodeType())
                        .positionX(node.getPositionX() / 4) // Scale down
                        .positionY(node.getPositionY() / 4)
                        .width(30.0)
                        .height(15.0)
                        .label(node.getLabel())
                        .color(node.getColor())
                        .build())
                .collect(Collectors.toList());

        // Simplified edges for minimap
        List<WorkflowDiagramResponse.DiagramEdge> minimapEdges = edges.stream()
                .map(edge -> WorkflowDiagramResponse.DiagramEdge.builder()
                        .id(edge.getId())
                        .transitionId(edge.getTransitionId())
                        .fromNodeId(edge.getFromNodeId())
                        .toNodeId(edge.getToNodeId())
                        .edgeType(edge.getEdgeType())
                        .isLooped(edge.getIsLooped())
                        .build())
                .collect(Collectors.toList());

        WorkflowDiagramResponse response = WorkflowDiagramResponse.builder()
                .workflowId(workflowId)
                .layoutId(layout.getId())
                .version(layout.getLayoutVersion())
                .isLocked(false)
                .nodes(minimapNodes)
                .edges(minimapEdges)
                .metadata(WorkflowDiagramResponse.DiagramMetadata.builder()
                        .totalNodes(nodes.size())
                        .totalEdges(edges.size())
                        .isMinimap(true)
                        .build())
                .build();

        return ResponseEntity.ok(response);
    }

    private com.jira.workflow.dto.WorkflowLayoutNodeResponse mapToNodeResponse(WorkflowLayoutNode node) {
        return com.jira.workflow.dto.WorkflowLayoutNodeResponse.builder()
                .id(node.getId())
                .statusId(node.getStatusId())
                .nodeType(node.getNodeType())
                .positionX(node.getPositionX())
                .positionY(node.getPositionY())
                .width(node.getWidth())
                .height(node.getHeight())
                .color(node.getColor())
                .isExpanded(node.getIsExpanded())
                .label(node.getLabel())
                .sortOrder(node.getSortOrder())
                .build();
    }

    private com.jira.workflow.dto.WorkflowLayoutEdgeResponse mapToEdgeResponse(WorkflowLayoutEdge edge) {
        return com.jira.workflow.dto.WorkflowLayoutEdgeResponse.builder()
                .id(edge.getId())
                .transitionId(edge.getTransitionId())
                .fromNodeId(edge.getFromNodeId())
                .toNodeId(edge.getToNodeId())
                .edgeType(edge.getEdgeType())
                .pathPoints(edge.getPathPoints())
                .labelOffsetX(edge.getLabelOffsetX())
                .labelOffsetY(edge.getLabelOffsetY())
                .isLooped(edge.getIsLooped())
                .sortOrder(edge.getSortOrder())
                .build();
    }

    private WorkflowDiagramResponse.DiagramNode mapToDiagramNode(WorkflowLayoutNode node) {
        return WorkflowDiagramResponse.DiagramNode.builder()
                .id(node.getId())
                .statusId(node.getStatusId())
                .nodeType(node.getNodeType())
                .positionX(node.getPositionX())
                .positionY(node.getPositionY())
                .width(node.getWidth())
                .height(node.getHeight())
                .label(node.getLabel())
                .color(node.getColor())
                .isExpanded(node.getIsExpanded())
                .build();
    }

    private WorkflowDiagramResponse.DiagramEdge mapToDiagramEdge(WorkflowLayoutEdge edge) {
        return WorkflowDiagramResponse.DiagramEdge.builder()
                .id(edge.getId())
                .transitionId(edge.getTransitionId())
                .fromNodeId(edge.getFromNodeId())
                .toNodeId(edge.getToNodeId())
                .edgeType(edge.getEdgeType())
                .pathPoints(edge.getPathPoints())
                .labelOffsetX(edge.getLabelOffsetX())
                .labelOffsetY(edge.getLabelOffsetY())
                .isLooped(edge.getIsLooped())
                .build();
    }

    private String generateSvgDiagram(UUID workflowId, List<WorkflowLayoutNode> nodes, List<WorkflowLayoutEdge> edges) {
        StringBuilder svg = new StringBuilder();
        svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"800\" height=\"600\" viewBox=\"0 0 800 600\">\n");
        svg.append("  <style>\n");
        svg.append("    .node { fill: #ffffff; stroke: #0052cc; stroke-width: 2px; }\n");
        svg.append("    .node-initial { fill: #deebff; stroke: #0052cc; stroke-width: 2px; }\n");
        svg.append("    .node-done { fill: #e3fcef; stroke: #00875a; stroke-width: 2px; }\n");
        svg.append("    .node-label { font-family: Arial; font-size: 12px; fill: #172b4d; }\n");
        svg.append("    .edge { stroke: #6b778c; stroke-width: 2px; fill: none; marker-end: url(#arrowhead); }\n");
        svg.append("  </style>\n");
        svg.append("  <defs>\n");
        svg.append("    <marker id=\"arrowhead\" markerWidth=\"10\" markerHeight=\"7\" refX=\"9\" refY=\"3.5\" orient=\"auto\">\n");
        svg.append("      <polygon points=\"0 0, 10 3.5, 0 7\" fill=\"#6b778c\"/>\n");
        svg.append("    </marker>\n");
        svg.append("  </defs>\n");

        // Generate edges first (so they appear behind nodes)
        for (WorkflowLayoutEdge edge : edges) {
            WorkflowLayoutNode fromNode = nodes.stream()
                    .filter(n -> n.getId().equals(edge.getFromNodeId()))
                    .findFirst()
                    .orElse(null);
            WorkflowLayoutNode toNode = nodes.stream()
                    .filter(n -> n.getId().equals(edge.getToNodeId()))
                    .findFirst()
                    .orElse(null);

            if (fromNode != null && toNode != null) {
                double x1 = fromNode.getPositionX() + fromNode.getWidth() / 2;
                double y1 = fromNode.getPositionY() + fromNode.getHeight() / 2;
                double x2 = toNode.getPositionX() + toNode.getWidth() / 2;
                double y2 = toNode.getPositionY() + toNode.getHeight() / 2;

                String edgeClass = edge.getIsLooped() ? "edge\" stroke-dasharray=\"5,5" : "edge";
                svg.append(String.format("  <line class=\"%s\" x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\"/>\n",
                        edgeClass, x1, y1, x2, y2));
            }
        }

        // Generate nodes
        for (WorkflowLayoutNode node : nodes) {
            String nodeClass = "node";
            if ("INITIAL".equals(node.getNodeType())) {
                nodeClass = "node-initial";
            } else if ("DONE".equals(node.getNodeType())) {
                nodeClass = "node-done";
            }

            svg.append(String.format("  <rect class=\"%s\" x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"4\"/>\n",
                    nodeClass, node.getPositionX(), node.getPositionY(), node.getWidth(), node.getHeight()));

            if (node.getLabel() != null) {
                svg.append(String.format("  <text class=\"node-label\" x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\">%s</text>\n",
                        node.getPositionX() + node.getWidth() / 2,
                        node.getPositionY() + node.getHeight() / 2 + 4,
                        escapeXml(node.getLabel())));
            }
        }

        svg.append("</svg>\n");
        return svg.toString();
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
