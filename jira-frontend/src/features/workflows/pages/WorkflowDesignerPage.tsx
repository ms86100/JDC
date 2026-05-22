import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  ReactFlow,
  ReactFlowProvider,
  Background,
  Controls,
  MiniMap,
  Handle,
  Position,
  useNodesState,
  useEdgesState,
  useReactFlow,
  type Node,
  type Edge,
  type OnNodeDrag,
  type Connection,
  MarkerType,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { workflowApi, WorkflowTransitionDetail } from '../../../api/workflowApi';
import { TransitionConfigPanel } from '../components/TransitionConfigPanel';
import './WorkflowDesignerPage.css';
import './workflow-management.css';

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
  nodes?: LayoutNodeDto[];
  edges?: LayoutEdgeDto[];
}

interface StatusNodeData extends Record<string, unknown> {
  label: string;
  nodeType: string;
  statusId?: string;
}

interface TransitionEdgeData extends Record<string, unknown> {
  transitionId: string;
  layoutEdgeId?: string;
  synthetic?: boolean;
}

const edgeMarker = { type: MarkerType.ArrowClosed, color: '#0052cc' };
const edgeStyle = { stroke: '#0052cc', strokeWidth: 2 };
const edgeLabelStyle = { fontSize: 11, fill: '#42526e' };

function toFlowEdge(
  id: string,
  source: string,
  target: string,
  label: string | undefined,
  data: TransitionEdgeData
): Edge {
  return {
    id,
    source,
    target,
    label,
    markerEnd: edgeMarker,
    style: edgeStyle,
    labelStyle: edgeLabelStyle,
    data,
  };
}

function unwrapLayout(payload: unknown): WorkflowLayoutResponse {
  const body = payload as WorkflowLayoutResponse & { data?: WorkflowLayoutResponse };
  if (Array.isArray(body?.nodes)) {
    return { nodes: body.nodes ?? [], edges: body.edges ?? [] };
  }
  if (body?.data && Array.isArray(body.data.nodes)) {
    return { nodes: body.data.nodes ?? [], edges: body.data.edges ?? [] };
  }
  return { nodes: [], edges: [] };
}

function extractApiErrorMessage(err: unknown, fallback: string): string {
  const axiosErr = err as {
    response?: { data?: { message?: string }; status?: number };
    message?: string;
  };
  const apiMessage = axiosErr?.response?.data?.message;
  if (apiMessage) return apiMessage;
  if (axiosErr?.response?.status) {
    return `${fallback} (HTTP ${axiosErr.response.status})`;
  }
  return (err as Error)?.message || fallback;
}

/** Builds diagram nodes/edges from layout + transitions (arrows stay in sync with the table). */
function buildDiagramFromLayout(
  layout: WorkflowLayoutResponse,
  transitions: WorkflowTransitionDetail[]
): { nodes: Node<StatusNodeData>[]; edges: Edge[] } {
  const layoutNodes = layout.nodes ?? [];
  const nodes: Node<StatusNodeData>[] = layoutNodes.map((n) => ({
    id: n.id,
    type: 'statusNode',
    position: { x: n.positionX ?? 0, y: n.positionY ?? 0 },
    data: {
      label: n.label || n.statusName || n.nodeType || 'Status',
      nodeType: n.nodeType,
      statusId: n.statusId,
    },
    style: { width: n.width ?? 140, minHeight: n.height ?? 56 },
  }));

  const nodeIdByStatusId = new Map<string, string>();
  for (const n of layoutNodes) {
    if (n.statusId) nodeIdByStatusId.set(n.statusId, n.id);
  }

  const edges: Edge[] = [];
  const linkedTransitionIds = new Set<string>();

  for (const e of layout.edges ?? []) {
    if (!e.fromNodeId || !e.toNodeId || !e.transitionId) continue;
    const transitionId = e.transitionId;
    const label = transitions.find((t) => t.id === transitionId)?.name;
    linkedTransitionIds.add(transitionId);
    edges.push(
      toFlowEdge(e.id, e.fromNodeId, e.toNodeId, label, {
        transitionId,
        layoutEdgeId: e.id,
      })
    );
  }

  for (const t of transitions) {
    if (linkedTransitionIds.has(t.id)) continue;
    const fromNodeId = nodeIdByStatusId.get(t.fromStatusId);
    const toNodeId = nodeIdByStatusId.get(t.toStatusId);
    if (!fromNodeId || !toNodeId) continue;
    edges.push(
      toFlowEdge(`transition-${t.id}`, fromNodeId, toNodeId, t.name, {
        transitionId: t.id,
        synthetic: true,
      })
    );
  }

  return { nodes, edges };
}

function StatusNode({ data }: { data: StatusNodeData }) {
  const catClass = data.nodeType?.toLowerCase().includes('done')
    ? 'wf-cat-done'
    : data.nodeType?.toLowerCase().includes('progress')
      ? 'wf-cat-ip'
      : 'wf-cat-todo';
  return (
    <div className={`wf-rf-node ${catClass}`}>
      <Handle type="target" position={Position.Top} className="wf-rf-handle" />
      <span className="wf-node-label">{data.label}</span>
      <span className="wf-node-type">{data.nodeType}</span>
      <Handle type="source" position={Position.Bottom} className="wf-rf-handle" />
    </div>
  );
}

const nodeTypes = { statusNode: StatusNode };

function FitViewOnLoad({ ready }: { ready: boolean }) {
  const { fitView } = useReactFlow();
  useEffect(() => {
    if (ready) {
      const t = window.setTimeout(() => fitView({ padding: 0.25, duration: 200 }), 50);
      return () => window.clearTimeout(t);
    }
  }, [ready, fitView]);
  return null;
}

function WorkflowDesignerCanvas({
  workflowId,
  onSelectTransition,
  onClearSelection,
}: {
  workflowId: string;
  onSelectTransition: (t: WorkflowTransitionDetail) => void;
  onClearSelection: () => void;
}) {
  const queryClient = useQueryClient();
  const [nodes, setNodes, onNodesChange] = useNodesState<Node<StatusNodeData>>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);
  const [layoutError, setLayoutError] = useState<string | null>(null);
  const [selectedEdgeId, setSelectedEdgeId] = useState<string | null>(null);

  const { data: transitions = [] } = useQuery({
    queryKey: ['workflow-transitions', workflowId],
    queryFn: () => workflowApi.getTransitionsWithDetails(workflowId).then((r) => r.data),
    enabled: !!workflowId,
    refetchInterval: 4000,
    refetchOnWindowFocus: true,
  });

  const loadLayout = useCallback(async (): Promise<WorkflowLayoutResponse> => {
    try {
      const res = await workflowApi.getLayout(workflowId);
      const data = unwrapLayout(res.data);
      if ((data.nodes?.length ?? 0) === 0) {
        const autoRes = await workflowApi.autoLayout(workflowId);
        return unwrapLayout(autoRes.data);
      }
      return data;
    } catch (getErr: unknown) {
      // If layout load fails (e.g. stale data), try generating from workflow statuses
      try {
        const autoRes = await workflowApi.autoLayout(workflowId);
        return unwrapLayout(autoRes.data);
      } catch {
        throw getErr;
      }
    }
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

  const applyDiagram = useCallback(
    (layoutData: WorkflowLayoutResponse, transitionList: WorkflowTransitionDetail[]) => {
      const { nodes: flowNodes, edges: flowEdges } = buildDiagramFromLayout(layoutData, transitionList);
      setNodes(flowNodes);
      setEdges(flowEdges);
    },
    [setNodes, setEdges]
  );

  useEffect(() => {
    if (layoutIsError) {
      setLayoutError(extractApiErrorMessage(layoutQueryError, 'Failed to load workflow diagram'));
      return;
    }
    setLayoutError(null);
    if (!layout) return;
    applyDiagram(layout, transitions);
  }, [layout, layoutIsError, layoutQueryError, transitions, applyDiagram]);

  const syncPositionsMutation = useMutation({
    mutationFn: (positions: { nodeId: string; positionX: number; positionY: number }[]) =>
      workflowApi.syncDesignerLayout(workflowId, positions),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['workflow-layout', workflowId] }),
  });

  const onNodeDragStop: OnNodeDrag = useCallback(
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
    setLayoutError(null);
    try {
      await workflowApi.autoLayout(workflowId);
      await refetchLayout();
    } catch (err: unknown) {
      setLayoutError(extractApiErrorMessage(err, 'Auto layout failed'));
    }
  }, [workflowId, refetchLayout]);

  const invalidateWorkflowCaches = useCallback(async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['workflow-transitions', workflowId] }),
      queryClient.invalidateQueries({ queryKey: ['workflow-detail', workflowId] }),
      queryClient.invalidateQueries({ queryKey: ['workflow-layout', workflowId] }),
    ]);
  }, [queryClient, workflowId]);

  const createTransitionMutation = useMutation({
    mutationFn: (data: { name: string; fromStatusId: string; toStatusId: string }) =>
      workflowApi.createTransition({ workflowId, ...data }),
    onSuccess: async (res) => {
      const created = res.data as WorkflowTransitionDetail & { id: string };
      if (layout && created?.id) {
        applyDiagram(layout, [...transitions, { ...created, workflowId }]);
      }
      await invalidateWorkflowCaches();
      void refetchLayout();
    },
  });

  const deleteTransitionMutation = useMutation({
    mutationFn: (transitionId: string) => workflowApi.deleteTransition(transitionId),
    onSuccess: async (_res, transitionId) => {
      setSelectedEdgeId(null);
      onClearSelection();
      if (layout) {
        applyDiagram(
          layout,
          transitions.filter((t) => t.id !== transitionId)
        );
      }
      await invalidateWorkflowCaches();
      void refetchLayout();
    },
    onError: (err: unknown) => {
      setLayoutError(extractApiErrorMessage(err, 'Failed to delete transition'));
    },
  });

  const onConnect = useCallback(
    (connection: Connection) => {
      const sourceNode = nodes.find((n) => n.id === connection.source);
      const targetNode = nodes.find((n) => n.id === connection.target);
      const fromStatusId = sourceNode?.data.statusId;
      const toStatusId = targetNode?.data.statusId;
      if (!fromStatusId || !toStatusId) {
        setLayoutError('Connect two status nodes to create a transition.');
        return;
      }
      const defaultName = `${sourceNode?.data.label ?? 'Status'} → ${targetNode?.data.label ?? 'Status'}`;
      const name = window.prompt('Transition name', defaultName);
      if (!name?.trim()) return;
      createTransitionMutation.mutate({ name: name.trim(), fromStatusId, toStatusId });
    },
    [nodes, createTransitionMutation]
  );

  const getTransitionIdFromEdge = useCallback((edge: Edge): string | undefined => {
    const data = edge.data as TransitionEdgeData | undefined;
    if (data?.transitionId) return data.transitionId;
    if (edge.id.startsWith('transition-')) return edge.id.slice('transition-'.length);
    return undefined;
  }, []);

  const onEdgeClick = useCallback(
    (_event: React.MouseEvent, edge: Edge) => {
      setSelectedEdgeId(edge.id);
      const transitionId = getTransitionIdFromEdge(edge);
      if (!transitionId) return;
      const t = transitions.find((tr) => tr.id === transitionId);
      if (t) onSelectTransition(t);
    },
    [transitions, onSelectTransition, getTransitionIdFromEdge]
  );

  const onPaneClick = useCallback(() => {
    setSelectedEdgeId(null);
    onClearSelection();
  }, [onClearSelection]);

  const onEdgesDelete = useCallback(
    (deleted: Edge[]) => {
      for (const edge of deleted) {
        const transitionId = getTransitionIdFromEdge(edge);
        if (transitionId) {
          deleteTransitionMutation.mutate(transitionId);
          return;
        }
      }
    },
    [getTransitionIdFromEdge, deleteTransitionMutation]
  );

  const selectedEdge = edges.find((e) => e.id === selectedEdgeId);
  const selectedEdgeTransitionId = selectedEdge ? getTransitionIdFromEdge(selectedEdge) : undefined;

  const hasDiagram = nodes.length > 0;

  return (
    <>
      {layoutError && (
        <div className="wf-designer-error" role="alert">
          {layoutError}
          <button type="button" className="ab-btn ab-btn-sm ab-btn-secondary" onClick={autoLayout}>
            Retry auto layout
          </button>
        </div>
      )}
      <div className="wf-designer-canvas wf-designer-reactflow">
        {layoutLoading && !hasDiagram && !layoutError ? (
          <div className="wf-designer-placeholder">Loading workflow diagram…</div>
        ) : !hasDiagram && !layoutLoading && !layoutError ? (
          <div className="wf-designer-placeholder">
            <p>No diagram nodes yet. Add statuses to this workflow, then generate the layout.</p>
            <button type="button" className="ab-btn ab-btn-primary" onClick={autoLayout}>
              Generate diagram
            </button>
          </div>
        ) : hasDiagram ? (
          <>
          {selectedEdge && selectedEdgeTransitionId && (
            <div className="wf-edge-toolbar">
              <span className="wf-edge-toolbar-label">
                Transition: <strong>{String(selectedEdge.label ?? 'Unnamed')}</strong>
              </span>
              <button
                type="button"
                className="ab-btn ab-btn-sm ab-btn-secondary"
                disabled={deleteTransitionMutation.isPending}
                onClick={() => deleteTransitionMutation.mutate(selectedEdgeTransitionId)}
              >
                Delete arrow
              </button>
              <span className="wf-muted wf-edge-toolbar-hint">or press Delete</span>
            </div>
          )}
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onNodeDragStop={onNodeDragStop}
            onConnect={onConnect}
            onEdgeClick={onEdgeClick}
            onPaneClick={onPaneClick}
            onEdgesDelete={onEdgesDelete}
            nodesConnectable
            elementsSelectable
            edgesFocusable
            deleteKeyCode={['Backspace', 'Delete']}
            nodeTypes={nodeTypes}
            fitView
            minZoom={0.15}
            maxZoom={2}
            proOptions={{ hideAttribution: true }}
          >
            <FitViewOnLoad ready={hasDiagram} />
            <Background gap={16} size={1} color="#dfe1e6" />
            <Controls />
            <MiniMap />
          </ReactFlow>
          </>
        ) : null}
      </div>
    </>
  );
}

export default function WorkflowDesignerPage() {
  const { workflowId } = useParams<{ workflowId: string }>();
  const queryClient = useQueryClient();
  const [selectedTransition, setSelectedTransition] = useState<WorkflowTransitionDetail | null>(null);

  const { data: workflow } = useQuery({
    queryKey: ['workflow', workflowId],
    queryFn: () => workflowApi.getById(workflowId!).then((r) => r.data),
    enabled: !!workflowId,
  });

  const autoLayoutMutation = useMutation({
    mutationFn: () => workflowApi.autoLayout(workflowId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['workflow-layout', workflowId] });
      queryClient.invalidateQueries({ queryKey: ['workflow-transitions', workflowId] });
    },
  });

  const publishMutation = useMutation({
    mutationFn: () => workflowApi.publishWorkflow(workflowId!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['workflow', workflowId] }),
  });

  const draftMutation = useMutation({
    mutationFn: () => workflowApi.createWorkflowDraft(workflowId!),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['workflow', workflowId] }),
  });

  if (!workflowId) {
    return <div className="wf-designer-page">Missing workflow id.</div>;
  }

  return (
    <div className="wf-designer-page">
      <header className="wf-designer-header">
        <div>
          <Link to={`/workflows/${workflowId}`} className="wf-designer-back">
            ← Workflow settings
          </Link>
          <h1>{workflow?.name ?? 'Workflow designer'}</h1>
          {workflow?.isDraft && <span className="wf-draft-badge">Draft</span>}
          {!workflow?.isActive && <span className="wf-draft-badge wf-inactive-badge">Inactive</span>}
        </div>
        <div className="wf-designer-actions">
          <button type="button" className="ab-btn ab-btn-secondary" onClick={() => draftMutation.mutate()}>
            Save draft
          </button>
          <button
            type="button"
            className="ab-btn ab-btn-secondary"
            disabled={autoLayoutMutation.isPending}
            onClick={() => autoLayoutMutation.mutate()}
          >
            Auto layout
          </button>
          <button
            type="button"
            className="ab-btn ab-btn-secondary"
            onClick={() => workflowApi.lockLayout(workflowId!)}
          >
            Lock layout
          </button>
          <button
            type="button"
            className="ab-btn ab-btn-secondary"
            onClick={() => workflowApi.unlockLayout(workflowId!)}
          >
            Unlock layout
          </button>
          <button
            type="button"
            className="ab-btn ab-btn-primary"
            disabled={publishMutation.isPending}
            onClick={() => publishMutation.mutate()}
          >
            Publish
          </button>
        </div>
      </header>
      <p className="wf-muted wf-designer-hint">
        Drag statuses to reposition. Drag between handles to create a transition. Click an arrow to configure or delete it (Delete key). Changes sync with the Transitions table.
      </p>
      <div className={`wf-designer-layout ${selectedTransition ? 'wf-designer-layout--split' : ''}`}>
        <ReactFlowProvider>
          <WorkflowDesignerCanvas
            workflowId={workflowId}
            onSelectTransition={setSelectedTransition}
            onClearSelection={() => setSelectedTransition(null)}
          />
        </ReactFlowProvider>
        {selectedTransition && (
          <TransitionConfigPanel
            transition={selectedTransition}
            onClose={() => setSelectedTransition(null)}
          />
        )}
      </div>
    </div>
  );
}
