import React, { useState, useMemo } from 'react';
import { useSprintIssues, SprintIssueResponse } from '../../hooks/useSprint';
import { useBoard, BoardColumnResponse as BoardColumn, BoardSwimlaneResponse as BoardSwimlane } from '../../hooks/useBoardConfig';
import SprintCard from './SprintCard';

interface SwimlanesViewProps {
  sprintId: string;
  boardId: string;
  onCompleteIssue: (planItemId: string) => void;
  onRemoveIssue: (planItemId: string) => void;
}

type GroupingField = 'NONE' | 'EPIC' | 'ASSIGNEE' | 'PROJECT' | 'PRIORITY' | 'LABEL';

interface Swimlane {
  id: string;
  name: string;
  issues: SprintIssueResponse[];
  isCollapsed?: boolean;
}

export default function SwimlanesView({
  sprintId,
  boardId,
  onCompleteIssue,
  onRemoveIssue,
}: SwimlanesViewProps) {
  const { data: issues, isLoading } = useSprintIssues(sprintId);
  const { data: board } = useBoard(boardId);

  const [groupBy, setGroupBy] = useState<GroupingField>('NONE');
  const [collapsedLanes, setCollapsedLanes] = useState<Set<string>>(new Set());

  // Group issues by swimlane
  const swimlanes = useMemo((): Swimlane[] => {
    if (!issues) return [];

    if (groupBy === 'NONE') {
      return [{
        id: 'all',
        name: 'All Issues',
        issues,
      }];
    }

    const groups = new Map<string, SprintIssueResponse[]>();

    issues.forEach((issue) => {
      let key = 'Unassigned';
      // In a real implementation, you would extract the grouping field from the issue
      // For now, we'll group by a placeholder
      switch (groupBy) {
        case 'EPIC':
          key = (issue as any).epicName || 'No Epic';
          break;
        case 'ASSIGNEE':
          key = (issue as any).assignee || 'Unassigned';
          break;
        case 'PROJECT':
          key = (issue as any).project || 'No Project';
          break;
        case 'PRIORITY':
          key = (issue as any).priority || 'No Priority';
          break;
        case 'LABEL':
          key = (issue as any).labels?.[0] || 'No Label';
          break;
      }

      if (!groups.has(key)) {
        groups.set(key, []);
      }
      groups.get(key)!.push(issue);
    });

    return Array.from(groups.entries()).map(([name, laneIssues]) => ({
      id: name,
      name,
      issues: laneIssues,
    }));
  }, [issues, groupBy]);

  const toggleLane = (laneId: string) => {
    setCollapsedLanes((prev) => {
      const next = new Set(prev);
      if (next.has(laneId)) {
        next.delete(laneId);
      } else {
        next.add(laneId);
      }
      return next;
    });
  };

  const getColumnIssues = (swimlane: Swimlane, column: BoardColumn) => {
    return swimlane.issues.filter((issue) => {
      const isCompleted = issue.completionStatus === 'COMPLETED';
      const columnName = column.name.toLowerCase();

      if (columnName === 'done') return isCompleted;
      if (columnName === 'in progress') return !isCompleted && issue.completionStatus === 'IN_PROGRESS';
      return !isCompleted;
    });
  };

  if (isLoading) {
    return <div className="ab-swimlanes-loading">Loading swimlanes...</div>;
  }

  const groupingOptions: { value: GroupingField; label: string }[] = [
    { value: 'NONE', label: 'None' },
    { value: 'EPIC', label: 'Epic' },
    { value: 'ASSIGNEE', label: 'Assignee' },
    { value: 'PROJECT', label: 'Project' },
    { value: 'PRIORITY', label: 'Priority' },
    { value: 'LABEL', label: 'Label' },
  ];

  return (
    <div className="ab-swimlanes-view">
      {/* Controls */}
      <div className="ab-swimlanes-controls">
        <label className="ab-swimlanes-label">Group by:</label>
        <select
          className="ab-select"
          value={groupBy}
          onChange={(e) => setGroupBy(e.target.value as GroupingField)}
        >
          {groupingOptions.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      </div>

      {/* Swimlane Headers */}
      {groupBy !== 'NONE' && (
        <div className="ab-swimlanes-headers">
          <div className="ab-swimlane-header-cell ab-swimlane-label-cell">
            Swimlane
          </div>
          {board?.columns?.map((column) => (
            <div
              key={column.id}
              className="ab-swimlane-header-cell"
              style={{ backgroundColor: column.color || '#f0f0f0' }}
            >
              {column.name}
            </div>
          ))}
        </div>
      )}

      {/* Swimlane Rows */}
      <div className="ab-swimlanes-body">
        {swimlanes.map((swimlane) => {
          const isCollapsed = collapsedLanes.has(swimlane.id);

          return (
            <div key={swimlane.id} className="ab-swimlane-row">
              {/* Swimlane Label */}
              <div
                className="ab-swimlane-label"
                onClick={() => toggleLane(swimlane.id)}
              >
                <span className={`ab-swimlane-toggle ${isCollapsed ? 'ab-collapsed' : ''}`}>
                  {isCollapsed ? '▶' : '▼'}
                </span>
                <span className="ab-swimlane-name">{swimlane.name}</span>
                <span className="ab-swimlane-count">({swimlane.issues.length})</span>
              </div>

              {/* Swimlane Columns */}
              {!isCollapsed && (
                <div className="ab-swimlane-columns">
                  {board?.columns?.map((column) => (
                    <div key={column.id} className="ab-swimlane-column">
                      {getColumnIssues(swimlane, column).map((issue) => (
                        <SprintCard
                          key={issue.id}
                          issue={issue}
                          sprintId={sprintId}
                          onComplete={() => onCompleteIssue(issue.planItemId)}
                          onRemove={() => onRemoveIssue(issue.planItemId)}
                        />
                      ))}
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Empty state */}
      {swimlanes.length === 0 && (
        <div className="ab-swimlanes-empty">
          <p>No issues in this sprint.</p>
        </div>
      )}
    </div>
  );
}
