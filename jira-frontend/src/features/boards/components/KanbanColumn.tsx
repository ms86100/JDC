import React from 'react';
import { BoardColumn, BoardIssue } from '../../../api/boardApi';
import IssueCard from './IssueCard';

type CardLayout = 'FULL' | 'COMPACT' | 'MINI';

interface KanbanColumnProps {
  column: BoardColumn;
  issues: BoardIssue[];
  isOver: boolean;
  wipStatus: { status: string; message: string };
  onDragOver: (e: React.DragEvent) => void;
  onDragLeave: () => void;
  onDrop: (e: React.DragEvent) => void;
  onDragStart: (e: React.DragEvent, issue: BoardIssue) => void;
  onDragEnd: () => void;
  onCardClick: (issue: BoardIssue) => void;
  onCreateIssue: () => void;
  getCardColor: (issue: BoardIssue) => string | undefined;
  cardLayout: CardLayout;
  draggedIssue: BoardIssue | null;
  showWorkVsCapacity: boolean;
  boardCapacity: { capacity: number; committed: number };
}

export default function KanbanColumn({
  column,
  issues,
  isOver,
  wipStatus,
  onDragOver,
  onDragLeave,
  onDrop,
  onDragStart,
  onDragEnd,
  onCardClick,
  onCreateIssue,
  getCardColor,
  cardLayout,
  draggedIssue,
  showWorkVsCapacity,
  boardCapacity,
}: KanbanColumnProps) {
  const isCollapsed = column.isHidden;

  return (
    <div className={`ab-kanban-column ${isOver ? 'ab-drag-over' : ''} ${isCollapsed ? 'ab-collapsed' : ''}`}>
      {/* Column Header */}
      <div className="ab-column-header" onClick={() => {}}>
        <div className="ab-column-indicator" style={{ backgroundColor: column.color }} />
        <div className="ab-column-title-row">
          <h3 className="ab-column-title">{column.name}</h3>
          <span className="ab-column-count">{issues.length}</span>
          {column.maxIssues && (
            <span className={`ab-wip-badge ${wipStatus.status}`}>
              {issues.length}/{column.maxIssues}
            </span>
          )}
        </div>
        <div className="ab-column-actions">
          <button
            className="ab-icon-btn"
            onClick={onCreateIssue}
            title="Add issue"
          >
            <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor">
              <path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"/>
            </svg>
          </button>
          {column.isCollapsible && (
            <button
              className="ab-icon-btn"
              title="Collapse column"
            >
              <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor">
                <path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
              </svg>
            </button>
          )}
        </div>
      </div>

      {/* WIP Warning */}
      {wipStatus.status !== 'ok' && (
        <div className={`ab-wip-warning ab-wip-${wipStatus.status}`}>
          <span className="ab-wip-icon">
            {wipStatus.status === 'exceeded' ? '⚠️' : '⚡'}
          </span>
          {wipStatus.message}
        </div>
      )}

      {/* Work vs Capacity Bar (for In Progress columns) */}
      {showWorkVsCapacity && column.statusCategory === 'IN_PROGRESS' && (
        <div className="ab-capacity-bar-container">
          <div className="ab-capacity-bar">
            <div
              className="ab-capacity-fill"
              style={{
                width: `${Math.min((issues.length / column.maxIssues!) * 100, 100)}%`,
                backgroundColor: issues.length >= column.maxIssues! ? '#dc3545' :
                               issues.length >= column.maxIssues! * 0.8 ? '#ffc107' : '#28a745'
              }}
            />
          </div>
          <span className="ab-capacity-label">
            {issues.length} / {column.maxIssues || '∞'} WIP
          </span>
        </div>
      )}

      {/* Column Content */}
      <div
        className="ab-column-content"
        onDragOver={onDragOver}
        onDragLeave={onDragLeave}
        onDrop={onDrop}
      >
        {issues.length === 0 ? (
          <div className="ab-column-empty">
            <div className="ab-empty-state">
              <span className="ab-empty-icon">📋</span>
              <p>No issues</p>
              <button className="ab-btn-link" onClick={onCreateIssue}>
                + Create issue
              </button>
            </div>
          </div>
        ) : (
          <div className="ab-issue-list">
            {issues.map((issue, index) => (
              <IssueCard
                key={issue.id}
                issue={issue}
                layout={cardLayout}
                color={getCardColor(issue)}
                isDragging={draggedIssue?.id === issue.id}
                onDragStart={(e) => onDragStart(e, issue)}
                onDragEnd={onDragEnd}
                onClick={() => onCardClick(issue)}
                rank={index + 1}
              />
            ))}
          </div>
        )}

        {/* Drop indicator */}
        {isOver && (
          <div className="ab-drop-indicator">
            <span>Drop here</span>
          </div>
        )}
      </div>

      {/* Column Footer */}
      <div className="ab-column-footer">
        <button className="ab-add-issue-btn" onClick={onCreateIssue}>
          <svg width="12" height="12" viewBox="0 0 16 16" fill="currentColor">
            <path d="M8 4a.5.5 0 0 1 .5.5v3h3a.5.5 0 0 1 0 1h-3v3a.5.5 0 0 1-1 0v-3h-3a.5.5 0 0 1 0-1h3v-3A.5.5 0 0 1 8 4z"/>
          </svg>
          Add issue
        </button>
      </div>

      <style>{`
        .ab-kanban-column {
          display: flex;
          flex-direction: column;
          background: var(--ab-gray-50);
          border-radius: var(--ab-radius-lg);
          min-width: 280px;
          max-width: 320px;
          flex: 1;
          height: fit-content;
          max-height: calc(100vh - 200px);
          transition: all var(--ab-transition-fast);
        }

        .ab-kanban-column.ab-drag-over {
          background: var(--ab-primary-50);
          border: 2px dashed var(--ab-primary-400);
        }

        .ab-kanban-column.ab-collapsed {
          max-width: 50px;
        }

        .ab-kanban-column.ab-collapsed .ab-column-content,
        .ab-kanban-column.ab-collapsed .ab-column-footer {
          display: none;
        }

        .ab-column-header {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
          padding: var(--ab-spacing-md);
          background: var(--ab-white);
          border-bottom: 1px solid var(--ab-gray-200);
          cursor: pointer;
          border-radius: var(--ab-radius-lg) var(--ab-radius-lg) 0 0;
        }

        .ab-column-indicator {
          width: 4px;
          height: 20px;
          border-radius: 2px;
          flex-shrink: 0;
        }

        .ab-column-title-row {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
          flex: 1;
        }

        .ab-column-title {
          font-size: var(--ab-font-size-sm);
          font-weight: 600;
          color: var(--ab-gray-800);
          margin: 0;
        }

        .ab-column-count {
          font-size: var(--ab-font-size-xs);
          font-weight: 600;
          color: var(--ab-gray-500);
          background: var(--ab-gray-200);
          padding: 2px 6px;
          border-radius: var(--ab-radius-full);
        }

        .ab-wip-badge {
          font-size: var(--ab-font-size-xs);
          font-weight: 500;
          padding: 2px 6px;
          border-radius: var(--ab-radius-full);
        }

        .ab-wip-badge.ok {
          background: var(--ab-gray-100);
          color: var(--ab-gray-600);
        }

        .ab-wip-badge.warning {
          background: var(--ab-warning-100);
          color: var(--ab-warning-700);
        }

        .ab-wip-badge.exceeded {
          background: var(--ab-danger-100);
          color: var(--ab-danger-700);
        }

        .ab-column-actions {
          display: flex;
          gap: var(--ab-spacing-xs);
        }

        .ab-icon-btn {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 24px;
          height: 24px;
          background: transparent;
          border: none;
          border-radius: var(--ab-radius-sm);
          cursor: pointer;
          color: var(--ab-gray-400);
          transition: all var(--ab-transition-fast);
        }

        .ab-icon-btn:hover {
          background: var(--ab-gray-100);
          color: var(--ab-gray-600);
        }

        .ab-wip-warning {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-xs);
          padding: var(--ab-spacing-xs) var(--ab-spacing-sm);
          font-size: var(--ab-font-size-xs);
          font-weight: 500;
        }

        .ab-wip-warning.ab-wip-exceeded {
          background: var(--ab-danger-100);
          color: var(--ab-danger-700);
        }

        .ab-wip-warning.ab-wip-warning {
          background: var(--ab-warning-100);
          color: var(--ab-warning-700);
        }

        .ab-wip-icon {
          font-size: var(--ab-font-size-sm);
        }

        .ab-capacity-bar-container {
          padding: var(--ab-spacing-xs) var(--ab-spacing-md);
          background: var(--ab-gray-100);
        }

        .ab-capacity-bar {
          height: 4px;
          background: var(--ab-gray-300);
          border-radius: 2px;
          overflow: hidden;
        }

        .ab-capacity-fill {
          height: 100%;
          transition: all var(--ab-transition-fast);
        }

        .ab-capacity-label {
          font-size: 10px;
          color: var(--ab-gray-500);
          display: block;
          text-align: right;
          margin-top: 2px;
        }

        .ab-column-content {
          flex: 1;
          padding: var(--ab-spacing-sm);
          overflow-y: auto;
          display: flex;
          flex-direction: column;
          gap: var(--ab-spacing-sm);
          min-height: 100px;
        }

        .ab-issue-list {
          display: flex;
          flex-direction: column;
          gap: var(--ab-spacing-sm);
        }

        .ab-column-empty {
          display: flex;
          align-items: center;
          justify-content: center;
          flex: 1;
          padding: var(--ab-spacing-lg);
        }

        .ab-empty-state {
          text-align: center;
        }

        .ab-empty-icon {
          font-size: 32px;
          display: block;
          margin-bottom: var(--ab-spacing-sm);
          opacity: 0.5;
        }

        .ab-empty-state p {
          margin: 0 0 var(--ab-spacing-sm);
          color: var(--ab-gray-400);
          font-size: var(--ab-font-size-sm);
        }

        .ab-btn-link {
          background: none;
          border: none;
          color: var(--ab-primary-500);
          font-size: var(--ab-font-size-sm);
          cursor: pointer;
          text-decoration: none;
        }

        .ab-btn-link:hover {
          text-decoration: underline;
        }

        .ab-drop-indicator {
          border: 2px dashed var(--ab-primary-400);
          border-radius: var(--ab-radius-md);
          padding: var(--ab-spacing-md);
          text-align: center;
          color: var(--ab-primary-600);
          font-size: var(--ab-font-size-sm);
          font-weight: 500;
          background: var(--ab-primary-50);
        }

        .ab-column-footer {
          padding: var(--ab-spacing-sm) var(--ab-spacing-md);
          border-top: 1px solid var(--ab-gray-200);
        }

        .ab-add-issue-btn {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-xs);
          width: 100%;
          padding: var(--ab-spacing-xs) var(--ab-spacing-sm);
          background: transparent;
          border: 1px dashed var(--ab-gray-300);
          border-radius: var(--ab-radius-md);
          color: var(--ab-gray-500);
          font-size: var(--ab-font-size-sm);
          cursor: pointer;
          transition: all var(--ab-transition-fast);
        }

        .ab-add-issue-btn:hover {
          background: var(--ab-white);
          border-color: var(--ab-primary-400);
          color: var(--ab-primary-600);
        }
      `}</style>
    </div>
  );
}