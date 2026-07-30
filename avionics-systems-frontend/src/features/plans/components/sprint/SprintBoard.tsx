import React, { useState, useMemo } from 'react';
import {
  DndContext,
  DragOverlay,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragStartEvent,
  DragEndEvent,
} from '@dnd-kit/core';
import {
  SortableContext,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { useNavigate } from 'react-router-dom';
import {
  useSprint,
  useSprints,
  useSprintIssues,
  useSprintBurndown,
  useCompleteIssue,
  useRemoveIssueFromSprint,
  useStartSprint,
  useCloseSprint,
  useUpdateIssueColumn,
  useReopenSprint,
  useBulkMoveIssues,
  useMoveToBacklog,
  useToggleFlag,
  useUpdateEstimation,
  useRankIssue,
  useEventBurndown,
  SprintIssueResponse,
} from '../../hooks/useSprint';
import { useBoard } from '../../hooks/useBoardConfig';
import SprintCard from './SprintCard';
import SprintHeader from './SprintHeader';
import BurndownChart from './BurndownChart';
import QuickFilters from './QuickFilters';
import CloseSprintDialog from './CloseSprintDialog';

interface SprintBoardProps {
  sprintId: string;
  boardId: string;
}

export default function SprintBoard({ sprintId, boardId }: SprintBoardProps) {
  const navigate = useNavigate();
  const { data: sprint, isLoading: sprintLoading } = useSprint(sprintId);
  const { data: issues, isLoading: issuesLoading } = useSprintIssues(sprintId);
  const { data: burndown } = useSprintBurndown(sprintId);
  const { data: board } = useBoard(boardId);
  const { data: allSprints } = useSprints(boardId);

  const completeIssue = useCompleteIssue();
  const removeIssue = useRemoveIssueFromSprint();
  const startSprint = useStartSprint();
  const closeSprint = useCloseSprint();
  const updateIssueColumn = useUpdateIssueColumn();
  const reopenSprint = useReopenSprint();
  const bulkMove = useBulkMoveIssues();
  const moveToBacklog = useMoveToBacklog();
  const toggleFlag = useToggleFlag();
  const updateEstimation = useUpdateEstimation();
  const rankIssue = useRankIssue();

  const [selectedIssue, setSelectedIssue] = useState<string | null>(null);
  const [activeIssue, setActiveIssue] = useState<string | null>(null);
  const [filterMode, setFilterMode] = useState<'ALL' | 'MY_ISSUES' | 'RECENTLY_UPDATED'>('ALL');

  // Gap 4: Multi-select mode
  const [multiSelectMode, setMultiSelectMode] = useState(false);
  const [checkedIssues, setCheckedIssues] = useState<Set<string>>(new Set());
  const [bulkMoveTargetSprint, setBulkMoveTargetSprint] = useState<string>('');

  // Gap 20: Close sprint dialog
  const [showCloseDialog, setShowCloseDialog] = useState(false);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
    useSensor(KeyboardSensor)
  );

  const filteredIssues = useMemo(() => {
    if (!issues) return [];
    if (filterMode === 'ALL') return issues;

    const now = new Date();
    const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);

    switch (filterMode) {
      case 'MY_ISSUES':
        return issues.filter(issue => issue.assigneeId !== null && issue.assigneeId !== undefined);
      case 'RECENTLY_UPDATED':
        return issues.filter(issue => {
          if (!issue.updatedAt) return true;
          return new Date(issue.updatedAt) >= sevenDaysAgo;
        });
      default:
        return issues;
    }
  }, [issues, filterMode]);

  const issuesByColumn = useMemo(() => {
    if (!board?.columns || !filteredIssues) return {};

    return board.columns.reduce((acc: Record<string, SprintIssueResponse[]>, column: any) => {
      acc[column.id] = filteredIssues.filter((issue) => {
        const isCompleted = issue.completionStatus === 'COMPLETED';
        const columnName = column.name.toLowerCase();
        if (columnName === 'done') return isCompleted;
        if (columnName === 'in progress') return !isCompleted && issue.completionStatus === 'UNCOMPLETED';
        if (columnName === 'to do' || columnName === 'todo') return !isCompleted;
        return !isCompleted;
      });
      return acc;
    }, {} as Record<string, SprintIssueResponse[]>);
  }, [board?.columns, filteredIssues]);

  const handleDragStart = (event: DragStartEvent) => {
    setActiveIssue(event.active.id as string);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    setActiveIssue(null);
    if (!over || active.id === over.id) return;

    const targetColumnId = over.id as string;
    const issue = filteredIssues.find((i) => i.id === active.id);

    if (issue && board?.columns) {
      const targetColumn = board.columns.find((c: any) => c.id === targetColumnId);
      if (targetColumn) {
        updateIssueColumn.mutate({ sprintId, planItemId: issue.planItemId, columnName: targetColumn.name });
      }
    }
  };

  const handleToggleCheck = (issueId: string) => {
    const newChecked = new Set(checkedIssues);
    if (newChecked.has(issueId)) {
      newChecked.delete(issueId);
    } else {
      newChecked.add(issueId);
    }
    setCheckedIssues(newChecked);
  };

  const handleBulkMove = () => {
    if (bulkMoveTargetSprint && checkedIssues.size > 0) {
      const planItemIds = filteredIssues
        .filter(i => checkedIssues.has(i.id))
        .map(i => i.planItemId);
      bulkMove.mutate({
        sprintId: bulkMoveTargetSprint,
        issueIds: planItemIds,
        userId: 'current',
      });
      setCheckedIssues(new Set());
      setMultiSelectMode(false);
    }
  };

  const handleBulkMoveToBacklog = () => {
    if (checkedIssues.size > 0) {
      const planItemIds = filteredIssues
        .filter(i => checkedIssues.has(i.id))
        .map(i => i.planItemId);
      moveToBacklog.mutate({ planItemIds, userId: 'current' });
      setCheckedIssues(new Set());
      setMultiSelectMode(false);
    }
  };

  if (sprintLoading || issuesLoading) {
    return <div className="ab-sprint-loading">Loading sprint...</div>;
  }

  if (!sprint) {
    return <div className="ab-sprint-error">Sprint not found</div>;
  }

  const activeIssueData = activeIssue ? filteredIssues.find((i) => i.id === activeIssue) : null;
  const incompleteIssues = (issues || []).filter(i => i.completionStatus !== 'COMPLETED');
  const otherSprints = (allSprints || []).filter(s => s.id !== sprintId && (s.state === 'FUTURE' || s.state === 'ACTIVE'));

  return (
    <div className="ab-sprint-board">
      <SprintHeader
        sprint={sprint}
        onStart={() => startSprint.mutate({ sprintId })}
        onClose={() => setShowCloseDialog(true)}
        onReopen={() => reopenSprint.mutate({ sprintId, userId: 'current' })}
      />

      {/* Bulk Action Bar (Gap 4) */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px', flexWrap: 'wrap' }}>
        <QuickFilters activeFilter={filterMode} onFilterChange={setFilterMode} />

        <button
          className={`ab-btn ab-btn-sm ${multiSelectMode ? 'ab-btn-primary' : 'ab-btn-secondary'}`}
          onClick={() => { setMultiSelectMode(!multiSelectMode); setCheckedIssues(new Set()); }}
          style={{ marginLeft: 'auto' }}
        >
          {multiSelectMode ? 'Cancel Select' : 'Multi-Select'}
        </button>
      </div>

      {multiSelectMode && checkedIssues.size > 0 && (
        <div style={{
          display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 12px',
          background: 'var(--ab-primary-50, #eff6ff)', border: '1px solid var(--ab-primary-200, #bfdbfe)',
          borderRadius: '8px', marginBottom: '12px', fontSize: '0.875rem',
        }}>
          <span style={{ fontWeight: 500 }}>{checkedIssues.size} selected</span>
          <select
            value={bulkMoveTargetSprint}
            onChange={(e) => setBulkMoveTargetSprint(e.target.value)}
            style={{ padding: '4px 8px', borderRadius: '4px', border: '1px solid #d1d5db', fontSize: '0.813rem' }}
          >
            <option value="">Move to sprint...</option>
            {otherSprints.map(s => (
              <option key={s.id} value={s.id}>{s.name} ({s.state})</option>
            ))}
          </select>
          <button className="ab-btn ab-btn-sm ab-btn-primary" onClick={handleBulkMove} disabled={!bulkMoveTargetSprint}>
            Move
          </button>
          <button className="ab-btn ab-btn-sm ab-btn-secondary" onClick={handleBulkMoveToBacklog}>
            Send to Backlog
          </button>
        </div>
      )}

      <DndContext sensors={sensors} collisionDetection={closestCenter} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
        <div className="ab-board-columns">
          {board?.columns?.map((column: any) => (
            <div key={column.id} className="ab-board-column" data-column-id={column.id}>
              <div className="ab-column-header" style={{ backgroundColor: column.color || '#f0f0f0' }}>
                <h3 className="ab-column-name">{column.name}</h3>
                <span className="ab-column-count">{issuesByColumn[column.id]?.length || 0}</span>
                {column.maxIssues && <span className="ab-column-wip">WIP: {column.maxIssues}</span>}
              </div>
              <SortableContext id={column.id} items={issuesByColumn[column.id]?.map((i) => i.id) || []} strategy={verticalListSortingStrategy}>
                <div className="ab-column-cards" data-column-id={column.id}>
                  {issuesByColumn[column.id]?.map((issue) => (
                    <SprintCard
                      key={issue.id}
                      issue={issue}
                      sprintId={sprintId}
                      isSelected={selectedIssue === issue.id}
                      multiSelectMode={multiSelectMode}
                      isChecked={checkedIssues.has(issue.id)}
                      onToggleCheck={() => handleToggleCheck(issue.id)}
                      onSelect={() => setSelectedIssue(selectedIssue === issue.id ? null : issue.id)}
                      onComplete={() => completeIssue.mutate({ sprintId, planItemId: issue.planItemId })}
                      onRemove={() => removeIssue.mutate({ sprintId, planItemId: issue.planItemId })}
                      onClick={() => navigate(`/issues/${issue.issueId}`)}
                      onFlag={(flagged, reason) => toggleFlag.mutate({ sprintId, planItemId: issue.planItemId, flagged, reason, userId: 'current' })}
                      onMoveToBacklog={() => moveToBacklog.mutate({ planItemIds: [issue.planItemId], userId: 'current' })}
                      onEstimationChange={(pts) => updateEstimation.mutate({ boardId, planItemId: issue.planItemId, value: pts, userId: 'current' })}
                    />
                  ))}
                </div>
              </SortableContext>
            </div>
          ))}
        </div>

        <DragOverlay>
          {activeIssueData && (
            <div className="ab-drag-overlay">
              <SprintCard issue={activeIssueData} sprintId={sprintId} isSelected={false} isDragging />
            </div>
          )}
        </DragOverlay>
      </DndContext>

      <BurndownChart burndown={burndown} sprint={sprint} />

      {/* Gap 20: Close Sprint Dialog */}
      {showCloseDialog && (
        <CloseSprintDialog
          sprint={sprint}
          boardId={boardId}
          incompleteIssues={incompleteIssues}
          onClose={() => setShowCloseDialog(false)}
        />
      )}
    </div>
  );
}
