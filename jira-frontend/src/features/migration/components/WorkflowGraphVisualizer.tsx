import React, { useMemo } from 'react';
import {
  ReactFlow,
  ReactFlowProvider,
  Background,
  Controls,
  MiniMap,
  Handle,
  Position,
  MarkerType,
  type Node,
  type Edge,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import MigrationPanel from './MigrationPanel';
import './WorkflowGraphVisualizer.css';

export interface WorkflowGraphNode {
  stepId: string;
  stepName: string;
  statusName?: string;
  terminal?: boolean;
}

export interface WorkflowGraphEdge {
  actionId?: string;
  actionName?: string;
  fromStepId?: string | null;
  toStepId?: string;
  global?: boolean;
  initial?: boolean;
}

export interface WorkflowGraphData {
  workflowName?: string;
  nodes?: WorkflowGraphNode[] | Record<string, WorkflowGraphNode>;
  edges?: WorkflowGraphEdge[];
  globalEdges?: WorkflowGraphEdge[];
  initialEdges?: WorkflowGraphEdge[];
}

type StepNodeData = Record<string, unknown> & {
  label: string;
  stepId: string;
  terminal?: boolean;
};

function normalizeNodes(graph: WorkflowGraphData): WorkflowGraphNode[] {
  if (!graph.nodes) return [];
  if (Array.isArray(graph.nodes)) return graph.nodes;
  return Object.values(graph.nodes);
}

function StepNode({ data }: { data: StepNodeData }) {
  return (
    <div className={`wf-migration-node ${data.terminal ? 'wf-migration-node--terminal' : ''}`}>
      <Handle type="target" position={Position.Top} />
      <div className="wf-migration-node__id">{data.stepId}</div>
      <div className="wf-migration-node__label">{data.label}</div>
      <Handle type="source" position={Position.Bottom} />
    </div>
  );
}

const nodeTypes = { stepNode: StepNode };

const COLS = 4;
const X_GAP = 220;
const Y_GAP = 100;

function buildFlowElements(
  nodes: WorkflowGraphNode[],
  rawEdges: WorkflowGraphEdge[]
): { nodes: Node<StepNodeData>[]; edges: Edge[] } {
  const flowNodes: Node<StepNodeData>[] = nodes.map((n, i) => ({
    id: n.stepId,
    type: 'stepNode',
    position: { x: (i % COLS) * X_GAP, y: Math.floor(i / COLS) * Y_GAP },
    data: {
      stepId: n.stepId,
      label: n.stepName || n.statusName || n.stepId,
      terminal: n.terminal,
    },
  }));

  const nodeIds = new Set(nodes.map((n) => n.stepId));
  const initNodeId = nodes[0]?.stepId;

  const flowEdges: Edge[] = [];
  rawEdges.forEach((e, i) => {
    const target = e.toStepId;
    if (!target || !nodeIds.has(target)) return;

    let source = e.fromStepId ?? null;
    if (!source || !nodeIds.has(source)) {
      if (e.initial && initNodeId) source = initNodeId;
      else return;
    }
    if (source === target) return;

    flowEdges.push({
      id: `e-${e.actionId ?? i}-${source}-${target}`,
      source,
      target,
      label: e.actionName || (e.global ? 'global' : undefined),
      markerEnd: { type: MarkerType.ArrowClosed, color: 'var(--sa-brand-600)' },
      style: {
        stroke: e.global ? 'var(--sa-n500)' : 'var(--sa-brand-600)',
        strokeWidth: 1.5,
      },
      labelStyle: { fontSize: 10, fill: 'var(--sa-n700)' },
    });
  });

  return { nodes: flowNodes, edges: flowEdges };
}

function WorkflowGraphCanvas({ graph }: { graph: WorkflowGraphData }) {
  const nodes = normalizeNodes(graph);
  const rawEdges = [
    ...(graph.edges || []),
    ...(graph.globalEdges || []),
    ...(graph.initialEdges || []),
  ];

  const { nodes: flowNodes, edges: flowEdges } = useMemo(
    () => buildFlowElements(nodes, rawEdges),
    [nodes, rawEdges]
  );

  if (!nodes.length) {
    return (
      <p style={{ fontSize: 'var(--sa-fs-sm)', color: 'var(--sa-n600)', margin: 0 }}>
        No workflow steps in graph data.
      </p>
    );
  }

  return (
    <div className="workflow-graph-viz__canvas" data-testid="workflow-graph-canvas">
      <ReactFlow
        nodes={flowNodes}
        edges={flowEdges}
        nodeTypes={nodeTypes}
        fitView
        fitViewOptions={{ padding: 0.2 }}
        nodesDraggable
        nodesConnectable={false}
        elementsSelectable
        proOptions={{ hideAttribution: true }}
      >
        <Background gap={16} color="var(--sa-n200)" />
        <Controls />
        <MiniMap
          nodeColor={() => 'var(--sa-brand-600)'}
          maskColor="rgba(0,0,0,0.08)"
          style={{ border: '1px solid var(--sa-n200)' }}
        />
      </ReactFlow>
    </div>
  );
}

export default function WorkflowGraphVisualizer({ graph }: { graph: WorkflowGraphData | null }) {
  if (!graph) {
    return (
      <p style={{ fontSize: 'var(--sa-fs-sm)', color: 'var(--sa-n600)' }}>
        No graph data. Run validation first.
      </p>
    );
  }

  const nodes = normalizeNodes(graph);
  const edgeCount = [
    ...(graph.edges || []),
    ...(graph.globalEdges || []),
    ...(graph.initialEdges || []),
  ].length;

  return (
    <div data-testid="workflow-graph-visualizer">
      <MigrationPanel
        title={`Workflow: ${graph.workflowName || '—'}`}
        subtitle={`${nodes.length} steps · ${edgeCount} transitions — drag nodes, use minimap`}
        noPadding
      >
        <div className="workflow-graph-viz">
          <ReactFlowProvider>
            <WorkflowGraphCanvas graph={graph} />
          </ReactFlowProvider>
        </div>
      </MigrationPanel>
    </div>
  );
}
