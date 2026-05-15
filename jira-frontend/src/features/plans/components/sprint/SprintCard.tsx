import React, { useState } from 'react';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { SprintIssueResponse } from '../../hooks/useSprint';

interface SprintCardProps {
  issue: SprintIssueResponse;
  sprintId: string;
  isSelected?: boolean;
  isDragging?: boolean;
  onSelect?: () => void;
  onComplete?: () => void;
  onRemove?: () => void;
  onClick?: () => void;
}

export default function SprintCard({
  issue,
  sprintId,
  isSelected = false,
  isDragging = false,
  onSelect,
  onComplete,
  onRemove,
  onClick,
}: SprintCardProps) {
  const [showActions, setShowActions] = useState(false);

  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging: isSortableDragging,
  } = useSortable({ id: issue.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isSortableDragging ? 0.5 : 1,
  };

  const isCompleted = issue.completionStatus === 'COMPLETED';

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`ab-issue-card ${isCompleted ? 'ab-completed' : ''} ${isSelected ? 'ab-selected' : ''} ${isDragging ? 'ab-dragging' : ''}`}
      onMouseEnter={() => setShowActions(true)}
      onMouseLeave={() => setShowActions(false)}
    >
      {/* Drag Handle */}
      <div className="ab-card-drag-handle" {...attributes} {...listeners}>
        ⋮⋮
      </div>

      {/* Card Content */}
      <div className="ab-card-content" onClick={onClick}>
        <div className="ab-card-issue-key">{issue.issueId?.slice(0, 12) || 'No Key'}</div>
        <div className="ab-card-status">
          <span className={`ab-status-badge ${issue.completionStatus?.toLowerCase()}`}>
            {issue.completionStatus || 'UNCOMPLETED'}
          </span>
        </div>
      </div>

      {/* Action Buttons - Appear on hover */}
      {showActions && !isDragging && (
        <div className="ab-card-actions">
          {!isCompleted && onComplete && (
            <button
              className="ab-btn ab-btn-sm ab-btn-success"
              onClick={(e) => { e.stopPropagation(); onComplete(); }}
              title="Complete"
            >
              ✓
            </button>
          )}
          {onRemove && (
            <button
              className="ab-btn ab-btn-sm ab-btn-danger"
              onClick={(e) => { e.stopPropagation(); onRemove(); }}
              title="Remove from sprint"
            >
              ×
            </button>
          )}
          <button
            className="ab-btn ab-btn-sm ab-btn-secondary"
            onClick={(e) => { e.stopPropagation(); onSelect?.(); }}
            title="More actions"
          >
            ...
          </button>
        </div>
      )}

      {/* Completion indicator */}
      {isCompleted && (
        <div className="ab-completed-indicator">✓</div>
      )}
    </div>
  );
}
