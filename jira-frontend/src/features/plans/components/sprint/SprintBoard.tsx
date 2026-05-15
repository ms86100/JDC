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
  useSprintIssues,
  useSprintBurndown,
  useCompleteIssue,
  useRemoveIssueFromSprint,
  useStartSprint,
  useCloseSprint,
  useUpdateIssueColumn,
} from '../../hooks/useSprint';
import { useBoard } from '../../hooks/useBoardConfig';
import SprintCard from './SprintCard';
import SprintHeader from './SprintHeader';
import BurndownChart from './BurndownChart';
import QuickFilters from './QuickFilters';

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

  const completeIssue = useCompleteIssue();
  const removeIssue = useRemoveIssueFromSprint();
  const startSprint = useStartSprint();
  const closeSprint = useCloseSprint();
  const updateIssueColumn = useUpdateIssueColumn();

  const [selectedIssue, setSelectedIssue] = useState<string | null>(null);
  const [activeIssue, setActiveIssue] = useState<string | null>(null);
  const [filterMode, setFilterMode] = useState<'ALL' | 'MY_ISSUES' | 'RECENTLY_UPDATED'>('ALL');

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    }),
    useSensor(KeyboardSensor)
  );

  // Filter issues based on quick filter
  const filteredIssues = useMemo(() => {
    if (!issues) return [];
    if (filterMode === 'ALL') return issues;

    switch (filterMode) {
      case 'MY_ISSUES':
        // Filter to issues assigned to current user or unassigned
        return issues; // TODO: Replace with actual current user check
      case 'RECENTLY_UPDATED':
        // Filter to issues updated in last 7 days
        const sevenDaysAgo = new Date();
        sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);
        return issues;
      default:
        return issues;
    }
  }, [issues, filterMode]);

  // Group issues by column
  const issuesByColumn = useMemo(() => {
    if (!board?.columns || !filteredIssues) return {};

    return board.columns.reduce((acc, column) => {
      acc[column.id] = filteredIssues.filter((issue) => {
        // Match based on completion status
        const isCompleted = issue.completionStatus === 'COMPLETED';
        const columnName = column.name.toLowerCase();

        if (columnName === 'done') return isCompleted;
        if (columnName === 'in progress') return !isCompleted && issue.completionStatus === 'UNCOMPLETED';
        if (columnName === 'to do' || columnName === 'todo') return !isCompleted;

        return !isCompleted;
      });
      return acc;
    }, {} as Record<string, typeof filteredIssues>);
  }, [board?.columns, filteredIssues]);

  const handleDragStart = (event: DragStartEvent) => {
    setActiveIssue(event.active.id as string);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    setActiveIssue(null);

    if (!over || active.id === over.id) return;

    // Find the target column
    const targetColumnId = over.id as string;
    const issue = filteredIssues.find((i) => i.id === active.id);

    if (issue && board?.columns) {
      const targetColumn = board.columns.find((c) => c.id === targetColumnId);
      if (targetColumn) {
        // Update issue status based on column
        updateIssueColumn.mutate({
          sprintId,
          planItemId: issue.planItemId,
          columnName: targetColumn.name,
        });
      }
    }
  };

  if (sprintLoading || issuesLoading) {
    return <div className="ab-sprint-loading">Loading sprint...</div>;
  }

  if (!sprint) {
    return <div className="ab-sprint-error">Sprint not found</div>;
  }

  const activeIssueData = activeIssue ? filteredIssues.find((i) => i.id === activeIssue) : null;

  return (
    <div className="ab-sprint-board">
      {/* Sprint Header */}
      <SprintHeader
        sprint={sprint}
        onStart={() => startSprint.mutate({ sprintId })}
        onClose={() => closeSprint.mutate({ sprintId })}
      />

      {/* Quick Filters - Key feature from video */}
      <QuickFilters
        activeFilter={filterMode}
        onFilterChange={setFilterMode}
      />

      {/* Board Columns with Drag and Drop */}
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragStart={handleDragStart}
        onDragEnd={handleDragEnd}
      >
        <div className="ab-board-columns">
          {board?.columns?.map((column) => (
            <div key={column.id} className="ab-board-column" data-column-id={column.id}>
              <div
                className="ab-column-header"
                style={{ backgroundColor: column.color || '#f0f0f0' }}
              >
                <h3 className="ab-column-name">{column.name}</h3>
                <span className="ab-column-count">
                  {issuesByColumn[column.id]?.length || 0}
                </span>
                {column.maxIssues && (
                  <span className="ab-column-wip">WIP: {column.maxIssues}</span>
                )}
              </div>
              <SortableContext
                id={column.id}
                items={issuesByColumn[column.id]?.map((i) => i.id) || []}
                strategy={verticalListSortingStrategy}
              >
                <div
                  className="ab-column-cards"
                  data-column-id={column.id}
                >
                  {issuesByColumn[column.id]?.map((issue) => (
                    <SprintCard
                      key={issue.id}
                      issue={issue}
                      sprintId={sprintId}
                      isSelected={selectedIssue === issue.id}
                      onSelect={() => setSelectedIssue(selectedIssue === issue.id ? null : issue.id)}
                      onComplete={() => completeIssue.mutate({ sprintId, planItemId: issue.planItemId })}
                      onRemove={() => removeIssue.mutate({ sprintId, planItemId: issue.planItemId })}
                      onClick={() => navigate(`/issues/${issue.issueId}`)}
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
              <SprintCard
                issue={activeIssueData}
                sprintId={sprintId}
                isSelected={false}
                isDragging
              />
            </div>
          )}
        </DragOverlay>
      </DndContext>

      {/* Burndown Chart - Enhanced version from video */}
      <BurndownChart
        burndown={burndown}
        sprint={sprint}
      />
    </div>
  );
}
