import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
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
import BoardTransitionModal from './BoardTransitionModal';
import KanbanWorkspaceToolbar, { type KanbanStatusBanner } from './KanbanWorkspaceToolbar';
import KanbanFilterStrip from './KanbanFilterStrip';
import type { ProjectResponse } from '../../../api/projectApi';
import type { AgileBoard } from '../../../api/boardApi';
import '../styles/kanban-board.css';
import {
  KANBAN_DC_COLUMNS,
  issueMatchesColumn,
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
import { useAuth } from '../../auth/context/AuthContext';

interface EnhancedKanbanBoardProps {
  projectId?: string;
  initialBoardId?: string;
  /** Active sprint board: filter issues to sprint + show Complete sprint header */
  scrumActiveSprintMode?: boolean;
  lockActiveSprintId?: string | null;
  /** Open issue in right drawer instead of full-page navigate */
  useIssueDrawer?: boolean;
  /** Jira DC classic Kanban columns, quick filters, and swimlanes */
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

/** Jira DC board quick filter labels (JQL unchanged). */
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

  const [showEpicsPanel, setShowEpicsPanel] = useState(
    kanbanClassicMode && !unifiedWorkspace,
  );
  const [epicsPanelCollapsed, setEpicsPanelCollapsed] = useState(unifiedWorkspace);
  const [selectedEpicId, setSelectedEpicId] = useState<string | null>(null);
  const [pendingTransition, setPendingTransition] = useState<PendingBoardTransition | null>(null);
  const [transitionComment, setTransitionComment] = useState('');
  const [transitionScreenInput, setTransitionScreenInput] = useState<Record<string, unknown>>({});
  const [boardError, setBoardError] = useState<string | null>(null);
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);
  const [dragOverColumnId, setDragOverColumnId] = useState<string | null>(null);

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
    queryFn: () => sprintApi.getAll(projectId),
    enabled: !!projectId,
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
    if (searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      list = list.filter(
        (i) =>
          i.title?.toLowerCase().includes(q) ||
          i.issueKey?.toLowerCase().includes(q),
      );
    }
    return list;
  }, [issuesRaw, scrumActiveSprintMode, activeSprintId, selectedEpicId, searchQuery]);

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
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData('application/json', JSON.stringify(issue));
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
      column: BoardColumn,
      dropIndex: number,
      transitionOverride?: PendingBoardTransition['transition'],
      screenPayload?: { comment?: string; screenInput?: Record<string, unknown> },
    ) => {
      if (!draggedIssue) return;
      const pid = projectId || draggedIssue.projectId;
      if (!pid) return;

      const columnIssues = sortIssuesByRank(
        issues.filter((i) => issueMatchesColumn(i, column)),
      );

      try {
        setBoardError(null);
        if (boardId) {
          await executeBoardDrop(
            boardId,
            pid,
            draggedIssue,
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
            draggedIssue,
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
    [draggedIssue, boardId, projectId, issues, queryClient],
  );

  const handleDrop = useCallback(
    async (e: React.DragEvent, column: BoardColumn, targetSwimlane?: string) => {
      e.preventDefault();
      if (!draggedIssue) return;

      if (
        column.maxIssues &&
        column.maxIssues > 0 &&
        !issueMatchesColumn(draggedIssue, column)
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
      const pid = projectId || draggedIssue.projectId;
      const targetStatus = targetStatusForColumn(column);
      const statusChanged =
        (draggedIssue.status ?? '').toLowerCase() !== targetStatus.toLowerCase();

      if (statusChanged && pid) {
        const { transition } = await findTransitionForColumn(draggedIssue, pid, column);
        if (transition?.hasScreen) {
          setPendingTransition({
            issue: draggedIssue,
            column,
            targetStatus,
            transition,
            dropIndex,
            swimlaneKey: targetSwimlane,
          });
          return;
        }
        await applyDrop(column, dropIndex, transition);
        return;
      }

      await applyDrop(column, dropIndex);
    },
    [draggedIssue, issues, projectId, applyDrop],
  );

  const handleDropAtIndex = useCallback(
    async (e: React.DragEvent, column: BoardColumn, index: number) => {
      e.preventDefault();
      if (!draggedIssue) return;

      if (
        column.maxIssues &&
        column.maxIssues > 0 &&
        !issueMatchesColumn(draggedIssue, column)
      ) {
        const colIssues = issues.filter((i) => issueMatchesColumn(i, column));
        if (colIssues.length >= column.maxIssues) {
          setBoardError(`WIP limit exceeded for ${column.name} (${column.maxIssues})`);
          setDraggedIssue(null);
          return;
        }
      }

      const pid = projectId || draggedIssue.projectId;
      const targetStatus = targetStatusForColumn(column);
      const statusChanged =
        (draggedIssue.status ?? '').toLowerCase() !== targetStatus.toLowerCase();

      if (statusChanged && pid) {
        const { transition } = await findTransitionForColumn(draggedIssue, pid, column);
        if (transition?.hasScreen) {
          setPendingTransition({
            issue: draggedIssue,
            column,
            targetStatus,
            transition,
            dropIndex: index,
          });
          return;
        }
        await applyDrop(column, index, transition);
        return;
      }

      await applyDrop(column, index);
    },
    [draggedIssue, issues, projectId, applyDrop],
  );

  // Handle drag end
  const handleDragEnd = useCallback(() => {
    setDraggedIssue(null);
    setDragOverColumn(null);
    setDragOverSwimlane(null);
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
                />
              );
            })}
          </div>
        )}
      </div>

      {/* Board Configuration Panel */}
      {showConfigPanel && (
        <BoardConfigPanel
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
      </div>
    );
  }

  return boardInner;
}