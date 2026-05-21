import React, { useState, useEffect, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useSearchParams } from 'react-router-dom';
import boardApi, { BoardColumn, BoardIssue, AgileBoard, QuickFilter, SwimlaneConfig, BoardConfig, BoardDataResponse } from '../../../api/boardApi';
import { issueApi, IssueResponse } from '../../../api/issueApi';
import KanbanColumn from './KanbanColumn';
import BoardHeader from './BoardHeader';
import QuickFilterBar from './QuickFilterBar';
import SwimlaneView from './SwimlaneView';
import BoardConfigPanel from './BoardConfigPanel';
import IssueCard from './IssueCard';
import CreateIssueModal from '../../issues/components/CreateIssueModal';
import SprintSelector from './SprintSelector';

interface EnhancedKanbanBoardProps {
  projectId?: string;
  initialBoardId?: string;
}

type SwimlanField = 'none' | 'epic' | 'assignee' | 'priority' | 'labels' | 'sprint';
type CardLayout = 'FULL' | 'COMPACT' | 'MINI';
type ViewMode = 'board' | 'swimlane';

const DEFAULT_QUICK_FILTERS: QuickFilter[] = [
  { id: 'qf-assigned-me', name: 'Assigned to Me', jql: 'assignee = currentUser()' },
  { id: 'qf-reporter-me', name: 'Reported by Me', jql: 'reporter = currentUser()' },
  { id: 'qf-recently-updated', name: 'Recently Updated', jql: 'updated >= -1d' },
  { id: 'qf-no-assignee', name: 'Unassigned', jql: 'assignee is empty' },
  { id: 'qf-has-due-date', name: 'Has Due Date', jql: 'duedate is not empty' },
];

const DEFAULT_COLUMNS: BoardColumn[] = [
  { id: 'col-backlog', name: 'Backlog', sequence: 0, statusCategory: 'TODO', isDone: false, currentIssues: 0, color: '#6c757d', isCollapsible: true, isHidden: false },
  { id: 'col-todo', name: 'To Do', sequence: 1, statusCategory: 'TODO', isDone: false, currentIssues: 0, color: '#6c757d', isCollapsible: true, isHidden: false, maxIssues: undefined },
  { id: 'col-inprogress', name: 'In Progress', sequence: 2, statusCategory: 'IN_PROGRESS', isDone: false, currentIssues: 0, color: '#0066ff', isCollapsible: true, isHidden: false, maxIssues: 5 },
  { id: 'col-review', name: 'In Review', sequence: 3, statusCategory: 'IN_REVIEW', isDone: false, currentIssues: 0, color: '#ff9200', isCollapsible: true, isHidden: false, maxIssues: 3 },
  { id: 'col-done', name: 'Done', sequence: 4, statusCategory: 'DONE', isDone: true, currentIssues: 0, color: '#28a745', isCollapsible: true, isHidden: false },
];

export default function EnhancedKanbanBoard({ projectId, initialBoardId }: EnhancedKanbanBoardProps) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();

  // Board state
  const [boardId, setBoardId] = useState<string | null>(initialBoardId || searchParams.get('boardId'));
  const [boardType, setBoardType] = useState<'SCRUM' | 'KANBAN' | 'BADGE'>('SCRUM');
  const [activeSprintId, setActiveSprintId] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<ViewMode>('board');

  // Column configuration
  const [columns, setColumns] = useState<BoardColumn[]>(DEFAULT_COLUMNS);

  // Filter state
  const [quickFilters, setQuickFilters] = useState<QuickFilter[]>(DEFAULT_QUICK_FILTERS);
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

  // Fetch board data
  const { data: boardData, isLoading: boardLoading, error: boardError, refetch: refetchBoard } = useQuery({
    queryKey: ['board-data', boardId],
    queryFn: async () => {
      if (boardId) {
        return boardApi.getBoardData(boardId);
      }
      return null;
    },
    enabled: !!boardId,
  });

  // Fetch issues from board API or issue API
  const { data: issues, isLoading: issuesLoading, refetch: refetchIssues } = useQuery({
    queryKey: ['board-issues', boardId, activeQuickFilter, searchQuery],
    queryFn: async () => {
      if (boardId) {
        const jql = activeQuickFilter
          ? quickFilters.find(f => f.id === activeQuickFilter)?.jql
          : undefined;
        return boardApi.getBoardIssues(boardId, jql);
      }
      // Fallback to issue API
      const params: Record<string, string> = {};
      if (projectId) params['projectId'] = projectId;
      if (searchQuery) params['search'] = searchQuery;
      const response = await issueApi.getAll(params);
      return (response.data || []) as BoardIssue[];
    },
  });

  // Transition mutation
  const transitionMutation = useMutation({
    mutationFn: ({
      issueId,
      statusId,
      pid,
    }: {
      issueId: string;
      statusId: string;
      pid: string;
    }) => issueApi.transitionStatus(issueId, pid, { statusId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['board-issues'] });
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

  // Quick filter mutation (for custom quick filters)
  const quickFilterMutation = useMutation({
    mutationFn: ({ boardId, quickFilterId }: { boardId: string; quickFilterId: string }) =>
      boardApi.applyQuickFilter(boardId, quickFilterId),
    onSuccess: (data) => {
      console.log('Quick filter applied:', data.length, 'issues');
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

  // Handle drop
  const handleDrop = useCallback((e: React.DragEvent, column: BoardColumn, targetSwimlane?: string) => {
    e.preventDefault();
    setDragOverColumn(null);
    setDragOverSwimlane(null);

    if (!draggedIssue) return;

    const issueStatus = column.statusCategory === 'TODO' ? 'To Do'
      : column.statusCategory === 'IN_PROGRESS' ? (column.name === 'In Review' ? 'In Review' : 'In Progress')
      : column.statusCategory === 'DONE' ? 'Done' : column.name;

    if (draggedIssue.status !== issueStatus) {
      const pid = projectId || draggedIssue.projectId;
      if (pid) {
        transitionMutation.mutate({
          issueId: draggedIssue.id,
          statusId: issueStatus,
          pid,
        });
      }
    }

    // Update local state optimistically
    if (issues) {
      const updatedIssues = issues.map(i =>
        i.id === draggedIssue.id ? { ...i, status: issueStatus } : i
      );
      queryClient.setQueryData(['board-issues', boardId, activeQuickFilter, searchQuery], updatedIssues);
    }

    setDraggedIssue(null);
  }, [draggedIssue, issues, boardId, activeQuickFilter, searchQuery, transitionMutation, queryClient]);

  // Handle drag end
  const handleDragEnd = useCallback(() => {
    setDraggedIssue(null);
    setDragOverColumn(null);
    setDragOverSwimlane(null);
  }, []);

  // Get issues by column
  const getIssuesByColumn = useCallback((column: BoardColumn, swimlaneKey?: string) => {
    if (!issues) return [];

    let filteredIssues = issues.filter(issue => {
      // Map status category to actual status
      const statusMatch = column.statusCategory === 'TODO'
        ? issue.status === 'To Do' || issue.status === 'Backlog'
        : column.statusCategory === 'IN_PROGRESS'
        ? issue.status === 'In Progress' || issue.status === 'In Review'
        : column.statusCategory === 'DONE'
        ? issue.status === 'Done' || issue.status === 'Closed'
        : true;

      if (!statusMatch) return false;

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

    return filteredIssues;
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

  // Handle quick filter selection
  const handleQuickFilter = useCallback((filterId: string | null) => {
    setActiveQuickFilter(filterId);
    if (filterId && boardId) {
      quickFilterMutation.mutate({ boardId, quickFilterId: filterId });
    }
  }, [boardId, quickFilterMutation]);

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
  const handleColumnConfigChange = useCallback((columnId: string, config: Partial<BoardColumn>) => {
    setColumns(prev => prev.map(col =>
      col.id === columnId ? { ...col, ...config } : col
    ));
  }, []);

  // Handle card click
  const handleCardClick = useCallback((issue: BoardIssue) => {
    navigate(`/issues/${issue.id}`);
  }, [navigate]);

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
    const columnIssues = issues?.filter(issue => {
      const statusMatch = column.statusCategory === 'TODO'
        ? issue.status === 'To Do' || issue.status === 'Backlog'
        : column.statusCategory === 'IN_PROGRESS'
        ? issue.status === 'In Progress' || issue.status === 'In Review'
        : column.statusCategory === 'DONE'
        ? issue.status === 'Done' || issue.status === 'Closed'
        : true;
      return statusMatch;
    }) || [];

    return { total: columnIssues.length, bySwimlane: new Map() };
  }, [issues]);

  // Toggle swimlane view
  const toggleSwimlaneView = useCallback(() => {
    setViewMode(prev => prev === 'board' ? 'swimlane' : 'board');
  }, []);

  // Loading state
  if (boardLoading) {
    return (
      <div className="ab-board-loading">
        <div className="ab-loading-spinner">
          <div className="ab-spinner-lg"></div>
          <p>Loading board...</p>
        </div>
      </div>
    );
  }

  // Error state
  if (boardError) {
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

  return (
    <div className="ab-enhanced-kanban-board">
      {/* Board Header */}
      <BoardHeader
        boardType={boardType}
        boardName={boardData?.board?.name || (boardType === 'SCRUM' ? 'Scrum Board' : 'Kanban Board')}
        cardLayout={cardLayout}
        onCardLayoutChange={setCardLayout}
        onOpenConfig={() => setShowConfigPanel(true)}
        viewMode={viewMode}
        onToggleView={toggleSwimlaneView}
        activeSprintId={activeSprintId}
        onSprintChange={setActiveSprintId}
      />

      {/* Quick Filters */}
      <QuickFilterBar
        quickFilters={quickFilters}
        activeFilter={activeQuickFilter}
        onFilterChange={handleQuickFilter}
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
      />

      {/* Swimlane Toggle */}
      {boardType === 'SCRUM' && (
        <div className="ab-swimlane-controls">
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
        </div>
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

              return (
                <KanbanColumn
                  key={column.id}
                  column={column}
                  issues={columnIssues}
                  isOver={isOver}
                  wipStatus={wipStatus}
                  onDragOver={(e) => handleDragOver(e, column.id)}
                  onDragLeave={handleDragLeave}
                  onDrop={(e) => handleDrop(e, column)}
                  onDragStart={handleDragStart}
                  onDragEnd={handleDragEnd}
                  onCardClick={handleCardClick}
                  onCreateIssue={() => handleCreateIssue(column.statusCategory)}
                  getCardColor={getCardColor}
                  cardLayout={cardLayout}
                  draggedIssue={draggedIssue}
                  showWorkVsCapacity={showWorkVsCapacity}
                  boardCapacity={boardCapacity}
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
          onClose={() => setShowConfigPanel(false)}
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
          onClose={() => setShowCreateModal(false)}
          onSuccess={() => setShowCreateModal(false)}
        />
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
          overflow: auto;
          padding: var(--ab-spacing-md);
        }

        .ab-board-columns {
          display: flex;
          gap: var(--ab-spacing-md);
          min-height: 100%;
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
  );
}