import React from 'react';
import { SprintResponse } from '../../hooks/useSprint';

interface SprintHeaderProps {
  sprint: SprintResponse;
  onStart?: () => void;
  onClose?: () => void;
}

export default function SprintHeader({ sprint, onStart, onClose }: SprintHeaderProps) {
  const getStateColor = (state: string) => {
    switch (state) {
      case 'ACTIVE': return 'ab-state-active';
      case 'FUTURE': return 'ab-state-future';
      case 'CLOSED': return 'ab-state-closed';
      case 'ABANDONED': return 'ab-state-abandoned';
      default: return '';
    }
  };

  const formatDate = (dateStr: string | null) => {
    if (!dateStr) return null;
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  };

  return (
    <div className="ab-sprint-header">
      <div className="ab-sprint-info">
        <h2 className="ab-sprint-name">{sprint.name}</h2>
        <span className={`ab-sprint-state ${getStateColor(sprint.state)}`}>
          {sprint.state}
        </span>
      </div>

      {sprint.goal && (
        <p className="ab-sprint-goal">{sprint.goal}</p>
      )}

      <div className="ab-sprint-dates">
        {sprint.startDate && (
          <span className="ab-date-item">
            <span className="ab-date-label">Started:</span>
            <span className="ab-date-value">{formatDate(sprint.startDate)}</span>
          </span>
        )}
        {sprint.endDate && (
          <span className="ab-date-item">
            <span className="ab-date-label">Ends:</span>
            <span className="ab-date-value">{formatDate(sprint.endDate)}</span>
          </span>
        )}
      </div>

      {/* Sprint Stats */}
      <div className="ab-sprint-stats">
        <div className="ab-stat">
          <span className="ab-stat-value">{sprint.totalIssues}</span>
          <span className="ab-stat-label">Total</span>
        </div>
        <div className="ab-stat">
          <span className="ab-stat-value">{sprint.completedIssues}</span>
          <span className="ab-stat-label">Completed</span>
        </div>
        <div className="ab-stat">
          <span className="ab-stat-value">{sprint.velocity || 0}</span>
          <span className="ab-stat-label">Velocity</span>
        </div>
      </div>

      {/* Actions */}
      <div className="ab-sprint-actions">
        {sprint.state === 'FUTURE' && onStart && (
          <button className="ab-btn ab-btn-primary" onClick={onStart}>
            Start Sprint
          </button>
        )}
        {sprint.state === 'ACTIVE' && onClose && (
          <button className="ab-btn ab-btn-success" onClick={onClose}>
            Close Sprint
          </button>
        )}
      </div>
    </div>
  );
}
