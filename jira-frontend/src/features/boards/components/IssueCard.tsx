import React from 'react';
import { BoardIssue } from '../../../api/boardApi';
import JiraDcIssueCard from './JiraDcIssueCard';

type CardLayout = 'FULL' | 'COMPACT' | 'MINI';

export interface CardCustomFieldRow {
  displayName: string;
  value: unknown;
}

interface IssueCardProps {
  issue: BoardIssue;
  layout: CardLayout;
  customFields?: CardCustomFieldRow[];
  color?: string;
  isDragging: boolean;
  onDragStart: (e: React.DragEvent) => void;
  onDragEnd: () => void;
  onClick: () => void;
  onMouseEnter?: () => void;
  onMouseLeave?: () => void;
  onContextMenu?: (e: React.MouseEvent) => void;
  rank?: number;
  jiraDcLayout?: boolean;
}

function formatCfValue(value: unknown): string {
  if (value == null) return '';
  if (Array.isArray(value)) return value.join(', ');
  return String(value);
}

export default function IssueCard({
  issue,
  layout,
  customFields = [],
  color,
  isDragging,
  onDragStart,
  onDragEnd,
  onClick,
  onMouseEnter,
  onMouseLeave,
  onContextMenu,
  rank,
  jiraDcLayout = false,
}: IssueCardProps) {
  const getPriorityIcon = (priority: string | undefined) => {
    switch (priority?.toLowerCase()) {
      case 'critical':
      case 'highest':
        return '🔴';
      case 'high':
        return '🟠';
      case 'medium':
        return '🟡';
      case 'low':
      case 'lowest':
        return '🟢';
      default:
        return '⚪';
    }
  };

  const getTypeIcon = (type: string | undefined) => {
    switch (type?.toLowerCase()) {
      case 'bug':
        return '🐛';
      case 'story':
        return '📖';
      case 'task':
        return '✓';
      case 'epic':
        return '⚡';
      case 'sub-task':
        return '↳';
      default:
        return '📋';
    }
  };

  const getPriorityColor = (priority: string | undefined) => {
    switch (priority?.toLowerCase()) {
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
        return '#6c757d';
    }
  };

  const formatDueDate = (dateStr: string | undefined) => {
    if (!dateStr) return null;
    const date = new Date(dateStr);
    const now = new Date();
    const diff = Math.ceil((date.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));

    if (diff < 0) return { text: 'Overdue', className: 'overdue' };
    if (diff === 0) return { text: 'Today', className: 'today' };
    if (diff <= 3) return { text: `${diff}d`, className: 'soon' };
    return { text: date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }), className: '' };
  };

  const dueDate = formatDueDate(issue.dueDate);
  const borderColor = color || (issue.priority && getPriorityColor(issue.priority));

  if (layout === 'MINI') {
    return (
      <div
        className={`ab-issue-card ab-card-mini ${isDragging ? 'ab-dragging' : ''}`}
        draggable
        onDragStart={onDragStart}
        onDragEnd={onDragEnd}
        onClick={onClick}
        onMouseEnter={onMouseEnter}
        onMouseLeave={onMouseLeave}
        onContextMenu={onContextMenu}
        style={borderColor ? { borderLeftColor: borderColor } : undefined}
      >
        <span className="ab-card-key-mini">{issue.issueKey}</span>
        <span className="ab-card-type-mini">{getTypeIcon(issue.issueType)}</span>
      </div>
    );
  }

  if (layout === 'COMPACT') {
    return (
      <div
        className={`ab-issue-card ab-card-compact ${isDragging ? 'ab-dragging' : ''}`}
        draggable
        onDragStart={onDragStart}
        onDragEnd={onDragEnd}
        onClick={onClick}
        onMouseEnter={onMouseEnter}
        onMouseLeave={onMouseLeave}
        onContextMenu={onContextMenu}
        style={borderColor ? { borderLeftColor: borderColor } : undefined}
      >
        <div className="ab-card-compact-row">
          <span className="ab-card-type-icon">{getTypeIcon(issue.issueType)}</span>
          <span className="ab-card-key">{issue.issueKey}</span>
          <span className="ab-card-priority-icon">{getPriorityIcon(issue.priority)}</span>
        </div>
        <div className="ab-card-compact-title">{issue.title}</div>
        {issue.assigneeId && (
          <div className="ab-card-compact-footer">
            <span className="ab-avatar-xs">
              {(issue.assigneeName || issue.assigneeId || 'U').charAt(0).toUpperCase()}
            </span>
            {issue.storyPoints && (
              <span className="ab-story-points">{issue.storyPoints}</span>
            )}
          </div>
        )}
      </div>
    );
  }

  // Full layout (default)
  if (jiraDcLayout) {
    return (
      <JiraDcIssueCard
        issue={issue}
        isDragging={isDragging}
        onDragStart={onDragStart}
        onDragEnd={onDragEnd}
        onClick={onClick}
        onMouseEnter={onMouseEnter}
        onMouseLeave={onMouseLeave}
        onContextMenu={onContextMenu}
      />
    );
  }

  return (
    <div
      className={`ab-issue-card ab-card-full ${isDragging ? 'ab-dragging' : ''}`}
      draggable
      onDragStart={onDragStart}
      onDragEnd={onDragEnd}
      onClick={onClick}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
      onContextMenu={onContextMenu}
    >
      {/* Card Header */}
      <div className="ab-card-header">
        <span className="ab-card-type" title={issue.issueType}>
          {getTypeIcon(issue.issueType)}
        </span>
        <span className="ab-card-key">{issue.issueKey}</span>
        <span className="ab-card-priority" title={issue.priority}>
          {getPriorityIcon(issue.priority)}
        </span>
        {rank && <span className="ab-card-rank">#{rank}</span>}
      </div>

      {/* Epic Badge */}
      {issue.epicName && (
        <div className="ab-epic-badge" style={{ backgroundColor: issue.epicColor || '#6f42c1' }}>
          <span className="ab-epic-icon">⚡</span>
          <span className="ab-epic-name">{issue.epicName}</span>
        </div>
      )}

      {/* Card Title */}
      <div className="ab-card-title">{issue.title || issue.issueKey}</div>

      {/* Labels */}
      {issue.labels && issue.labels.length > 0 && (
        <div className="ab-card-labels">
          {issue.labels.slice(0, 3).map((label, idx) => (
            <span key={idx} className="ab-label-tag">{label}</span>
          ))}
          {issue.labels.length > 3 && (
            <span className="ab-label-more">+{issue.labels.length - 3}</span>
          )}
        </div>
      )}

      {customFields.length > 0 && (
        <div className="ab-card-custom-fields">
          {customFields.map((cf) => (
            <div key={cf.displayName} className="ab-card-cf-row">
              <span className="ab-card-cf-label">{cf.displayName}</span>
              <span className="ab-card-cf-value">{formatCfValue(cf.value)}</span>
            </div>
          ))}
        </div>
      )}

      {/* Card Footer */}
      <div className="ab-card-footer">
        <div className="ab-card-footer-left">
          <span className="ab-card-type-icon" title={issue.issueType}>
            {getTypeIcon(issue.issueType)}
          </span>
          <span
            className="ab-card-priority-dot"
            style={{ backgroundColor: getPriorityColor(issue.priority) || '#dfe1e6' }}
            title={issue.priority}
          />
          <span className="ab-card-key">{issue.issueKey}</span>
        </div>
        <div className="ab-card-footer-right">
          {issue.storyPoints != null && issue.storyPoints > 0 && (
            <span className="ab-story-points-badge" title="Story points">
              {issue.storyPoints}
            </span>
          )}
          {dueDate && (
            <span className={`ab-due-date ${dueDate.className}`} title={`Due: ${issue.dueDate}`}>
              {dueDate.text}
            </span>
          )}
          {issue.assigneeId ? (
            <span className="ab-avatar-xs" title={issue.assigneeName || issue.assigneeId}>
              {(issue.assigneeName || issue.assigneeId || 'U').charAt(0).toUpperCase()}
            </span>
          ) : (
            <span className="ab-avatar-unassigned" title="Unassigned">?</span>
          )}
        </div>
      </div>

      {/* Metadata */}
      <div className="ab-card-metadata">
        {issue.sprintName && (
          <span className="ab-sprint-tag" title="Sprint">
            🏃 {issue.sprintName}
          </span>
        )}
      </div>

      <style>{`
        .ab-issue-card {
          background: var(--ab-white);
          border: 1px solid var(--ab-gray-200);
          border-left: 3px solid var(--ab-gray-300);
          border-radius: var(--ab-radius-md);
          padding: var(--ab-spacing-sm);
          cursor: grab;
          transition: all var(--ab-transition-fast);
        }

        .ab-issue-card:hover {
          box-shadow: var(--ab-shadow-md);
          border-color: var(--ab-gray-300);
          transform: translateY(-1px);
        }

        .ab-issue-card.ab-dragging {
          opacity: 0.5;
          cursor: grabbing;
          box-shadow: var(--ab-shadow-lg);
        }

        /* MINI Layout */
        .ab-card-mini {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-xs);
          padding: var(--ab-spacing-xs);
          font-size: var(--ab-font-size-xs);
        }

        .ab-card-key-mini {
          font-family: var(--ab-font-mono);
          color: var(--ab-gray-500);
        }

        .ab-card-type-mini {
          font-size: 10px;
        }

        /* COMPACT Layout */
        .ab-card-compact {
          padding: var(--ab-spacing-xs) var(--ab-spacing-sm);
        }

        .ab-card-compact-row {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-xs);
          margin-bottom: var(--ab-spacing-xs);
        }

        .ab-card-compact-title {
          font-size: var(--ab-font-size-sm);
          font-weight: 500;
          color: var(--ab-gray-800);
          line-height: 1.3;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
          margin-bottom: var(--ab-spacing-xs);
        }

        .ab-card-compact-footer {
          display: flex;
          align-items: center;
          justify-content: space-between;
        }

        /* FULL Layout */
        .ab-card-header {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-xs);
          margin-bottom: var(--ab-spacing-xs);
        }

        .ab-card-type {
          font-size: var(--ab-font-size-sm);
        }

        .ab-card-key {
          font-family: var(--ab-font-mono);
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-500);
          flex: 1;
        }

        .ab-card-priority {
          font-size: var(--ab-font-size-xs);
        }

        .ab-card-rank {
          font-size: 10px;
          color: var(--ab-gray-400);
          background: var(--ab-gray-100);
          padding: 1px 4px;
          border-radius: var(--ab-radius-sm);
        }

        .ab-epic-badge {
          display: inline-flex;
          align-items: center;
          gap: var(--ab-spacing-xs);
          padding: 2px var(--ab-spacing-xs);
          border-radius: var(--ab-radius-sm);
          font-size: 10px;
          color: var(--ab-white);
          margin-bottom: var(--ab-spacing-xs);
        }

        .ab-epic-icon {
          font-size: 10px;
        }

        .ab-epic-name {
          max-width: 120px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .ab-card-title {
          font-size: var(--ab-font-size-sm);
          font-weight: 500;
          color: var(--ab-gray-800);
          line-height: 1.4;
          margin-bottom: var(--ab-spacing-xs);
          display: -webkit-box;
          -webkit-line-clamp: 3;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }

        .ab-card-labels {
          display: flex;
          flex-wrap: wrap;
          gap: var(--ab-spacing-xs);
          margin-bottom: var(--ab-spacing-xs);
        }

        .ab-label-tag {
          font-size: 10px;
          padding: 1px 4px;
          background: var(--ab-gray-100);
          color: var(--ab-gray-600);
          border-radius: var(--ab-radius-sm);
        }

        .ab-label-more {
          font-size: 10px;
          color: var(--ab-gray-400);
        }

        .ab-card-footer {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-top: var(--ab-spacing-xs);
          padding-top: var(--ab-spacing-xs);
          border-top: 1px solid var(--ab-gray-100);
        }

        .ab-card-footer-left {
          display: flex;
          align-items: center;
        }

        .ab-card-assignee {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-xs);
        }

        .ab-assignee-name {
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-600);
          max-width: 80px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .ab-unassigned {
          font-size: var(--ab-font-size-xs);
          color: var(--ab-gray-400);
          font-style: italic;
        }

        .ab-card-footer-right {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-xs);
        }

        .ab-story-points-badge {
          font-size: 10px;
          font-weight: 600;
          color: var(--ab-gray-600);
          background: var(--ab-gray-100);
          padding: 2px 4px;
          border-radius: var(--ab-radius-sm);
        }

        .ab-due-date {
          font-size: 10px;
          padding: 2px 4px;
          border-radius: var(--ab-radius-sm);
          background: var(--ab-gray-100);
          color: var(--ab-gray-600);
        }

        .ab-due-date.overdue {
          background: var(--ab-danger-100);
          color: var(--ab-danger-700);
        }

        .ab-due-date.today {
          background: var(--ab-warning-100);
          color: var(--ab-warning-700);
        }

        .ab-due-date.soon {
          background: var(--ab-accent-100);
          color: var(--ab-accent-700);
        }

        .ab-card-custom-fields {
          margin: var(--ab-spacing-xs) 0;
          padding: var(--ab-spacing-xs) 0;
          border-top: 1px dashed var(--ab-gray-200);
          font-size: 10px;
        }

        .ab-card-cf-row {
          display: flex;
          justify-content: space-between;
          gap: 6px;
          line-height: 1.3;
          margin-bottom: 2px;
        }

        .ab-card-cf-label {
          color: var(--ab-gray-500);
          flex-shrink: 0;
          max-width: 45%;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .ab-card-cf-value {
          color: var(--ab-gray-800);
          text-align: right;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .ab-card-metadata {
          display: flex;
          gap: var(--ab-spacing-xs);
          margin-top: var(--ab-spacing-xs);
        }

        .ab-sprint-tag,
        .ab-rank-tag {
          font-size: 10px;
          color: var(--ab-gray-500);
          display: flex;
          align-items: center;
          gap: 2px;
        }

        .ab-avatar-xs {
          width: 18px;
          height: 18px;
          border-radius: 50%;
          background: var(--ab-primary-500);
          color: var(--ab-white);
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 10px;
          font-weight: 600;
        }
      `}</style>
    </div>
  );
}