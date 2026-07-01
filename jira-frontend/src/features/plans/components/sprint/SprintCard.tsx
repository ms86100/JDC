import React, { useState } from 'react';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { SprintIssueResponse } from '../../hooks/useSprint';

interface SprintCardProps {
  issue: SprintIssueResponse;
  sprintId: string;
  isSelected?: boolean;
  isDragging?: boolean;
  multiSelectMode?: boolean;
  isChecked?: boolean;
  onSelect?: () => void;
  onComplete?: () => void;
  onRemove?: () => void;
  onClick?: () => void;
  onToggleCheck?: () => void;
  onFlag?: (flagged: boolean, reason: string | null) => void;
  onMoveToBacklog?: () => void;
  onEstimationChange?: (points: number) => void;
  storyPoints?: number | null;
}

export default function SprintCard({
  issue,
  sprintId,
  isSelected = false,
  isDragging = false,
  multiSelectMode = false,
  isChecked = false,
  onSelect,
  onComplete,
  onRemove,
  onClick,
  onToggleCheck,
  onFlag,
  onMoveToBacklog,
  onEstimationChange,
  storyPoints,
}: SprintCardProps) {
  const [showActions, setShowActions] = useState(false);
  const [editingPoints, setEditingPoints] = useState(false);
  const [pointsValue, setPointsValue] = useState(String(storyPoints || ''));
  const [showFlagInput, setShowFlagInput] = useState(false);
  const [flagReason, setFlagReason] = useState('');

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
  const isFlagged = issue.flagged;

  const handlePointsSave = () => {
    const val = parseInt(pointsValue);
    if (!isNaN(val) && onEstimationChange) {
      onEstimationChange(val);
    }
    setEditingPoints(false);
  };

  const handleFlagSubmit = () => {
    onFlag?.(true, flagReason || null);
    setShowFlagInput(false);
    setFlagReason('');
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`ab-issue-card ${isCompleted ? 'ab-completed' : ''} ${isSelected ? 'ab-selected' : ''} ${isDragging ? 'ab-dragging' : ''} ${isFlagged ? 'ab-flagged' : ''}`}
      onMouseEnter={() => setShowActions(true)}
      onMouseLeave={() => { setShowActions(false); setShowFlagInput(false); }}
    >
      {/* Multi-select checkbox (Gap 4) */}
      {multiSelectMode && (
        <div style={{ marginRight: '4px' }}>
          <input
            type="checkbox"
            checked={isChecked}
            onChange={() => onToggleCheck?.()}
            onClick={(e) => e.stopPropagation()}
            style={{ cursor: 'pointer', width: '14px', height: '14px' }}
          />
        </div>
      )}

      {/* Flag indicator (Gap 19) */}
      {isFlagged && (
        <div
          className="ab-flag-indicator"
          title={issue.flagReason || 'Flagged'}
          style={{ position: 'absolute', top: '4px', right: '4px', color: '#ef4444', fontSize: '14px', cursor: 'pointer' }}
          onClick={(e) => { e.stopPropagation(); onFlag?.(false, null); }}
        >
          ⚑
        </div>
      )}

      {/* Drag Handle */}
      <div className="ab-card-drag-handle" {...attributes} {...listeners}>
        ⋮⋮
      </div>

      {/* Card Content */}
      <div className="ab-card-content" onClick={onClick}>
        <div className="ab-card-issue-key">{issue.issueId?.slice(0, 12) || 'No Key'}</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <span className={`ab-status-badge ${issue.completionStatus?.toLowerCase()}`}>
            {issue.completionStatus || 'UNCOMPLETED'}
          </span>
          {/* Story points badge (Gap 17) */}
          {editingPoints ? (
            <input
              type="number"
              value={pointsValue}
              onChange={(e) => setPointsValue(e.target.value)}
              onBlur={handlePointsSave}
              onKeyDown={(e) => e.key === 'Enter' && handlePointsSave()}
              autoFocus
              onClick={(e) => e.stopPropagation()}
              style={{ width: '40px', padding: '1px 4px', fontSize: '0.75rem', border: '1px solid #3b82f6', borderRadius: '4px', textAlign: 'center' }}
            />
          ) : (
            <span
              onClick={(e) => { e.stopPropagation(); setEditingPoints(true); }}
              title="Story Points — click to edit"
              style={{
                background: 'var(--ab-gray-100, #f3f4f6)',
                padding: '1px 6px',
                borderRadius: '10px',
                fontSize: '0.7rem',
                fontWeight: 600,
                color: 'var(--ab-gray-600, #4b5563)',
                cursor: 'pointer',
                minWidth: '20px',
                textAlign: 'center',
              }}
            >
              {storyPoints != null ? storyPoints : '—'}
            </span>
          )}
        </div>
      </div>

      {/* Action Buttons */}
      {showActions && !isDragging && (
        <div className="ab-card-actions">
          {!isCompleted && onComplete && (
            <button className="ab-btn ab-btn-sm ab-btn-success" onClick={(e) => { e.stopPropagation(); onComplete(); }} title="Complete">✓</button>
          )}
          {onRemove && (
            <button className="ab-btn ab-btn-sm ab-btn-danger" onClick={(e) => { e.stopPropagation(); onRemove(); }} title="Remove from sprint">×</button>
          )}
          {/* Flag button (Gap 19) */}
          {!isFlagged && onFlag && (
            <button
              className="ab-btn ab-btn-sm ab-btn-secondary"
              onClick={(e) => { e.stopPropagation(); setShowFlagInput(true); }}
              title="Flag issue"
              style={{ color: '#ef4444' }}
            >
              ⚑
            </button>
          )}
          {/* Move to backlog (Gap 5) */}
          {onMoveToBacklog && (
            <button
              className="ab-btn ab-btn-sm ab-btn-secondary"
              onClick={(e) => { e.stopPropagation(); onMoveToBacklog(); }}
              title="Move to backlog"
            >
              ↩
            </button>
          )}
          <button className="ab-btn ab-btn-sm ab-btn-secondary" onClick={(e) => { e.stopPropagation(); onSelect?.(); }} title="More actions">...</button>
        </div>
      )}

      {/* Flag reason input popup (Gap 19) */}
      {showFlagInput && (
        <div style={{
          position: 'absolute', bottom: '100%', left: 0, right: 0,
          background: 'white', border: '1px solid #d1d5db', borderRadius: '6px',
          padding: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.15)', zIndex: 100,
        }} onClick={(e) => e.stopPropagation()}>
          <input
            placeholder="Reason for flagging..."
            value={flagReason}
            onChange={(e) => setFlagReason(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleFlagSubmit()}
            autoFocus
            style={{ width: '100%', padding: '4px 8px', fontSize: '0.75rem', border: '1px solid #d1d5db', borderRadius: '4px', marginBottom: '4px' }}
          />
          <div style={{ display: 'flex', gap: '4px', justifyContent: 'flex-end' }}>
            <button className="ab-btn ab-btn-sm ab-btn-secondary" onClick={() => setShowFlagInput(false)} style={{ padding: '2px 8px', fontSize: '0.7rem' }}>Cancel</button>
            <button className="ab-btn ab-btn-sm ab-btn-primary" onClick={handleFlagSubmit} style={{ padding: '2px 8px', fontSize: '0.7rem' }}>Flag</button>
          </div>
        </div>
      )}

      {isCompleted && <div className="ab-completed-indicator">✓</div>}
    </div>
  );
}
