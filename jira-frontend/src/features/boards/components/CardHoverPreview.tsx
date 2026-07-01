import React, { useEffect, useRef, useState } from 'react';
import type { BoardIssue } from '../../../api/boardApi';
import { formatDate, getPriorityIcon } from '../../../utils/format';

interface Props {
  issue: BoardIssue | null;
  anchorRef: React.RefObject<HTMLElement | null>;
  onClose: () => void;
  onOpenIssue: (issueId: string) => void;
}

export default function CardHoverPreview({ issue, anchorRef, onClose, onOpenIssue }: Props) {
  const [visible, setVisible] = useState(false);
  const [position, setPosition] = useState({ top: 0, left: 0 });
  const previewRef = useRef<HTMLDivElement>(null);
  const hideTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (issue && anchorRef.current) {
      hideTimeoutRef.current = setTimeout(() => {
        const rect = anchorRef.current!.getBoundingClientRect();
        const previewWidth = 360;
        const previewHeight = 280;

        let left = rect.right + 12;
        let top = rect.top;

        if (left + previewWidth > window.innerWidth - 20) {
          left = rect.left - previewWidth - 12;
        }
        if (top + previewHeight > window.innerHeight - 20) {
          top = window.innerHeight - previewHeight - 20;
        }
        if (top < 20) {
          top = 20;
        }

        setPosition({ top, left });
        setVisible(true);
      }, 400);
    } else {
      setVisible(false);
    }

    return () => {
      if (hideTimeoutRef.current) {
        clearTimeout(hideTimeoutRef.current);
      }
    };
  }, [issue, anchorRef]);

  if (!issue || !visible) return null;

  const priorityIcon = getPriorityIcon(issue.priority);
  const statusColor = issue.statusCategory === 'done' ? '#00875a' :
                      issue.statusCategory === 'in_progress' ? 'var(--sa-brand-500)' : 'var(--sa-n600)';

  return (
    <div
      ref={previewRef}
      className="sa-card-preview"
      style={{ top: position.top, left: position.left }}
      role="dialog"
      aria-label={`Issue preview: ${issue.key}`}
      onMouseEnter={() => {
        if (hideTimeoutRef.current) clearTimeout(hideTimeoutRef.current);
      }}
      onMouseLeave={onClose}
    >
      <div className="sa-card-preview-header">
        <span className="sa-card-preview-key">{issue.key}</span>
        <span className="sa-card-preview-type">{issue.typeName || 'Issue'}</span>
        {priorityIcon && (
          <span className="sa-card-preview-priority" title={issue.priority}>
            {priorityIcon}
          </span>
        )}
      </div>

      <h3 className="sa-card-preview-title">{issue.summary}</h3>

      <div className="sa-card-preview-meta">
        <div className="sa-card-preview-row">
          <span className="sa-card-preview-label">Status</span>
          <span
            className="sa-card-preview-status"
            style={{ color: statusColor }}
          >
            {issue.status}
          </span>
        </div>

        {issue.assigneeName && (
          <div className="sa-card-preview-row">
            <span className="sa-card-preview-label">Assignee</span>
            <span className="sa-card-preview-value">
              {issue.assigneeAvatar ? (
                <img
                  src={issue.assigneeAvatar}
                  alt=""
                  className="sa-card-preview-avatar"
                />
              ) : null}
              {issue.assigneeName}
            </span>
          </div>
        )}

        {issue.storyPoints != null && (
          <div className="sa-card-preview-row">
            <span className="sa-card-preview-label">Story Points</span>
            <span className="sa-card-preview-value">{issue.storyPoints}</span>
          </div>
        )}

        {issue.dueDate && (
          <div className="sa-card-preview-row">
            <span className="sa-card-preview-label">Due Date</span>
            <span className={`sa-card-preview-value${new Date(issue.dueDate) < new Date() ? ' is-overdue' : ''}`}>
              {formatDate(issue.dueDate)}
            </span>
          </div>
        )}

        {issue.epicName && (
          <div className="sa-card-preview-row">
            <span className="sa-card-preview-label">Epic</span>
            <span
              className="sa-card-preview-epic"
              style={{ borderLeftColor: issue.epicColor || 'var(--sa-brand-500)' }}
            >
              {issue.epicName}
            </span>
          </div>
        )}

        {issue.labels && issue.labels.length > 0 && (
          <div className="sa-card-preview-row">
            <span className="sa-card-preview-label">Labels</span>
            <span className="sa-card-preview-labels">
              {issue.labels.slice(0, 3).map((label) => (
                <span key={label} className="sa-card-preview-label-tag">{label}</span>
              ))}
              {issue.labels.length > 3 && (
                <span className="sa-card-preview-label-more">+{issue.labels.length - 3}</span>
              )}
            </span>
          </div>
        )}
      </div>

      {issue.description && (
        <div className="sa-card-preview-description">
          {issue.description.slice(0, 200)}
          {issue.description.length > 200 && '…'}
        </div>
      )}

      <div className="sa-card-preview-footer">
        <button
          type="button"
          className="sa-card-preview-open"
          onClick={() => onOpenIssue(issue.id)}
        >
          Open issue
        </button>
      </div>
    </div>
  );
}
