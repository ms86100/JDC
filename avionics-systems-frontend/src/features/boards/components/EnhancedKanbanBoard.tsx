import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { boardFieldApi } from '../../../api/fieldApi';
import type { CardCustomFieldRow } from './IssueCard';
import { useNavigate, useSearchParams } from 'react-router-dom';
import boardApi, { BoardColumn, BoardIssue, QuickFilter } from '../../../api/boardApi';
import { issueApi } from '../../../api/issueApi';
import { sprintApi, SprintResponse } from '../../../api/sprintApi';
import { pickDefaultBoard } from '../../../components/workspace/boardLinks';
import KanbanColumn from './KanbanColumn';
import BoardHeader from './BoardHeader';
import QuickFilterBar from './QuickFilterBar';
import SwimlaneView from './SwimlaneView';
import BoardConfigPanel from './BoardConfigPanel';
import CreateIssueModal from '../../issues/components/CreateIssueModal';
import ScrumBoardToolbar from './ScrumBoardToolbar';
import IssueDetailPage from '../../issues/pages/IssueDetailPage';
import BoardEpicsPanel from './BoardEpicsPanel';
import VersionPanel from './VersionPanel';
import CardHoverPreview from './CardHoverPreview';
import VelocityIndicator from './VelocityIndicator';
import ContextMenu from './ContextMenu';
import KeyboardShortcutsModal from './KeyboardShortcutsModal';
import BulkActionsBar from './BulkActionsBar';
import BoardTransitionModal from './BoardTransitionModal';
import { useKeyboardNavigation } from '../hooks/useKeyboardNavigation';
import KanbanWorkspaceToolbar, { type KanbanStatusBanner } from './KanbanWorkspaceToolbar';
import KanbanFilterStrip from './KanbanFilterStrip';
import { useBoardWebSocket } from '../hooks/useBoardWebSocket';
import type { ProjectResponse } from '../../../api/projectApi';
import type { AgileBoard } from '../../../api/boardApi';
import '../styles/kanban-board.css';
import '../styles/avisys-dc-kanban.css';
import {
  KANBAN_DC_COLUMNS,
  issueMatchesColumn,
  normalizeBoardStatus,
  targetStatusForColumn,
} from '../utils/boardColumnUtils';
import { sortIssuesByRank } from '../utils/boardRankUtils';
import {
  executeBoardDrop,
  executeStandaloneDrop,
  findTransitionForColumn,
  type PendingBoardTransition,
} from '../utils/boardDropHandler';
import { applyBoardQuickFilter } from '../utils/boardQuickFilters';
import { fetchBoardIssuesWithFallback } from '../utils/fetchProjectBoardIssues';
import { readDraggedIssue, writeDragPayload } from '../utils/boardDragUtils';
import { useAuth } from '../../auth/context/AuthContext';

interface EnhancedKanbanBoardProps {
  projectId?: string;
  initialBoardId?: string;
  /** Active sprint board: filter issues to sprint + show Complete sprint header */
  scrumActiveSprintMode?: boolean;
  lockActiveSprintId?: string | null;
  /** Open issue in right drawer instead of full-page navigate */
  useIssueDrawer?: boolean;
  /** Avionics Systems classic Kanban columns, quick filters, and swimlanes */
  kanbanClassicMode?: boolean;
  /** Unified /kanban chrome (single header + filter strip) */
  unifiedWorkspace?: boolean;
  workspaceContext?: {
    projects: ProjectResponse[];
    projectId: string;
    onProjectChange: (id: string) => void;
    boards?: AgileBoard[];
    boardId?: string;
    onBoardChange?: (id: string) => void;
    statusBanner?: KanbanStatusBanner | null;
    onRetryBoard?: () => void;
  };
}

type SwimlanField = 'none' | 'epic' | 'assignee' | 'priority' | 'labels' | 'sprint';
type CardLayout = 'FULL' | 'COMPACT' | 'MINI';
type ViewMode = 'board' | 'swimlane';

/** Avionics Systems board quick filter labels (JQL unchanged). */
const DEFAULT_QUICK_FILTERS: QuickFilter[] = [
  { id: 'qf-assigned-me', name: 'Only My Issues', jql: 'assignee = currentUser()' },
  { id: 'qf-reporter-me', name: 'Reported by me', jql: 'reporter = currentUser()' },
  { id: 'qf-recently-updated', name: 'Recently Updated', jql: 'updated >= -1d' },
  { id: 'qf-no-assignee', name: 'Unassigned', jql: 'assignee is empty' },
  { id: 'qf-has-due-date', name: 'Issues with due date', jql: 'duedate is not empty' },
];

const CLASSIC_QUICK_FILTERS: QuickFilter[] = [
  { id: 'qf-assigned-me', name: 'Only My Issues', jql: 'assignee = currentUser()' },
  { id: 'qf-recently-updated', name: 'Recently Updated', jql: 'updated >= -1d' },
];

const DEFAULT_COLUMNS: BoardColumn[] = [
  { id: 'col-backlog', name: 'Backlog', sequence: 0, statusCategory: 'TODO', isDone: false, currentIssues: 0, color: '#6c757d', isCollapsible: true, isHidden: false },
  { id: 'col-todo', name: 'To Do', sequence: 1, statusCategory: 'TODO', isDone: false, currentIssues: 0, color: '#6c757d', isCollapsible: true, isHidden: false, maxIssues: undefined },
  { id: 'col-inprogress', name: 'In Progress', sequence: 2, statusCategory: 'IN_PROGRESS', isDone: false, currentIssues: 0, color: '#0066ff', isCollapsible: true, isHidden: false, maxIssues: 5 },
  { id: 'col-review', name: 'In Review', sequence: 3, statusCategory: 'IN_REVIEW', isDone: false, currentIssues: 0, color: '#ff9200', isCollapsible: true, isHidden: false, maxIssues: 3 },
  { id: 'col-done', name: 'Done', sequence: 4, statusCategory: 'DONE', isDone: true, currentIssues: 0, color: '#28a745', isCollapsible: true, isHidden: false },
];

export default function EnhancedKanbanBoard({
  projectId,
  initialBoardId,
  scrumActiveSprintMode = false,
  lockActiveSprintId = null,
  useIssueDrawer = false,
  kanbanClassicMode = false,
  unifiedWorkspace = false,
  workspaceContext,
}: EnhancedKanbanBoardProps) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const { user } = useAuth();

  // Board state
  const [boardId, setBoardId] = useState<string | null>(initialBoardId || searchParams.get('boardId'));
  const [drawerIssueId, setDrawerIssueId] = useState<string | null>(
    () => searchParams.get('issueId'),
  );

  useEffect(() => {
    const id = searchParams.get('issueId');
    if (id && useIssueDrawer) setDrawerIssueId(id);
  }, [searchParams, useIssueDrawer]);
  const [boardType, setBoardType] = useState<'SCRUM' | 'KANBAN' | 'BADGE'>('SCRUM');
  const [activeSprintId, setActiveSprintId] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<ViewMode>('board');

  // Column configuration
  const [columns, setColumns] = useState<BoardColumn[]>(DEFAULT_COLUMNS);

  // Filter state
  const [quickFilters, setQuickFilters] = useState<QuickFilter[]>(
    kanbanClassicMode ? CLASSIC_QUICK_FILTERS : DEFAULT_QUICK_FILTERS,
  );
  const [activeQuickFilter, setActiveQuickFilter] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  // Swimlane state
  const [swimlaneField, setSwimlaneField] = useState<SwimlanField>('none');
  const [collapsedSwimlanes, setCollapsedSwimlanes] = useState<Set<string>>(new Set());

  // Card layout
  const [cardLayout, setCardLayout] = useState<CardLayout>('FULL');

  // Configuration panel
  const [showConfigPanel, setShowConfigPanel] = useState(false);

  // Drag state
  const [draggedIssue, setDraggedIssue] = useState<BoardIssue | null>(null);
  const [dragOverColumn, setDragOverColumn] = useState<string | null>(null);
  const [dragOverSwimlane, setDragOverSwimlane] = useState<string | null>(null);

  // Create issue modal
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createColumnStatus, setCreateColumnStatus] = useState<string>('');

  // Work vs capacity
  const [showWorkVsCapacity, setShowWorkVsCapacity] = useState(true);
  const [boardCapacity, setBoardCapacity] = useState({ capacity: 40, committed: 0 });

  // Card colors
  const [cardColorField, setCardColorField] = useState<'none' | 'priority' | 'type' | 'labels' | 'epic'>('priority');

  const [showEpicsPanel, setShowEpicsPanel] = useState(kanbanClassicMode);
  const [epicsPanelCollapsed, setEpicsPanelCollapsed] = useState(false);
  const [selectedEpicId, setSelectedEpicId] = useState<string | null>(null);
  const [activeAssigneeFilterId, setActiveAssigneeFilterId] = useState<string | null>(null);

  // Version panel state
  const [showVersionsPanel, setShowVersionsPanel] = useState(false);
  const [versionsPanelCollapsed, setVersionsPanelCollapsed] = useState(true);
  const [selectedVersionId, setSelectedVersionId] = useState<string | null>(null);

  // Hover preview state
  const [hoveredIssue, setHoveredIssue] = useState<BoardIssue | null>(null);
  const [hoverPreviewAnchor, setHoverPreviewAnchor] = useState<React.RefObject<HTMLElement | null>>({ current: null });

  const [pendingTransition, setPendingTransition] = useState<PendingBoardTransition | null>(null);
  const [transitionComment, setTransitionComment] = useState('');
  const [transitionScreenInput, setTransitionScreenInput] = useState<Record<string, unknown>>({});
  const [boardError, setBoardError] = useState<string | null>(null);
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);
  const [dragOverColumnId, setDragOverColumnId] = useState<string | null>(null);

  // Context menu state
  const [contextMenuIssue, setContextMenuIssue] = useState<BoardIssue | null>(null);
  const [contextMenuPosition, setContextMenuPosition] = useState<{ x: number; y: number }>({ x: 0, y: 0 });

  // Keyboard shortcuts modal
  const [showShortcutsModal, setShowShortcutsModal] = useState(false);

  // Bulk selection state
  const [selectedIssues, setSelectedIssues] = useState<Set<string>>(new Set());

  useQuery({
    queryKey: ['project-boards-pick', projectId],
    queryFn: () => boardApi.getBoardsByProject(projectId!),
    enabled: !!projectId && !boardId,
  });

  useEffect(() => {
    if (boardId || !projectId) return;
    boardApi.getBoardsByProject(projectId).then((boards) => {
      const picked = pickDefaultBoard(boards);
      if (picked) setBoardId(picked.id);
    });
  }, [projectId, boardId]);

  const { data: projectSprints = [] } = useQuery({
    queryKey: ['sprints', projectId],
    queryFn: async () => {
      try {
        return await sprintApi.getAll(projectId);
      } catch {
        return [];
      }
    },
    enabled: !!projectId,
    retry: false,
  });

  const activeSprint = useMemo((): SprintResponse | null => {
    if (lockActiveSprintId) {
      return projectSprints.find((s) => s.id === lockActiveSprintId) ?? null;
    }
    return projectSprints.find((s) => s.status === 'ACTIVE') ?? null;
  }, [projectSprints, lockActiveSprintId]);

  // Fetch board data
  const { data: boardConfig } = useQuery({
    queryKey: ['board-config', boardId],
    queryFn: () => boardApi.getBoardConfig(boardId!),
    enabled: !!boardId,
  });

  useEffect(() => {
    if (!boardConfig) return;
    if (boardConfig.swimlane?.field) {
      setSwimlaneField(boardConfig.swimlane.field as SwimlanField);
    }
    if (boardConfig.cardColors?.field) {
      setCardColorField(boardConfig.cardColors.field);
    }
    if (boardConfig.workVsCapacity != null) {
      setShowWorkVsCapacity(boardConfig.workVsCapacity);
    }
    if (boardConfig.swimlane?.collapsedSwimlanes?.length) {
      setCollapsedSwimlanes(new Set(boardConfig.swimlane.collapsedSwimlanes));
    }
  }, [boardConfig]);

  const saveConfigMutation = useMutation({
    mutationFn: () =>
      boardApi.updateBoardConfig(boardId!, {
        swimlane: {
          enabled: swimlaneField !== 'none',
          field: swimlaneField,
          collapsedSwimlanes: Array.from(collapsedSwimlanes),
        },
        workVsCapacity: showWorkVsCapacity,
        cardColors: { enabled: cardColorField !== 'none', field: cardColorField },
        quickFilters,
      } as Parameters<typeof boardApi.updateBoardConfig>[1]),
  });

  const { data: boardData, isLoading: boardLoading, error: boardLoadError, refetch: refetchBoard } = useQuery({
    queryKey: ['board-data', boardId],
    queryFn: async () => {
      if (boardId) {
        return boardApi.getBoardData(boardId);
      }
      return null;
    },
    enabled: !!boardId,
  });

  useEffect(() => {
    if (kanbanClassicMode) {
      setBoardType('KANBAN');
    }
  }, [kanbanClassicMode]);

  useEffect(() => {
    if (boardData?.board) {
      setBoardType(boardData.board.boardType);
      if (kanbanClassicMode) {
        setColumns(
          boardData.columns?.length && boardData.board.boardType === 'KANBAN'
            ? boardData.columns
            : KANBAN_DC_COLUMNS,
        );
      } else if (boardData.columns?.length) {
        setColumns(boardData.columns);
      }
    } else if (kanbanClassicMode && !boardLoading) {
      setColumns(KANBAN_DC_COLUMNS);
      setBoardType('KANBAN');
    }
    if (scrumActiveSprintMode) {
      const sid = lockActiveSprintId ?? boardData?.activeSprint?.id ?? activeSprint?.id ?? null;
      if (sid) setActiveSprintId(sid);
    } else if (boardData?.activeSprint?.id) {
      setActiveSprintId(boardData.activeSprint.id);
    }
  }, [boardData, boardLoading, kanbanClassicMode, scrumActiveSprintMode, lockActiveSprintId, activeSprint?.id]);

  // Fetch issues from board API with issue-service fallback; quick filters applied client-side
  const { data: cardLayoutConfig } = useQuery({
    queryKey: ['board-card-layout', boardId, projectId],
    queryFn: () => boardFieldApi.getCardLayout(boardId!, projectId).then((r) => r.data),
    enabled: !!boardId,
  });

  const cardFieldKeys = useMemo(
    () => cardLayoutConfig?.selectedFields?.map((f) => f.fieldKey) ?? [],
    [cardLayoutConfig],
  );

  const { data: issuesRaw, isLoading: issuesLoading, refetch: refetchIssues } = useQuery({
    queryKey: ['board-issues', boardId, projectId, activeQuickFilter],
    queryFn: async () => {
      let list = await fetchBoardIssuesWithFallback(boardId, projectId);
      if (activeQuickFilter) {
        list = applyBoardQuickFilter(list, activeQuickFilter, user?.userId);
      }
      return list;
    },
    enabled: !!boardId || !!projectId,
  });

  const { data: cardFieldBatch } = useQuery({
    queryKey: ['board-card-values', boardId, cardFieldKeys, issuesRaw?.length],
    queryFn: () =>
      boardFieldApi
        .batchIssueFieldValues({
          issueIds: (issuesRaw ?? []).map((i) => i.id),
          fieldKeys: cardFieldKeys,
          projectId,
        })
        .then((r) => r.data),
    enabled: !!boardId && cardFieldKeys.length > 0 && (issuesRaw?.length ?? 0) > 0,
  });

  const cardCustomFieldsByIssue = useMemo((): Record<string, CardCustomFieldRow[]> => {
    const map: Record<string, CardCustomFieldRow[]> = {};
    const byIssue = cardFieldBatch?.valuesByIssue ?? {};
    for (const [issueId, fields] of Object.entries(byIssue)) {
      map[issueId] = fields.map((f) => ({
        displayName: f.displayName || f.fieldKey,
        value: f.value,
      }));
    }
    return map;
  }, [cardFieldBatch]);

  const issues = useMemo(() => {
    if (!issuesRaw) return [];
    let list = issuesRaw;
    if (scrumActiveSprintMode && activeSprintId) {
      list = list.filter((i) => i.sprintId === activeSprintId);
    }
    if (selectedEpicId === '__none__') {
      list = list.filter((i) => !i.epicId);
    } else if (selectedEpicId) {
      list = list.filter((i) => i.epicId === selectedEpicId);
    }
    if (activeAssigneeFilterId) {
      list = list.filter((i) => i.assigneeId === activeAssigneeFilterId);
    }
    if (searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      list = list.filter(
        (i) =>
          i.title?.toLowerCase().includes(q) ||
          i.issueKey?.toLowerCase().includes(q),
      );
    }
    return list;
  }, [issuesRaw, scrumActiveSprintMode, activeSprintId, selectedEpicId, activeAssigneeFilterId, searchQuery]);

  useBoardWebSocket({
    boardId: boardId ?? '',
    enabled: false,
  });

  const assigneeQuickFilters = useMemo(() => {
    const map = new Map<string, string>();
    for (const issue of issuesRaw ?? []) {
      if (issue.assigneeId) {
        map.set(issue.assigneeId, issue.assigneeName || issue.assigneeId);
      }
    }
    return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
  }, [issuesRaw]);

  const epicFilterOptions = useMemo(() => {
    const map = new Map<string, string>();
    for (const issue of issuesRaw ?? []) {
      if (issue.epicId && issue.epicName) {
        map.set(issue.epicId, issue.epicName);
      }
    }
    return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
  }, [issuesRaw]);

  const handleGroupByChange = useCallback((field: SwimlanField) => {
    setSwimlaneField(field);
    if (field !== 'none') {
      setViewMode('swimlane');
    } else {
      setViewMode('board');
    }
  }, []);

  const moveMutation = useMutation({
    mutationFn: ({
      bid,
      issueId,
      status,
    }: {
      bid: string;
      issueId: string;
      status: string;
    }) => boardApi.moveIssue(bid, issueId, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['board-issues'] });
      queryClient.invalidateQueries({ queryKey: ['board-data'] });
    },
  });

  // Reorder mutation
  const reorderMutation = useMutation({
    mutationFn: ({ boardId, issueId, index, status }: { boardId: string; issueId: string; index: number; status: string }) =>
      boardApi.reorderIssue(boardId, issueId, index, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['board-issues'] });
    },
  });

  // Handle drag start
  const handleDragStart = useCallback((e: React.DragEvent, issue: BoardIssue) => {
    setDraggedIssue(issue);
    writeDragPayload(e, issue);
  }, []);

  // Handle drag over column
  const handleDragOver = useCallback((e: React.DragEvent, columnId: string, swimlaneKey?: string) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    setDragOverColumn(columnId);
    if (swimlaneKey) setDragOverSwimlane(swimlaneKey);
  }, []);

  // Handle drag leave
  const handleDragLeave = useCallback(() => {
    setDragOverColumn(null);
    setDragOverSwimlane(null);
  }, []);

  const applyDrop = useCallback(
    async (
      issue: BoardIssue,
      column: BoardColumn,
      dropIndex: number,
      transitionOverride?: PendingBoardTransition['transition'],
      screenPayload?: { comment?: string; screenInput?: Record<string, unknown> },
    ) => {
      if (!issue?.id) return;
      const pid = projectId || issue.projectId;
      if (!pid) {
        setBoardError('Cannot move issue: project context is missing.');
        return;
      }

      const columnIssues = sortIssuesByRank(
        issues.filter((i) => issueMatchesColumn(i, column)),
      );

      try {
        setBoardError(null);
        if (boardId) {
          await executeBoardDrop(
            boardId,
            pid,
            issue,
            column,
            columnIssues,
            dropIndex,
            boardApi,
            transitionOverride,
            screenPayload,
          );
        } else {
          await executeStandaloneDrop(
            pid,
            issue,
            column,
            transitionOverride,
            screenPayload,
          );
        }
        await queryClient.invalidateQueries({ queryKey: ['board-issues'] });
        await queryClient.invalidateQueries({ queryKey: ['board-data'] });
      } catch (err) {
        setBoardError(err instanceof Error ? err.message : 'Failed to move issue');
      } finally {
        setDraggedIssue(null);
        setDragOverColumn(null);
        setDragOverSwimlane(null);
        setDragOverIndex(null);
        setDragOverColumnId(null);
      }
    },
    [boardId, projectId, issues, queryClient],
  );

  const handleDrop = useCallback(
    async (e: React.DragEvent, column: BoardColumn, targetSwimlane?: string) => {
      e.preventDefault();
      e.stopPropagation();
      const issue = readDraggedIssue(e, draggedIssue);
      console.log('[Kanban] handleDrop called:', column.name, 'issue:', issue?.id);
      if (!issue) {
        console.log('[Kanban] No issue found from drag event');
        return;
      }

      if (
        column.maxIssues &&
        column.maxIssues > 0 &&
        !issueMatchesColumn(issue, column)
      ) {
        const colIssues = issues.filter((i) => issueMatchesColumn(i, column));
        if (colIssues.length >= column.maxIssues) {
          setBoardError(`WIP limit exceeded for ${column.name} (${column.maxIssues})`);
          setDraggedIssue(null);
          return;
        }
      }

      const dropIndex = sortIssuesByRank(
        issues.filter((i) => issueMatchesColumn(i, column)),
      ).length;
      const pid = projectId || issue.projectId;
      const targetStatus = targetStatusForColumn(column);
      // Check if status changed by comparing normalized status strings
      const currentNorm = normalizeBoardStatus(issue.status);
      const targetNorm = normalizeBoardStatus(targetStatus);
      const statusChanged = currentNorm !== targetNorm;

      if (statusChanged && pid) {
        const { transition, targetStatus: resolvedStatus } = await findTransitionForColumn(
          issue,
          pid,
          column,
        );
        if (transition?.hasScreen) {
          setPendingTransition({
            issue,
            column,
            targetStatus: resolvedStatus,
            transition,
            dropIndex,
            swimlaneKey: targetSwimlane,
          });
          return;
        }
        await applyDrop(issue, column, dropIndex, transition);
        return;
      }

      await applyDrop(issue, column, dropIndex);
    },
    [draggedIssue, issues, projectId, applyDrop],
  );

  const handleDropAtIndex = useCallback(
    async (e: React.DragEvent, column: BoardColumn, index: number) => {
      e.preventDefault();
      e.stopPropagation();
      const issue = readDraggedIssue(e, draggedIssue);
      if (!issue) return;

      if (
        column.maxIssues &&
        column.maxIssues > 0 &&
        !issueMatchesColumn(issue, column)
      ) {
        const colIssues = issues.filter((i) => issueMatchesColumn(i, column));
        if (colIssues.length >= column.maxIssues) {
          setBoardError(`WIP limit exceeded for ${column.name} (${column.maxIssues})`);
          setDraggedIssue(null);
          return;
        }
      }

      const pid = projectId || issue.projectId;
      const targetStatus = targetStatusForColumn(column);
      // Check if status changed by comparing normalized status strings
      const currentNorm = normalizeBoardStatus(issue.status);
      const targetNorm = normalizeBoardStatus(targetStatus);
      const statusChanged = currentNorm !== targetNorm;

      if (statusChanged && pid) {
        const { transition, targetStatus: resolvedStatus } = await findTransitionForColumn(
          issue,
          pid,
          column,
        );
        if (transition?.hasScreen) {
          setPendingTransition({
            issue,
            column,
            targetStatus: resolvedStatus,
            transition,
            dropIndex: index,
          });
          return;
        }
        await applyDrop(issue, column, index, transition);
        return;
      }

      await applyDrop(issue, column, index);
    },
    [draggedIssue, issues, projectId, applyDrop],
  );

  // Handle drag end (defer clear so drop handler runs first in all browsers)
  const handleDragEnd = useCallback(() => {
    requestAnimationFrame(() => {
      setDraggedIssue(null);
      setDragOverColumn(null);
      setDragOverSwimlane(null);
      setDragOverIndex(null);
      setDragOverColumnId(null);
    });
  }, []);

  // Get issues by column
  const getIssuesByColumn = useCallback((column: BoardColumn, swimlaneKey?: string) => {
    if (!issues) return [];

    let filteredIssues = issues.filter((issue) => {
      if (!issueMatchesColumn(issue, column)) return false;

      // Apply swimlane filter
      if (swimlaneKey && swimlaneField !== 'none') {
        switch (swimlaneField) {
          case 'epic':
            return issue.epicId === swimlaneKey;
          case 'assignee':
            return issue.assigneeId === swimlaneKey;
          case 'priority':
            return issue.priority === swimlaneKey;
          case 'labels':
            return issue.labels?.includes(swimlaneKey);
          default:
            return true;
        }
      }

      return true;
    });

    // Apply search filter
    if (searchQuery) {
      filteredIssues = filteredIssues.filter(issue =>
        issue.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        issue.issueKey.toLowerCase().includes(searchQuery.toLowerCase())
      );
    }

    return sortIssuesByRank(filteredIssues);
  }, [issues, searchQuery, swimlaneField]);

  // Get unique swimlane values
  const getSwimlaneKeys = useCallback((): { key: string; label: string }[] => {
    if (!issues || swimlaneField === 'none') return [];

    const keys = new Set<string>();

    issues.forEach(issue => {
      switch (swimlaneField) {
        case 'epic':
          if (issue.epicId) keys.add(issue.epicId);
          break;
        case 'assignee':
          if (issue.assigneeId) keys.add(issue.assigneeId);
          break;
        case 'priority':
          if (issue.priority) keys.add(issue.priority);
          break;
        case 'labels':
          issue.labels?.forEach(label => keys.add(label));
          break;
      }
    });

    return Array.from(keys).map(key => {
      let label = key;
      if (swimlaneField === 'assignee') {
        const issue = issues.find(i => i.assigneeId === key);
        label = issue?.assigneeName || key;
      } else if (swimlaneField === 'epic') {
        const issue = issues.find(i => i.epicId === key);
        label = issue?.epicName || key;
      }
      return { key, label };
    });
  }, [issues, swimlaneField]);

  // Handle quick filter selection (client-side via query key)
  const handleQuickFilter = useCallback((filterId: string | null) => {
    setActiveQuickFilter(filterId);
  }, []);

  // Handle swimlane toggle
  const handleSwimlaneToggle = useCallback((swimlaneKey: string) => {
    setCollapsedSwimlanes(prev => {
      const next = new Set(prev);
      if (next.has(swimlaneKey)) {
        next.delete(swimlaneKey);
      } else {
        next.add(swimlaneKey);
      }
      return next;
    });
  }, []);

  // Handle column configuration change
  const handleColumnConfigChange = useCallback(
    (columnId: string, config: Partial<BoardColumn>) => {
      setColumns((prev) =>
        prev.map((col) => (col.id === columnId ? { ...col, ...config } : col)),
      );
      if (boardId) {
        boardApi.updateColumn(boardId, columnId, config).catch(() => {
          /* local state still updated */
        });
      }
    },
    [boardId],
  );

  // Handle card click
  const handleCardClick = useCallback((issue: BoardIssue) => {
    if (useIssueDrawer) {
      setDrawerIssueId(issue.id);
    } else {
      navigate(`/issues/${issue.id}`);
    }
  }, [navigate, useIssueDrawer]);

  // Handle context menu
  const handleContextMenu = useCallback((e: React.MouseEvent, issue: BoardIssue) => {
    e.preventDefault();
    setContextMenuIssue(issue);
    setContextMenuPosition({ x: e.clientX, y: e.clientY });
  }, []);

  // Handle context menu action
  const handleContextMenuAction = useCallback((action: string, issueId?: string) => {
    switch (action) {
      case 'open':
        if (issueId) {
          if (useIssueDrawer) {
            setDrawerIssueId(issueId);
          } else {
            navigate(`/issues/${issueId}`);
          }
        }
        break;
      case 'edit':
        if (issueId) {
          setDrawerIssueId(issueId);
        }
        break;
      case 'assign':
        // Assign to current user
        if (issueId && user?.userId) {
          issueApi.update(issueId, { assigneeId: user.userId }).then(() => {
            queryClient.invalidateQueries({ queryKey: ['board-issues'] });
          });
        }
        break;
      case 'status-todo':
      case 'status-inprogress':
      case 'status-review':
      case 'status-done':
        if (issueId) {
          const statusMap: Record<string, string> = {
            'status-todo': 'To Do',
            'status-inprogress': 'In Progress',
            'status-review': 'In Review',
            'status-done': 'Done',
          };
          const newStatus = statusMap[action];
          if (newStatus) {
            // Find target column for this status
            const targetColumn = columns.find(col => col.name === newStatus);
            if (targetColumn && boardId) {
              executeBoardDrop({
                boardId,
                issueId,
                toColumnStatus: targetColumn.statusCategory || newStatus,
                rank: undefined,
              }).then(() => {
                queryClient.invalidateQueries({ queryKey: ['board-issues'] });
              });
            }
          }
        }
        break;
      case 'priority-highest':
      case 'priority-high':
      case 'priority-medium':
      case 'priority-low':
      case 'priority-lowest':
        if (issueId) {
          const priorityMap: Record<string, string> = {
            'priority-highest': 'Highest',
            'priority-high': 'High',
            'priority-medium': 'Medium',
            'priority-low': 'Low',
            'priority-lowest': 'Lowest',
          };
          const newPriority = priorityMap[action];
          if (newPriority) {
            issueApi.update(issueId, { priority: newPriority }).then(() => {
              queryClient.invalidateQueries({ queryKey: ['board-issues'] });
            });
          }
        }
        break;
      case 'copy':
        if (issueId) {
          navigator.clipboard.writeText(window.location.origin + `/issues/${issueId}`);
        }
        break;
      case 'delete':
        if (issueId && window.confirm('Are you sure you want to delete this issue?')) {
          issueApi.delete(issueId).then(() => {
            queryClient.invalidateQueries({ queryKey: ['board-issues'] });
          });
        }
        break;
    }
  }, [navigate, useIssueDrawer, user, queryClient, columns, boardId]);

  // Handle bulk selection
  const handleBulkAction = useCallback((action: string) => {
    const issueIds = Array.from(selectedIssues);
    switch (action) {
      case 'delete':
        if (window.confirm(`Delete ${issueIds.length} issues?`)) {
          issueIds.forEach(id => issueApi.delete(id).catch(() => {}));
          setSelectedIssues(new Set());
          queryClient.invalidateQueries({ queryKey: ['board-issues'] });
        }
        break;
      case 'archive':
        issueIds.forEach(id => issueApi.update(id, { archived: true }).catch(() => {}));
        setSelectedIssues(new Set());
        queryClient.invalidateQueries({ queryKey: ['board-issues'] });
        break;
    }
  }, [selectedIssues, queryClient]);

  // Handle keyboard navigation
  const handleKeyboardOpen = useCallback((issueId: string) => {
    if (useIssueDrawer) {
      setDrawerIssueId(issueId);
    } else {
      navigate(`/issues/${issueId}`);
    }
  }, [navigate, useIssueDrawer]);

  const keyboardNav = useKeyboardNavigation({
    columns,
    issues: issuesRaw ?? [],
    onIssueSelect: (issueId) => setDrawerIssueId(issueId),
    onIssueOpen: handleKeyboardOpen,
    onDragStart: (issueId) => {
      const issue = issuesRaw?.find(i => i.id === issueId);
      if (issue) {
        setDraggedIssue(issue);
      }
    },
    onDragEnd: () => setDraggedIssue(null),
    onEscape: () => {
      setDrawerIssueId(null);
      setHoveredIssue(null);
      setContextMenuIssue(null);
      setSelectedIssues(new Set());
    },
  });

  // Global keyboard shortcuts
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === '?' && !e.ctrlKey && !e.metaKey) {
        const target = e.target as HTMLElement;
        if (!['INPUT', 'TEXTAREA'].includes(target.tagName)) {
          e.preventDefault();
          setShowShortcutsModal(true);
        }
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Handle create issue
  const handleCreateIssue = useCallback((columnStatus: string) => {
    setCreateColumnStatus(columnStatus);
    setShowCreateModal(true);
  }, []);

  // Get card color based on configuration
  const getCardColor = useCallback((issue: BoardIssue): string | undefined => {
    if (cardColorField === 'none') return undefined;

    switch (cardColorField) {
      case 'priority':
        switch (issue.priority?.toLowerCase()) {
          case 'critical':
          case 'highest':
            return '#dc3545';
          case 'high':
            return '#fd7e14';
          case 'medium':
            return '#ffc107';
          case 'low':
          case 'lowest':
            return '#28a745';
          default:
            return undefined;
        }
      case 'type':
        switch (issue.issueType?.toLowerCase()) {
          case 'bug':
            return '#dc3545';
          case 'story':
            return '#28a745';
          case 'task':
            return '#0066ff';
          case 'epic':
            return '#6f42c1';
          default:
            return undefined;
        }
      case 'epic':
        return issue.epicColor || undefined;
      default:
        return undefined;
    }
  }, [cardColorField]);

  // Calculate WIP status
  const getWipStatus = useCallback((column: BoardColumn, currentCount: number) => {
    if (!column.maxIssues) return { status: 'ok', message: '' };
    if (currentCount >= column.maxIssues) {
      return { status: 'exceeded', message: `WIP limit exceeded (${currentCount}/${column.maxIssues})` };
    }
    if (currentCount >= column.maxIssues * 0.8) {
      return { status: 'warning', message: `Near WIP limit (${currentCount}/${column.maxIssues})` };
    }
    return { status: 'ok', message: '' };
  }, []);

  // Get column issue counts
  const getColumnCounts = useCallback((column: BoardColumn): { total: number; bySwimlane: Map<string, number> } => {
    const columnIssues = issues?.filter((issue) => issueMatchesColumn(issue, column)) || [];

    return { total: columnIssues.length, bySwimlane: new Map() };
  }, [issues]);

  // Toggle swimlane view
  const toggleSwimlaneView = useCallback(() => {
    setViewMode(prev => prev === 'board' ? 'swimlane' : 'board');
  }, []);

  // Loading state (only when a persisted board id is set)
  if (boardId && boardLoading) {
    return (
      <div className="ab-board-loading">
        <div className="ab-loading-spinner">
          <div className="ab-spinner-lg"></div>
          <p>Loading board...</p>
        </div>
      </div>
    );
  }

  if (issuesLoading && !issuesRaw?.length && kanbanClassicMode) {
    return (
      <div className="ab-board-loading">
        <div className="ab-loading-spinner">
          <div className="ab-spinner-lg"></div>
          <p>Loading issues...</p>
        </div>
      </div>
    );
  }

  // Error state
  if (boardId && boardLoadError) {
    return (
      <div className="ab-board-error">
        <div className="ab-error-content">
          <span className="ab-error-icon">⚠️</span>
          <h3>Unable to load board</h3>
          <p>There was an error loading the board data.</p>
          <button onClick={() => refetchBoard()} className="ab-btn ab-btn-primary">
            Try Again
          </button>
        </div>
      </div>
    );
  }

  const swimlanes = getSwimlaneKeys();
  const showSwimlanes = swimlaneField !== 'none' && swimlanes.length > 0;

  const useUnifiedChrome = unifiedWorkspace && !!workspaceContext;

  const boardInner = (
    <div className="sa-board-with-epics">
      {showEpicsPanel && projectId && (
        <BoardEpicsPanel
          projectId={projectId}
          issues={issuesRaw ?? []}
          selectedEpicId={selectedEpicId}
          onSelectEpic={setSelectedEpicId}
          collapsed={epicsPanelCollapsed}
          onToggleCollapsed={() => setEpicsPanelCollapsed((c) => !c)}
        />
      )}
      {showVersionsPanel && projectId && (
        <VersionPanel
          projectId={projectId}
          issues={issuesRaw ?? []}
          selectedVersionId={selectedVersionId}
          onSelectVersion={setSelectedVersionId}
          collapsed={versionsPanelCollapsed}
          onToggleCollapsed={() => setVersionsPanelCollapsed((c) => !c)}
        />
      )}
      <div className="sa-board-main">
    <div
      className={`ab-enhanced-kanban-board${useUnifiedChrome ? ' ab-enhanced-kanban-board--workspace' : ''}`}
      style={{ flex: 1, minHeight: 0 }}
    >
      {useUnifiedChrome && workspaceContext && (
        <>
          <KanbanWorkspaceToolbar
            projects={workspaceContext.projects}
            projectId={workspaceContext.projectId}
            onProjectChange={workspaceContext.onProjectChange}
            boards={workspaceContext.boards}
            boardId={workspaceContext.boardId}
            onBoardChange={workspaceContext.onBoardChange}
            issueCount={issues?.length ?? 0}
            statusBanner={workspaceContext.statusBanner}
            onCreateIssue={() => setShowCreateModal(true)}
            onOpenConfig={() => setShowConfigPanel(true)}
            showEpicsPanel={showEpicsPanel}
            onToggleEpics={() => setShowEpicsPanel((v) => !v)}
            cardLayout={cardLayout}
            onCardLayoutChange={setCardLayout}
            legacyDcVariant
          />
          <KanbanFilterStrip
            quickFilters={quickFilters}
            activeFilter={activeQuickFilter}
            onFilterChange={handleQuickFilter}
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
            swimlaneField={swimlaneField}
            onSwimlaneChange={setSwimlaneField}
            viewMode={viewMode}
            onToggleView={toggleSwimlaneView}
            legacyDcLayout
            assignees={assigneeQuickFilters}
            activeAssigneeId={activeAssigneeFilterId}
            onAssigneeFilterChange={setActiveAssigneeFilterId}
            epicOptions={epicFilterOptions}
            activeEpicId={selectedEpicId}
            onEpicFilterChange={setSelectedEpicId}
            groupBy={swimlaneField}
            onGroupByChange={handleGroupByChange}
          />
        </>
      )}

      {scrumActiveSprintMode && projectId && (
        <ScrumBoardToolbar
          sprint={activeSprint}
          projectId={projectId}
          issueCount={issues?.length ?? 0}
          onSprintComplete={() => setDrawerIssueId(null)}
        />
      )}

      {!useUnifiedChrome && !scrumActiveSprintMode && (
        <BoardHeader
          boardType={kanbanClassicMode ? 'KANBAN' : boardType}
          boardName={
            boardData?.board?.name ||
            (kanbanClassicMode ? 'Kanban board' : boardType === 'SCRUM' ? 'Scrum Board' : 'Kanban Board')
          }
          cardLayout={cardLayout}
          onCardLayoutChange={setCardLayout}
          onOpenConfig={() => setShowConfigPanel(true)}
          viewMode={viewMode}
          onToggleView={toggleSwimlaneView}
          activeSprintId={activeSprintId}
          onSprintChange={setActiveSprintId}
          showVersionsPanel={showVersionsPanel}
          onToggleVersions={() => setShowVersionsPanel((v) => !v)}
        />
      )}

      {!useUnifiedChrome && (
        <>
          <QuickFilterBar
            quickFilters={quickFilters}
            activeFilter={activeQuickFilter}
            onFilterChange={handleQuickFilter}
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
          />
          <div className="ab-swimlane-controls">
            {(boardType === 'SCRUM' || kanbanClassicMode) && (
              <>
                <label className="ab-swimlane-label">Swimlanes:</label>
                <select
                  value={swimlaneField}
                  onChange={(e) => setSwimlaneField(e.target.value as SwimlanField)}
                  className="ab-select ab-select-sm"
                >
                  <option value="none">None</option>
                  <option value="epic">Epic</option>
                  <option value="assignee">Assignee</option>
                  <option value="priority">Priority</option>
                  <option value="labels">Labels</option>
                </select>
              </>
            )}
            <div className="ab-board-toolbar-toggles">
              {projectId && (
                <button
                  type="button"
                  className={`ab-board-toggle-btn${showEpicsPanel ? ' is-active' : ''}`}
                  onClick={() => setShowEpicsPanel((v) => !v)}
                >
                  Epics
                </button>
              )}
            </div>
          </div>
        </>
      )}

      {/* Main Board Area */}
      <div className="ab-board-container">
        {showSwimlanes && viewMode === 'swimlane' ? (
          /* Swimlane View */
          <SwimlaneView
            columns={columns}
            swimlanes={swimlanes}
            issues={issues || []}
            collapsedSwimlanes={collapsedSwimlanes}
            onToggleSwimlane={handleSwimlaneToggle}
            getIssuesByColumn={getIssuesByColumn}
            onDragStart={handleDragStart}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onDragEnd={handleDragEnd}
            onCardClick={handleCardClick}
            getCardColor={getCardColor}
            cardLayout={cardLayout}
            draggedIssue={draggedIssue}
            dragOverColumn={dragOverColumn}
            swimlaneField={swimlaneField}
            cardCustomFieldsByIssue={cardCustomFieldsByIssue}
          />
        ) : (
          /* Standard Board View */
          <div className="ab-board-columns">
            {columns.filter(col => !col.isHidden).map((column) => {
              const columnIssues = getIssuesByColumn(column);
              const wipStatus = getWipStatus(column, columnIssues.length);
              const isOver = dragOverColumn === column.id;
              const wipBlocked =
                !!column.maxIssues &&
                column.maxIssues > 0 &&
                columnIssues.length >= column.maxIssues &&
                draggedIssue != null &&
                !issueMatchesColumn(draggedIssue, column);

              return (
                <KanbanColumn
                  key={column.id}
                  column={column}
                  issues={columnIssues}
                  isOver={isOver}
                  wipStatus={wipStatus}
                  wipBlocked={wipBlocked}
                  onDragOver={(e) => handleDragOver(e, column.id)}
                  onDragLeave={handleDragLeave}
                  onDrop={(e) => handleDrop(e, column)}
                  onDropAtIndex={(e, index) => handleDropAtIndex(e, column, index)}
                  dragOverIndex={dragOverColumnId === column.id ? dragOverIndex : null}
                  onDragOverIndex={(index) => {
                    setDragOverColumnId(column.id);
                    setDragOverIndex(index);
                  }}
                  onDragStart={handleDragStart}
                  onDragEnd={handleDragEnd}
                  onCardClick={handleCardClick}
                  onCardHover={setHoveredIssue}
                  onCardContextMenu={handleContextMenu}
                  onCreateIssue={() => handleCreateIssue(column.statusCategory)}
                  getCardColor={getCardColor}
                  cardLayout={cardLayout}
                  draggedIssue={draggedIssue}
                  showWorkVsCapacity={showWorkVsCapacity}
                  boardCapacity={boardCapacity}
                  releaseLink={
                    column.isDone && projectId
                      ? { label: 'Release…', href: `/projects/${projectId}/releases` }
                      : undefined
                  }
                  olderIssuesLink={
                    column.isDone && projectId
                      ? `/search?jql=${encodeURIComponent(`project = ${projectId} AND status = Done ORDER BY updated DESC`)}`
                      : undefined
                  }
                  cardCustomFieldsByIssue={cardCustomFieldsByIssue}
                  legacyDcLayout={useUnifiedChrome}
                />
              );
            })}
          </div>
        )}
      </div>

      {/* Board Configuration Panel */}
      {showConfigPanel && (
        <BoardConfigPanel
          boardId={boardId}
          projectId={projectId}
          columns={columns}
          boardType={boardType}
          swimlaneField={swimlaneField}
          cardColorField={cardColorField}
          showWorkVsCapacity={showWorkVsCapacity}
          quickFilters={quickFilters}
          onClose={() => {
            setShowConfigPanel(false);
            if (boardId) saveConfigMutation.mutate();
          }}
          onColumnConfigChange={handleColumnConfigChange}
          onBoardTypeChange={setBoardType}
          onSwimlaneFieldChange={setSwimlaneField}
          onCardColorFieldChange={setCardColorField}
          onShowWorkVsCapacityChange={setShowWorkVsCapacity}
          onQuickFiltersChange={setQuickFilters}
        />
      )}

      {/* Create Issue Modal */}
      {showCreateModal && (
        <CreateIssueModal
          projectId={projectId}
          onClose={() => setShowCreateModal(false)}
          onSuccess={() => {
            setShowCreateModal(false);
            refetchIssues();
          }}
        />
      )}

      {pendingTransition && (
        <BoardTransitionModal
          transition={pendingTransition.transition}
          comment={transitionComment}
          screenInput={transitionScreenInput}
          isSubmitting={moveMutation.isPending}
          onCommentChange={setTransitionComment}
          onScreenInputChange={setTransitionScreenInput}
          onCancel={() => {
            setPendingTransition(null);
            setDraggedIssue(null);
          }}
          onConfirm={async () => {
            await applyDrop(
              pendingTransition.issue,
              pendingTransition.column,
              pendingTransition.dropIndex,
              pendingTransition.transition,
              { comment: transitionComment, screenInput: transitionScreenInput },
            );
            setPendingTransition(null);
            setTransitionComment('');
            setTransitionScreenInput({});
          }}
        />
      )}

      {boardError && (
        <div className="ab-board-toast-error" role="alert">
          {boardError}
          <button
            type="button"
            style={{ marginLeft: 12, border: 'none', background: 'transparent', cursor: 'pointer' }}
            onClick={() => setBoardError(null)}
          >
            ×
          </button>
        </div>
      )}

      <style>{`
        .ab-enhanced-kanban-board {
          display: flex;
          flex-direction: column;
          height: 100%;
          background: var(--ab-gray-50);
        }

        .ab-board-loading,
        .ab-board-error {
          display: flex;
          align-items: center;
          justify-content: center;
          height: 400px;
        }

        .ab-loading-spinner {
          text-align: center;
        }

        .ab-spinner-lg {
          width: 48px;
          height: 48px;
          border: 4px solid var(--ab-gray-200);
          border-top-color: var(--ab-primary-500);
          border-radius: 50%;
          animation: spin 1s linear infinite;
          margin: 0 auto var(--ab-spacing-md);
        }

        @keyframes spin {
          to { transform: rotate(360deg); }
        }

        .ab-error-content {
          text-align: center;
          padding: var(--ab-spacing-xl);
        }

        .ab-error-icon {
          font-size: 48px;
          display: block;
          margin-bottom: var(--ab-spacing-md);
        }

        .ab-swimlane-controls {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
          padding: var(--ab-spacing-sm) var(--ab-spacing-md);
          background: var(--ab-white);
          border-bottom: 1px solid var(--ab-gray-200);
        }

        .ab-swimlane-label {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-600);
          font-weight: 500;
        }

        .ab-board-container {
          flex: 1;
          min-height: 0;
          overflow-x: auto;
          overflow-y: hidden;
          padding: 12px 16px 16px;
        }

        .ab-board-columns {
          display: flex;
          align-items: stretch;
          gap: 12px;
          height: 100%;
          min-height: min(100%, 480px);
        }

        .ab-enhanced-kanban-board--workspace .ab-kanban-column {
          flex: 0 0 280px;
          max-width: 300px;
        }

        .ab-select-sm {
          padding: var(--ab-spacing-xs) var(--ab-spacing-sm);
          font-size: var(--ab-font-size-sm);
          border: 1px solid var(--ab-gray-300);
          border-radius: var(--ab-radius-sm);
          background: var(--ab-white);
        }
      `}</style>
    </div>
      </div>
    </div>
  );

  if (useIssueDrawer) {
    return (
      <div className="jdc-board-with-drawer" style={{ flex: 1, minHeight: 0, display: 'flex' }}>
        {boardInner}
        {drawerIssueId && (
          <aside className="jdc-issue-detail-pane" aria-label="Issue detail">
            <div className="jdc-issue-detail-pane-header">
              <span style={{ fontWeight: 600, fontSize: 13 }}>Issue detail</span>
              <button
                type="button"
                className="jdc-issue-detail-pane-close"
                aria-label="Close issue panel"
                onClick={() => setDrawerIssueId(null)}
              >
                ×
              </button>
            </div>
            <IssueDetailPage
              issueIdOverride={drawerIssueId}
              embedded
              onClose={() => setDrawerIssueId(null)}
            />
          </aside>
        )}
        <CardHoverPreview
          issue={hoveredIssue}
          anchorRef={hoverPreviewAnchor}
          onClose={() => setHoveredIssue(null)}
          onOpenIssue={(id) => {
            setHoveredIssue(null);
            setDrawerIssueId(id);
          }}
        />
        <ContextMenu
          issue={contextMenuIssue}
          position={contextMenuPosition}
          onClose={() => setContextMenuIssue(null)}
          onAction={handleContextMenuAction}
        />
        <BulkActionsBar
          selectedIssues={(issuesRaw ?? []).filter(i => selectedIssues.has(i.id))}
          onClearSelection={() => setSelectedIssues(new Set())}
          onAction={handleBulkAction}
        />
        {showShortcutsModal && (
          <KeyboardShortcutsModal onClose={() => setShowShortcutsModal(false)} />
        )}
      </div>
    );
  }

  return (
    <>
      {boardInner}
      <CardHoverPreview
        issue={hoveredIssue}
        anchorRef={hoverPreviewAnchor}
        onClose={() => setHoveredIssue(null)}
        onOpenIssue={(id) => {
          setHoveredIssue(null);
          setDrawerIssueId(id);
        }}
      />
      <ContextMenu
        issue={contextMenuIssue}
        position={contextMenuPosition}
        onClose={() => setContextMenuIssue(null)}
        onAction={handleContextMenuAction}
      />
      <BulkActionsBar
        selectedIssues={(issuesRaw ?? []).filter(i => selectedIssues.has(i.id))}
        onClearSelection={() => setSelectedIssues(new Set())}
        onAction={handleBulkAction}
      />
      {showShortcutsModal && (
        <KeyboardShortcutsModal onClose={() => setShowShortcutsModal(false)} />
      )}
    </>
  );
}