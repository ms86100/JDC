import React, { useState } from 'react';
import { SprintResponse, usePartialUpdateSprint, useSprintProperties, useSetSprintProperty, useDeleteSprintProperty } from '../../hooks/useSprint';

interface SprintHeaderProps {
  sprint: SprintResponse;
  onStart?: () => void;
  onClose?: () => void;
  onReopen?: () => void;
}

export default function SprintHeader({ sprint, onStart, onClose, onReopen }: SprintHeaderProps) {
  const [editingName, setEditingName] = useState(false);
  const [editingGoal, setEditingGoal] = useState(false);
  const [nameValue, setNameValue] = useState(sprint.name);
  const [goalValue, setGoalValue] = useState(sprint.goal || '');
  const [showProperties, setShowProperties] = useState(false);
  const [newPropKey, setNewPropKey] = useState('');
  const [newPropValue, setNewPropValue] = useState('');

  const partialUpdate = usePartialUpdateSprint();
  const { data: properties } = useSprintProperties(sprint.id);
  const setProperty = useSetSprintProperty();
  const deleteProperty = useDeleteSprintProperty();

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
    return new Date(dateStr).toLocaleDateString('en-US', { day: 'numeric', month: 'short', year: 'numeric' });
  };

  const handleNameSave = () => {
    if (nameValue && nameValue !== sprint.name) {
      partialUpdate.mutate({ sprintId: sprint.id, data: { name: nameValue } });
    }
    setEditingName(false);
  };

  const handleGoalSave = () => {
    if (goalValue !== (sprint.goal || '')) {
      partialUpdate.mutate({ sprintId: sprint.id, data: { goal: goalValue } });
    }
    setEditingGoal(false);
  };

  const handleAddProperty = () => {
    if (newPropKey && newPropValue) {
      setProperty.mutate({ sprintId: sprint.id, key: newPropKey, value: newPropValue, userId: 'current' });
      setNewPropKey('');
      setNewPropValue('');
    }
  };

  return (
    <div className="ab-sprint-header">
      <div className="ab-sprint-info">
        {editingName ? (
          <input
            className="ab-inline-edit"
            value={nameValue}
            onChange={(e) => setNameValue(e.target.value)}
            onBlur={handleNameSave}
            onKeyDown={(e) => e.key === 'Enter' && handleNameSave()}
            autoFocus
            style={{ fontSize: '1.25rem', fontWeight: 600, border: '1px solid var(--ab-primary-500, #3b82f6)', borderRadius: '4px', padding: '2px 8px' }}
          />
        ) : (
          <h2 className="ab-sprint-name" onClick={() => setEditingName(true)} style={{ cursor: 'pointer' }} title="Click to edit">
            {sprint.name}
          </h2>
        )}
        <span className={`ab-sprint-state ${getStateColor(sprint.state)}`}>{sprint.state}</span>
      </div>

      {editingGoal ? (
        <textarea
          className="ab-inline-edit"
          value={goalValue}
          onChange={(e) => setGoalValue(e.target.value)}
          onBlur={handleGoalSave}
          rows={2}
          autoFocus
          style={{ width: '100%', border: '1px solid var(--ab-primary-500, #3b82f6)', borderRadius: '4px', padding: '4px 8px', resize: 'vertical' }}
        />
      ) : (
        <p className="ab-sprint-goal" onClick={() => setEditingGoal(true)} style={{ cursor: 'pointer' }} title="Click to edit goal">
          {sprint.goal || 'Click to add sprint goal...'}
        </p>
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
          <span className="ab-stat-value">{sprint.committedPoints || 0}</span>
          <span className="ab-stat-label">Committed</span>
        </div>
        <div className="ab-stat">
          <span className="ab-stat-value">{sprint.completedPoints || 0}</span>
          <span className="ab-stat-label">Pts Done</span>
        </div>
        <div className="ab-stat">
          <span className="ab-stat-value">{sprint.velocity || 0}</span>
          <span className="ab-stat-label">Velocity</span>
        </div>
      </div>

      <div className="ab-sprint-actions">
        {sprint.state === 'FUTURE' && onStart && (
          <button className="ab-btn ab-btn-primary" onClick={onStart}>Start Sprint</button>
        )}
        {sprint.state === 'ACTIVE' && onClose && (
          <button className="ab-btn ab-btn-success" onClick={onClose}>Close Sprint</button>
        )}
        {sprint.state === 'CLOSED' && onReopen && (
          <button className="ab-btn ab-btn-secondary" onClick={onReopen}>Reopen Sprint</button>
        )}
        <button
          className="ab-btn ab-btn-secondary"
          onClick={() => setShowProperties(!showProperties)}
          style={{ marginLeft: '8px' }}
        >
          {showProperties ? 'Hide Properties' : 'Properties'}
        </button>
      </div>

      {/* Sprint Properties Panel (Gap 3) */}
      {showProperties && (
        <div style={{ marginTop: '12px', padding: '12px', background: 'var(--ab-gray-50, #f9fafb)', borderRadius: '8px', border: '1px solid var(--ab-gray-200, #e5e7eb)' }}>
          <h4 style={{ margin: '0 0 8px', fontSize: '0.875rem', fontWeight: 600 }}>Sprint Properties</h4>
          {properties && properties.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
              {properties.map((prop) => (
                <div key={prop.key} style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.813rem' }}>
                  <span style={{ fontWeight: 500, minWidth: '100px' }}>{prop.key}:</span>
                  <span style={{ flex: 1, color: 'var(--ab-gray-600, #4b5563)' }}>{prop.value}</span>
                  <button
                    className="ab-btn ab-btn-sm ab-btn-danger"
                    onClick={() => deleteProperty.mutate({ sprintId: sprint.id, key: prop.key, userId: 'current' })}
                    style={{ padding: '2px 6px', fontSize: '0.75rem' }}
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <p style={{ margin: 0, fontSize: '0.813rem', color: 'var(--ab-gray-400, #9ca3af)' }}>No properties set</p>
          )}
          <div style={{ display: 'flex', gap: '4px', marginTop: '8px' }}>
            <input placeholder="Key" value={newPropKey} onChange={(e) => setNewPropKey(e.target.value)}
              style={{ flex: 1, padding: '4px 8px', fontSize: '0.813rem', border: '1px solid var(--ab-gray-300, #d1d5db)', borderRadius: '4px' }} />
            <input placeholder="Value" value={newPropValue} onChange={(e) => setNewPropValue(e.target.value)}
              style={{ flex: 1, padding: '4px 8px', fontSize: '0.813rem', border: '1px solid var(--ab-gray-300, #d1d5db)', borderRadius: '4px' }} />
            <button className="ab-btn ab-btn-sm ab-btn-primary" onClick={handleAddProperty} style={{ padding: '4px 12px', fontSize: '0.813rem' }}>Add</button>
          </div>
        </div>
      )}
    </div>
  );
}
