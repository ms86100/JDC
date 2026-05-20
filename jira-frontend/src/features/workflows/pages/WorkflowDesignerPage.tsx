import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  type Node,
  type Edge,
  type NodeDragHandler,
  MarkerType,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { workflowApi } from '../../../api/workflowApi';
import './WorkflowDesignerPage.css';

interface LayoutNodeDto {
  id: string;
  nodeType: string;
  label?: string;
  statusName?: string;
  positionX: number;
  positionY: number;
  statusId?: string;
  width?: number;
  height?: number;
}

interface LayoutEdgeDto {
  id: string;
  transitionId?: string;
  fromNodeId: string;
  toNodeId: string;
}

interface WorkflowLayoutResponse {
  nodes: LayoutNodeDto[];
  edges: LayoutEdgeDto[];
}

interface StatusNodeData {
  label: string;
  nodeType: string;
  statusId?: string;
}

function layoutToFlow(
  layout: WorkflowLayoutResponse,
  transitionNames: Map<string, string>
): { nodes: Node<StatusNodeData>[]; edges: Edge[] } {
  const nodes: Node<StatusNodeData>[] = (layout.nodes ?? []).map((n) => ({
    id: n.id,
    type: 'statusNode',
    position: { x: n.positionX ?? 0, y: n.positionY ?? 0 },
    data: {
      label: n.label || n.statusName || n.nodeType || 'Status',
      nodeType: n.nodeType,
      statusId: n.statusId,
    },
    style: {
      width: n.width ?? 140,
      minHeight: n.height ?? 56,
    },
  }));

  const edges: Edge[] = (layout.edges ?? []).map((e) => ({
    id: e.id,
    source: e.fromNodeId,
    target: e.toNodeId,
    label: e.transitionId ? transitionNames.get(e.transitionId) : undefined,
    markerEnd: { type: MarkerType.ArrowClosed, color: '#0052cc' },
    style: { stroke: '#0052cc', strokeWidth: 2 },
    labelStyle: { fontSize: 11, fill: '#42526e' },
  }));

  return { nodes, edges };
}

function StatusNode({ data }: { data: StatusNodeData }) {
  return (
    <div className="wf-rf-node">
      <span className="wf-node-label">{data.label}</span>
      <span className="wf-node-type">{data.nodeType}</span>
    </div>
  );
}

const nodeTypes = { statusNode: StatusNode };

export default function WorkflowDesignerPage() {
  const { workflowId } = useParams<{ workflowId: string }>();
  const queryClient = useQueryClient();
  const [nodes, setNodes, onNodesChange] = useNodesState<Node<StatusNodeData>>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);
  const [layoutError, setLayoutError] = useState<string | null>(null);

  const { data: workflow } = useQuery({
    queryKey: ['workflow', workflowId],
    queryFn: () => workflowApi.getById(workflowId!).then((r) => r.data),
    enabled: !!workflowId,
  });

  const { data: transitions } = useQuery({
    queryKey: ['workflow-transitions', workflowId],
    queryFn: () => workflowApi.getTransitionsWithDetails(workflowId!).then((r) => r.data),
    enabled: !!workflowId,
  });

  const transitionNameMap = useMemo(() => {
    const m = new Map<string, string>();
    (transitions ?? []).forEach((t) => m.set(t.id, t.name));
    return m;
  }, [transitions]);

  const loadLayout = useCallback(async (): Promise<WorkflowLayoutResponse> => {
    if (!workflowId) {
      throw new Error('Workflow id is required');
    }
    const res = await workflowApi.getLayout(workflowId);
    const data = res.data as WorkflowLayoutResponse;
    if ((data.nodes?.length ?? 0) === 0) {
      const autoRes = await workflowApi.autoLayout(workflowId);
      return autoRes.data as WorkflowLayoutResponse;
    }
    return data;
  }, [workflowId]);

  const {
    data: layout,
    refetch: refetchLayout,
    isLoading: layoutLoading,
    isError: layoutIsError,
    error: layoutQueryError,
  } = useQuery({
    queryKey: ['workflow-layout', workflowId],
    queryFn: loadLayout,
    enabled: !!workflowId,
    retry: 1,
  });

  useEffect(() => {
    if (layoutIsError) {
      const message =
        (layoutQueryError as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (layoutQueryError as Error)?.message ||
        'Failed to load workflow diagram';
      setLayoutError(message);
      return;
    }
    setLayoutError(null);
    if (!layout) return;
    const { nodes: flowNodes, edges: flowEdges } = layoutToFlow(layout, transitionNameMap);
    setNodes(flowNodes);
    setEdges(flowEdges);
  }, [layout, layoutIsError, layoutQueryError, transitionNameMap, setNodes, setEdges]);

  const syncPositionsMutation = useMutation({
    mutationFn: (positions: { nodeId: string; positionX: number; positionY: number }[]) =>
      workflowApi.syncDesignerLayout(workflowId!, positions),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['workflow-layout', workflowId] }),
  });

  const publishMutation = useMutation({
    mutationFn: () => workflowApi.publishWorkflow(workflowId!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['workflow', workflowId] }),
  });

  const onNodeDragStop: NodeDragHandler = useCallback(
    (_event, node) => {
      const positions = nodes.map((n) => ({
        nodeId: n.id,
        positionX: n.id === node.id ? node.position.x : n.position.x,
        positionY: n.id === node.id ? node.position.y : n.position.y,
      }));
      syncPositionsMutation.mutate(positions);
    },
    [nodes, syncPositionsMutation]
  );

  const autoLayout = useCallback(async () => {
    if (!workflowId) return;
    setLayoutError(null);
    try {
      await workflowApi.autoLayout(workflowId);
      await refetchLayout();
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (err as Error)?.message ||
        'Auto layout failed';
      setLayoutError(message);
    }
  }, [workflowId, refetchLayout]);

  const hasDiagram = nodes.length > 0;

  return (
    <div className="wf-designer-page">
      <header className="wf-designer-header">
        <div>
          <Link to="/admin/workflows" className="wf-designer-back">
            ← Workflows
          </Link>
          <h1>{workflow?.name ?? 'Workflow designer'}</h1>
          {workflow?.isDraft && <span className="wf-draft-badge">Draft</span>}
        </div>
        <div className="wf-designer-actions">
          <button type="button" className="dc-btn dc-btn-secondary" onClick={autoLayout}>
            Auto layout
          </button>
          <button
            type="button"
            className="dc-btn dc-btn-secondary"
            disabled={publishMutation.isPending}
            onClick={() => publishMutation.mutate()}
          >
            Publish
          </button>
        </div>
      </header>

      {layoutError && (
        <div className="wf-designer-error" role="alert">
          {layoutError}
          <button type="button" className="dc-btn dc-btn-sm dc-btn-secondary" onClick={autoLayout}>
            Retry auto layout
          </button>
        </div>
      )}

      <div className="wf-designer-canvas wf-designer-reactflow">
        {layoutLoading && !hasDiagram ? (
          <div className="wf-designer-placeholder">Loading workflow diagram…</div>
        ) : !hasDiagram && !layoutLoading ? (
          <div className="wf-designer-placeholder">
            <p>No diagram nodes yet for this workflow.</p>
            <button type="button" className="dc-btn dc-btn-primary" onClick={autoLayout}>
              Generate diagram
            </button>
          </div>
        ) : (
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onNodeDragStop={onNodeDragStop}
            nodeTypes={nodeTypes}
            fitView
            minZoom={0.2}
            maxZoom={2}
            proOptions={{ hideAttribution: true }}
          >
            <Background gap={16} size={1} color="#dfe1e6" />
            <Controls />
            <MiniMap />
          </ReactFlow>
        )}
      </div>
    </div>
  );
}
