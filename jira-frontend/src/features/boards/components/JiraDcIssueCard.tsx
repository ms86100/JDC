import React from 'react';
import type { BoardIssue } from '../../../api/boardApi';
import IssueTypeIcon, { labelStyle } from './IssueTypeIcon';

interface JiraDcIssueCardProps {
  issue: BoardIssue;
  isDragging: boolean;
  onDragStart: (e: React.DragEvent) => void;
  onDragEnd: () => void;
  onClick: () => void;
  onMouseEnter?: () => void;
  onMouseLeave?: () => void;
  onContextMenu?: (e: React.MouseEvent) => void;
}

/** Jira Data Center card: summary → labels → (type + key | avatar). */
export default function JiraDcIssueCard({
  issue,
  isDragging,
  onDragStart,
  onDragEnd,
  onClick,
  onMouseEnter,
  onMouseLeave,
  onContextMenu,
}: JiraDcIssueCardProps) {
  const summary = issue.title?.trim() || issue.issueKey;

  return (
    <article
      className={`jdc-kanban-card${isDragging ? ' is-dragging' : ''}`}
      draggable
      onDragStart={onDragStart}
      onDragEnd={onDragEnd}
      onClick={onClick}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
      onContextMenu={onContextMenu}
    >
      <p className="jdc-kanban-card__summary">{summary}</p>

      {issue.epicName && (
        <span
          className="jdc-kanban-card__epic"
          style={{ backgroundColor: issue.epicColor || '#6554c0' }}
        >
          {issue.epicName}
        </span>
      )}

      {issue.labels && issue.labels.length > 0 && (
        <div className="jdc-kanban-card__labels">
          {issue.labels.slice(0, 4).map((label) => (
            <span key={label} className="jdc-kanban-card__label" style={labelStyle(label)}>
              {label}
            </span>
          ))}
        </div>
      )}

      <div className="jdc-kanban-card__footer">
        <div className="jdc-kanban-card__meta">
          <IssueTypeIcon type={issue.issueType} size={16} />
          <span className="jdc-kanban-card__key">{issue.issueKey}</span>
        </div>
        {issue.assigneeId ? (
          <span
            className="jdc-kanban-card__avatar"
            title={issue.assigneeName || issue.assigneeId}
          >
            {(issue.assigneeName || 'U').charAt(0).toUpperCase()}
          </span>
        ) : (
          <span className="jdc-kanban-card__avatar jdc-kanban-card__avatar--empty" title="Unassigned" />
        )}
      </div>
    </article>
  );
}
