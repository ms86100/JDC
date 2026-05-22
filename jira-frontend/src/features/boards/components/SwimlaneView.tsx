import React from 'react';
import { BoardColumn, BoardIssue } from '../../../api/boardApi';
import KanbanColumn from './KanbanColumn';

type CardLayout = 'FULL' | 'COMPACT' | 'MINI';
type SwimlanField = 'none' | 'epic' | 'assignee' | 'priority' | 'labels' | 'sprint';

interface SwimlaneViewProps {
  columns: BoardColumn[];
  swimlanes: { key: string; label: string }[];
  issues: BoardIssue[];
  collapsedSwimlanes: Set<string>;
  onToggleSwimlane: (swimlaneKey: string) => void;
  getIssuesByColumn: (column: BoardColumn, swimlaneKey?: string) => BoardIssue[];
  onDragStart: (e: React.DragEvent, issue: BoardIssue) => void;
  onDragOver: (e: React.DragEvent, columnId: string, swimlaneKey?: string) => void;
  onDragLeave: () => void;
  onDrop: (e: React.DragEvent, column: BoardColumn, targetSwimlane?: string) => void;
  onDragEnd: () => void;
  onCardClick: (issue: BoardIssue) => void;
  getCardColor: (issue: BoardIssue) => string | undefined;
  cardLayout: CardLayout;
  draggedIssue: BoardIssue | null;
  dragOverColumn: string | null;
  swimlaneField: SwimlanField;
}

export default function SwimlaneView({
  columns,
  swimlanes,
  issues,
  collapsedSwimlanes,
  onToggleSwimlane,
  getIssuesByColumn,
  onDragStart,
  onDragOver,
  onDragLeave,
  onDrop,
  onDragEnd,
  onCardClick,
  getCardColor,
  cardLayout,
  draggedIssue,
  dragOverColumn,
  swimlaneField,
}: SwimlaneViewProps) {
  const getSwimlaneIcon = (field: SwimlanField) => {
    switch (field) {
      case 'epic': return '⚡';
      case 'assignee': return '👤';
      case 'priority': return '🔺';
      case 'labels': return '🏷️';
      case 'sprint': return '🏃';
      default: return '📋';
    }
  };

  const getSwimlaneColor = (key: string, field: SwimlanField) => {
    if (field === 'priority') {
      switch (key.toLowerCase()) {
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
      }
    }
    if (field === 'epic') {
      // Find an issue with this epic to get the color
      const issue = issues.find(i => i.epicId === key);
      return issue?.epicColor || '#6f42c1';
    }
    return '#6c757d';
  };

  return (
    <div className="ab-swimlane-view">
      {swimlanes.map((swimlane) => {
        const isCollapsed = collapsedSwimlanes.has(swimlane.key);
        const swimlaneColor = getSwimlaneColor(swimlane.key, swimlaneField);

        return (
          <div key={swimlane.key} className="ab-swimlane">
            {/* Swimlane Header */}
            <div
              className="ab-swimlane-header"
              onClick={() => onToggleSwimlane(swimlane.key)}
              style={{ borderLeftColor: swimlaneColor }}
            >
              <div className="ab-swimlane-toggle">
                <svg
                  width="12"
                  height="12"
                  viewBox="0 0 16 16"
                  fill="currentColor"
                  style={{ transform: isCollapsed ? 'rotate(-90deg)' : 'rotate(0deg)', transition: 'transform 0.2s' }}
                >
                  <path d="M4.646 1.646a.5.5 0 0 1 .708 0l6 6a.5.5 0 0 1 0 .708l-6 6a.5.5 0 0 1-.708-.708L10.293 8 4.646 2.354a.5.5 0 0 1 0-.708z"/>
                </svg>
              </div>
              <span className="ab-swimlane-icon">{getSwimlaneIcon(swimlaneField)}</span>
              <span className="ab-swimlane-label">{swimlane.label}</span>
              <span className="ab-swimlane-count">
                {issues.filter(issue => {
                  switch (swimlaneField) {
                    case 'epic': return issue.epicId === swimlane.key;
                    case 'assignee': return issue.assigneeId === swimlane.key;
                    case 'priority': return issue.priority === swimlane.key;
                    case 'labels': return issue.labels?.includes(swimlane.key);
                    default: return true;
                  }
                }).length} issues
              </span>
            </div>

            {/* Swimlane Content */}
            {!isCollapsed && (
              <div className="ab-swimlane-content">
                <div className="ab-swimlane-columns">
                  {columns.filter(col => !col.isHidden).map((column) => {
                    const columnIssues = getIssuesByColumn(column, swimlane.key);
                    const isOver = dragOverColumn === `${swimlane.key}-${column.id}`;

                    return (
                      <KanbanColumn
                        key={`${swimlane.key}-${column.id}`}
                        column={column}
                        issues={columnIssues}
                        isOver={isOver}
                        wipStatus={{ status: 'ok', message: '' }}
                        onDragOver={(e) => onDragOver(e, `${swimlane.key}-${column.id}`, swimlane.key)}
                        onDragLeave={onDragLeave}
                        onDrop={(e) => onDrop(e, column, swimlane.key)}
                        onDragStart={onDragStart}
                        onDragEnd={onDragEnd}
                        onCardClick={onCardClick}
                        onCreateIssue={() => {}}
                        getCardColor={getCardColor}
                        cardLayout={cardLayout}
                        draggedIssue={draggedIssue}
                        showWorkVsCapacity={false}
                        boardCapacity={{ capacity: 0, committed: 0 }}
                      />
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        );
      })}

      {/* No swimlanes state */}
      {swimlanes.length === 0 && (
        <div className="ab-swimlane-empty">
          <span className="ab-empty-icon">🌊</span>
          <h3>No Swimlanes</h3>
          <p>Select a field from the dropdown above to group issues by.</p>
        </div>
      )}

      <style>{`
        .ab-swimlane-view {
          display: flex;
          flex-direction: column;
          gap: var(--ab-spacing-md);
        }

        .ab-swimlane {
          background: var(--ab-white);
          border-radius: var(--ab-radius-lg);
          border: 1px solid var(--ab-gray-200);
          overflow: hidden;
        }

        .ab-swimlane-header {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
          padding: var(--ab-spacing-md);
          background: var(--ab-gray-50);
          border-left: 4px solid var(--ab-gray-400);
          cursor: pointer;
          user-select: none;
        }

        .ab-swimlane-header:hover {
          background: var(--ab-gray-100);
        }

        .ab-swimlane-toggle {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 20px;
          height: 20px;
          color: var(--ab-gray-500);
        }

        .ab-swimlane-icon {
          font-size: var(--ab-font-size-lg);
        }

        .ab-swimlane-label {
          font-size: var(--ab-font-size-base);
          font-weight: 600;
          color: var(--ab-gray-800);
          flex: 1;
        }

        .ab-swimlane-count {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-500);
          background: var(--ab-gray-200);
          padding: 2px 8px;
          border-radius: var(--ab-radius-full);
        }

        .ab-swimlane-content {
          padding: var(--ab-spacing-md);
          background: var(--ab-gray-50);
        }

        .ab-swimlane-columns {
          display: flex;
          gap: var(--ab-spacing-md);
          overflow-x: auto;
        }

        .ab-swimlane-empty {
          text-align: center;
          padding: var(--ab-spacing-xl);
          color: var(--ab-gray-500);
        }

        .ab-swimlane-empty .ab-empty-icon {
          font-size: 48px;
          display: block;
          margin-bottom: var(--ab-spacing-md);
        }

        .ab-swimlane-empty h3 {
          margin: 0 0 var(--ab-spacing-sm);
          color: var(--ab-gray-700);
        }

        .ab-swimlane-empty p {
          margin: 0;
          font-size: var(--ab-font-size-sm);
        }
      `}</style>
    </div>
  );
}