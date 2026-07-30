import React, { useMemo, useCallback } from 'react';
import { WorkflowVisualizerNode, WorkflowVisualizerEdge } from '../../../api/workflowEngineApi';
import './WorkflowVisualizer.css';

interface WorkflowVisualizerProps {
  nodes: WorkflowVisualizerNode[];
  edges: WorkflowVisualizerEdge[];
  currentStateId?: string;
  visitedStateIds?: string[];
  width?: number;
  height?: number;
  onNodeClick?: (nodeId: string) => void;
  selectedNodeId?: string;
  showLabels?: boolean;
  compact?: boolean;
}

interface NodePosition {
  x: number;
  y: number;
}

interface EdgePath {
  id: string;
  from: NodePosition;
  to: NodePosition;
  label: string;
  isTraversed: boolean;
  midX: number;
  midY: number;
  angle: number;
}

const DEFAULT_NODE_COLOR = 'var(--sa-n50)';
const INITIAL_NODE_COLOR = '#e3fcef';
const FINAL_NODE_COLOR = 'var(--sa-brand-50)';
const ACTIVE_NODE_COLOR = 'var(--sa-brand-500)';
const CURRENT_NODE_COLOR = '#00b8d9';
const TRAVERSED_EDGE_COLOR = 'var(--sa-brand-500)';
const UNTRAVERSED_EDGE_COLOR = '#b3bac1';
const NODE_WIDTH = 140;
const NODE_HEIGHT = 60;
const COMPACT_NODE_WIDTH = 100;
const COMPACT_NODE_HEIGHT = 45;
const ARROW_SIZE = 8;

const stateTypeStyles = {
  INITIAL: { color: INITIAL_NODE_COLOR, border: 'var(--sa-success-500)', label: 'Start' },
  INTERMEDIATE: { color: DEFAULT_NODE_COLOR, border: 'var(--sa-n200)', label: '' },
  FINAL: { color: FINAL_NODE_COLOR, border: '#4c9aff', label: 'End' },
};

export const WorkflowVisualizer: React.FC<WorkflowVisualizerProps> = ({
  nodes,
  edges,
  currentStateId,
  visitedStateIds = [],
  width = 800,
  height = 400,
  onNodeClick,
  selectedNodeId,
  showLabels = true,
  compact = false,
}) => {
  const nodeWidth = compact ? COMPACT_NODE_WIDTH : NODE_WIDTH;
  const nodeHeight = compact ? COMPACT_NODE_HEIGHT : NODE_HEIGHT;

  // Calculate edge paths with proper routing
  const edgePaths = useMemo<EdgePath[]>(() => {
    const nodeMap = new Map<string, WorkflowVisualizerNode>();
    nodes.forEach(n => nodeMap.set(n.id, n));

    return edges.map(edge => {
      const fromNode = nodeMap.get(edge.fromNodeId);
      const toNode = nodeMap.get(edge.toNodeId);
      if (!fromNode || !toNode) return null;

      // Calculate edge positions (from bottom center to top center)
      const fromX = fromNode.position.x + nodeWidth / 2;
      const fromY = fromNode.position.y + nodeHeight;
      const toX = toNode.position.x + nodeWidth / 2;
      const toY = toNode.position.y;

      // Calculate midpoint
      const midX = (fromX + toX) / 2;
      const midY = (fromY + toY) / 2;

      // Calculate angle for arrow
      const angle = Math.atan2(toY - fromY, toX - fromX) * (180 / Math.PI);

      return {
        id: edge.id,
        from: { x: fromX, y: fromY },
        to: { x: toX, y: toY },
        label: edge.label,
        isTraversed: edge.isTraversed,
        midX,
        midY,
        angle,
      };
    }).filter((p): p is EdgePath => p !== null);
  }, [nodes, edges, nodeWidth, nodeHeight]);

  // Auto-layout if positions are not set
  const layoutNodes = useMemo(() => {
    const positionedNodes = nodes.filter(n => n.position.x > 0 || n.position.y > 0);
    if (positionedNodes.length === nodes.length) {
      return nodes;
    }

    // Simple hierarchical layout
    const initialNodes = nodes.filter(n => n.stateType === 'INITIAL');
    const finalNodes = nodes.filter(n => n.stateType === 'FINAL');
    const intermediateNodes = nodes.filter(n => n.stateType === 'INTERMEDIATE');

    const result: WorkflowVisualizerNode[] = [];
    const nodeSpacingX = nodeWidth + 80;
    const nodeSpacingY = nodeHeight + 60;

    // Initial states on the left
    initialNodes.forEach((node, i) => {
      result.push({
        ...node,
        position: { x: 50, y: 50 + i * (nodeHeight + 40) },
      });
    });

    // Intermediate states in the middle rows
    const cols = Math.ceil(Math.sqrt(intermediateNodes.length));
    intermediateNodes.forEach((node, i) => {
      const col = i % cols;
      const row = Math.floor(i / cols);
      result.push({
        ...node,
        position: { x: 50 + (col + 1) * nodeSpacingX, y: 50 + row * nodeSpacingY },
      });
    });

    // Final states on the right
    finalNodes.forEach((node, i) => {
      result.push({
        ...node,
        position: { x: 50 + (cols + 2) * nodeSpacingX, y: 50 + i * (nodeHeight + 40) },
      });
    });

    return result;
  }, [nodes, nodeWidth, nodeHeight]);

  const handleNodeClick = useCallback((node: WorkflowVisualizerNode) => {
    onNodeClick?.(node.id);
  }, [onNodeClick]);

  const getNodeStyle = (node: WorkflowVisualizerNode) => {
    const style = stateTypeStyles[node.stateType] || stateTypeStyles.INTERMEDIATE;
    let backgroundColor = style.color;
    let borderColor = style.border;
    let borderWidth = 2;

    if (node.id === currentStateId) {
      backgroundColor = CURRENT_NODE_COLOR;
      borderColor = 'var(--sa-brand-500)';
      borderWidth = 3;
    } else if (visitedStateIds.includes(node.stateId)) {
      backgroundColor = ACTIVE_NODE_COLOR;
      borderColor = 'var(--sa-n1000)';
    } else if (node.id === selectedNodeId) {
      borderColor = 'var(--sa-brand-500)';
      borderWidth = 3;
    }

    return {
      backgroundColor,
      borderColor,
      borderWidth: `${borderWidth}px`,
      left: node.position.x,
      top: node.position.y,
      width: nodeWidth,
      minHeight: nodeHeight,
    };
  };

  const getNodeLabel = (node: WorkflowVisualizerNode) => {
    if (node.label) return node.label;
    if (node.stateType === 'INITIAL') return 'Start';
    if (node.stateType === 'FINAL') return 'End';
    return node.stateId;
  };

  const getEdgeColor = (path: EdgePath) => {
    return path.isTraversed ? TRAVERSED_EDGE_COLOR : UNTRAVERSED_EDGE_COLOR;
  };

  return (
    <div className="workflow-visualizer" style={{ width, height }}>
      <svg width={width} height={height} className="workflow-visualizer-svg">
        <defs>
          {/* Arrow marker for traversed edges */}
          <marker
            id="arrow-traversed"
            markerWidth={ARROW_SIZE}
            markerHeight={ARROW_SIZE}
            refX={ARROW_SIZE - 2}
            refY={ARROW_SIZE / 2}
            orient="auto"
          >
            <path d={`M0,0 L0,${ARROW_SIZE} L${ARROW_SIZE},${ARROW_SIZE / 2} z`} fill={TRAVERSED_EDGE_COLOR} />
          </marker>
          {/* Arrow marker for untransformed edges */}
          <marker
            id="arrow-default"
            markerWidth={ARROW_SIZE}
            markerHeight={ARROW_SIZE}
            refX={ARROW_SIZE - 2}
            refY={ARROW_SIZE / 2}
            orient="auto"
          >
            <path d={`M0,0 L0,${ARROW_SIZE} L${ARROW_SIZE},${ARROW_SIZE / 2} z`} fill={UNTRAVERSED_EDGE_COLOR} />
          </marker>
        </defs>

        {/* Render edges */}
        {edgePaths.map(path => (
          <g key={path.id}>
            <line
              x1={path.from.x}
              y1={path.from.y}
              x2={path.to.x}
              y2={path.to.y}
              stroke={getEdgeColor(path)}
              strokeWidth={path.isTraversed ? 3 : 2}
              strokeDasharray={path.isTraversed ? 'none' : '5,5'}
              markerEnd={path.isTraversed ? 'url(#arrow-traversed)' : 'url(#arrow-default)'}
              className="workflow-edge-line"
            />
            {showLabels && path.label && (
              <g transform={`translate(${path.midX}, ${path.midY})`}>
                <rect
                  x={-40}
                  y={-12}
                  width={80}
                  height={24}
                  rx={4}
                  fill="white"
                  stroke={getEdgeColor(path)}
                  strokeWidth={1}
                />
                <text
                  textAnchor="middle"
                  dominantBaseline="middle"
                  fontSize={11}
                  fill="var(--sa-n700)"
                >
                  {path.label}
                </text>
              </g>
            )}
          </g>
        ))}
      </svg>

      {/* Render nodes (as HTML overlay for better styling) */}
      <div className="workflow-visualizer-nodes">
        {layoutNodes.map(node => {
          const style = getNodeStyle(node);
          return (
            <div
              key={node.id}
              className={`workflow-node ${node.stateType.toLowerCase()} ${
                node.id === currentStateId ? 'current' : ''
              } ${visitedStateIds.includes(node.stateId) ? 'visited' : ''} ${
                node.id === selectedNodeId ? 'selected' : ''
              }`}
              style={{
                left: style.left,
                top: style.top,
                width: style.width,
                minHeight: style.minHeight,
                backgroundColor: style.backgroundColor,
                borderColor: style.borderColor,
                borderWidth: style.borderWidth,
              }}
              onClick={() => handleNodeClick(node)}
            >
              <div className="workflow-node-header">
                <span className="workflow-node-type">{stateTypeStyles[node.stateType]?.label}</span>
              </div>
              <div className="workflow-node-body">
                <span className="workflow-node-label">{getNodeLabel(node)}</span>
              </div>
              {node.isCurrent && <div className="workflow-node-indicator current-indicator" />}
              {node.isActive && !node.isCurrent && (
                <div className="workflow-node-indicator active-indicator" />
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

// Compact version for inline display
export const WorkflowVisualizerCompact: React.FC<{
  states: { id: string; name: string; stateType: string }[];
  currentStateId?: string;
}> = ({ states, currentStateId }) => {
  return (
    <div className="workflow-visualizer-compact">
      {states.map((state, index) => (
        <React.Fragment key={state.id}>
          <span
            className={`compact-state ${state.stateType.toLowerCase()} ${
              state.id === currentStateId ? 'current' : ''
            }`}
          >
            {state.name}
          </span>
          {index < states.length - 1 && <span className="compact-arrow">→</span>}
        </React.Fragment>
      ))}
    </div>
  );
};

export default WorkflowVisualizer;