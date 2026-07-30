import React from 'react';
import type { BoardIssue } from '../../../api/boardApi';

interface BulkActionsBarProps {
  selectedIssues: BoardIssue[];
  onClearSelection: () => void;
  onAction: (action: string) => void;
}

export default function BulkActionsBar({
  selectedIssues,
  onClearSelection,
  onAction,
}: BulkActionsBarProps) {
  if (selectedIssues.length === 0) return null;

  return (
    <div className="sa-bulk-actions-bar">
      <div className="sa-bulk-actions-info">
        <span className="sa-bulk-count">{selectedIssues.length}</span>
        <span className="sa-bulk-label">issue{selectedIssues.length !== 1 ? 's' : ''} selected</span>
      </div>

      <div className="sa-bulk-actions-buttons">
        <button
          type="button"
          className="sa-bulk-action-btn"
          onClick={() => onAction('assign')}
          title="Assign selected issues"
        >
          Assign
        </button>

        <button
          type="button"
          className="sa-bulk-action-btn"
          onClick={() => onAction('move')}
          title="Move selected issues"
        >
          Move
        </button>

        <button
          type="button"
          className="sa-bulk-action-btn"
          onClick={() => onAction('labels')}
          title="Edit labels"
        >
          Labels
        </button>

        <button
          type="button"
          className="sa-bulk-action-btn"
          onClick={() => onAction('priority')}
          title="Change priority"
        >
          Priority
        </button>

        <button
          type="button"
          className="sa-bulk-action-btn"
          onClick={() => onAction('archive')}
          title="Archive issues"
        >
          Archive
        </button>

        <div className="sa-bulk-actions-divider" />

        <button
          type="button"
          className="sa-bulk-action-btn is-danger"
          onClick={() => onAction('delete')}
          title="Delete issues"
        >
          Delete
        </button>
      </div>

      <button
        type="button"
        className="sa-bulk-clear-btn"
        onClick={onClearSelection}
        aria-label="Clear selection"
      >
        ×
      </button>
    </div>
  );
}
