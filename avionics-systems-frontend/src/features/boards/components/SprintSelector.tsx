import React from 'react';

interface SprintSelectorProps {
  activeSprintId: string | null;
  sprints: { id: string; name: string; state: string }[];
  onSprintChange: (sprintId: string | null) => void;
}

export default function SprintSelector({ activeSprintId, sprints, onSprintChange }: SprintSelectorProps) {
  return (
    <div className="ab-sprint-selector">
      <select
        value={activeSprintId || ''}
        onChange={(e) => onSprintChange(e.target.value || null)}
        className="ab-sprint-select"
      >
        <option value="">No Sprint</option>
        {sprints.map((sprint) => (
          <option key={sprint.id} value={sprint.id}>
            {sprint.name} ({sprint.state})
          </option>
        ))}
      </select>

      <style>{`
        .ab-sprint-selector {
          display: inline-flex;
        }

        .ab-sprint-select {
          padding: var(--ab-spacing-xs) var(--ab-spacing-sm);
          border: 1px solid var(--ab-gray-300);
          border-radius: var(--ab-radius-sm);
          font-size: var(--ab-font-size-sm);
          background: var(--ab-white);
          cursor: pointer;
        }

        .ab-sprint-select:focus {
          outline: none;
          border-color: var(--ab-primary-500);
        }
      `}</style>
    </div>
  );
}