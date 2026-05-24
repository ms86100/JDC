import React from 'react';

type CardLayout = 'FULL' | 'COMPACT' | 'MINI';
type ViewMode = 'board' | 'swimlane';

interface BoardHeaderProps {
  boardType: 'SCRUM' | 'KANBAN' | 'BADGE';
  boardName: string;
  cardLayout: CardLayout;
  onCardLayoutChange: (layout: CardLayout) => void;
  onOpenConfig: () => void;
  viewMode: ViewMode;
  onToggleView: () => void;
  activeSprintId: string | null;
  onSprintChange: (sprintId: string | null) => void;
  showVersionsPanel?: boolean;
  onToggleVersions?: () => void;
  velocityData?: { completed: number; committed: number };
}

export default function BoardHeader({
  boardType,
  boardName,
  cardLayout,
  onCardLayoutChange,
  onOpenConfig,
  viewMode,
  onToggleView,
  activeSprintId,
  onSprintChange,
  showVersionsPanel,
  onToggleVersions,
}: BoardHeaderProps) {
  return (
    <div className="ab-board-header">
      <div className="ab-board-header-left">
        <div className="ab-board-type-badge">
          {boardType === 'SCRUM' && <span className="ab-badge ab-badge-scrum">Scrum</span>}
          {boardType === 'KANBAN' && <span className="ab-badge ab-badge-kanban">Kanban</span>}
          {boardType === 'BADGE' && <span className="ab-badge ab-badge-badge">Badge</span>}
        </div>
        <h2 className="ab-board-title">{boardName}</h2>
        {boardType === 'SCRUM' && (
          <select
            value={activeSprintId || ''}
            onChange={(e) => onSprintChange(e.target.value || null)}
            className="ab-sprint-selector"
          >
            <option value="">No Sprint</option>
            <option value="active-sprint">Sprint 1 - Active</option>
            <option value="sprint-2">Sprint 2</option>
            <option value="sprint-3">Sprint 3</option>
          </select>
        )}
        {onToggleVersions && (
          <button
            onClick={onToggleVersions}
            className={`ab-btn ab-btn-ghost ${showVersionsPanel ? 'ab-btn-active' : ''}`}
            title="Toggle Versions Panel"
          >
            Versions
          </button>
        )}
      </div>

      <div className="ab-board-header-center">
        {/* View Toggle */}
        <div className="ab-view-toggle">
          <button
            className={`ab-toggle-btn ${viewMode === 'board' ? 'active' : ''}`}
            onClick={() => viewMode !== 'board' && onToggleView()}
            title="Board View"
          >
            <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
              <rect x="1" y="2" width="4" height="12" rx="1" />
              <rect x="6" y="2" width="4" height="12" rx="1" />
              <rect x="11" y="2" width="4" height="12" rx="1" />
            </svg>
          </button>
          <button
            className={`ab-toggle-btn ${viewMode === 'swimlane' ? 'active' : ''}`}
            onClick={() => viewMode !== 'swimlane' && onToggleView()}
            title="Swimlane View"
          >
            <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
              <rect x="0" y="1" width="16" height="4" rx="1" />
              <rect x="0" y="6" width="16" height="4" rx="1" />
              <rect x="0" y="11" width="16" height="4" rx="1" />
            </svg>
          </button>
        </div>
      </div>

      <div className="ab-board-header-right">
        {/* Card Layout */}
        <div className="ab-layout-selector">
          <label className="ab-layout-label">Layout:</label>
          <select
            value={cardLayout}
            onChange={(e) => onCardLayoutChange(e.target.value as CardLayout)}
            className="ab-select"
          >
            <option value="FULL">Full</option>
            <option value="COMPACT">Compact</option>
            <option value="MINI">Mini</option>
          </select>
        </div>

        {/* Board Configuration */}
        <button
          onClick={onOpenConfig}
          className="ab-btn ab-btn-secondary ab-btn-icon"
          title="Board Configuration"
        >
          <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M8 4.754a3.246 3.246 0 1 0 0 6.492 3.246 3.246 0 0 0 0-6.492zM5.754 8a2.246 2.246 0 1 1 4.492 0 2.246 2.246 0 0 1-4.492 0z"/>
            <path d="M9.796 1.343c-.527-1.79-3.065-1.79-3.592 0l-.094.319a.873.873 0 0 1-1.255.52l-.292-.16c-1.64-.892-3.433.902-2.54 2.541l.159.292a.873.873 0 0 1-.52 1.255l-.319.094c-1.79.527-1.79 3.065 0 3.592l.319.094a.873.873 0 0 1 .52 1.255l-.16.292c-.892 1.64.901 3.434 2.541 2.54l.292-.159a.873.873 0 0 1 1.255.52l.094.319c.527 1.79 3.065 1.79 3.592 0l.094-.319a.873.873 0 0 1 1.255-.52l.292.16c1.64.893 3.434-.902 2.54-2.541l-.159-.292a.873.873 0 0 1 .52-1.255l.319-.094c1.79-.527 1.79-3.065 0-3.592l-.319-.094a.873.873 0 0 1-.52-1.255l.16-.292c.893-1.64-.902-3.433-2.541-2.54l-.292.159a.873.873 0 0 1-1.255-.52l-.094-.319zm-2.633.283c.246-.835 1.428-.835 1.674 0l.094.319a1.873 1.873 0 0 0 2.693 1.115l.291-.16c.764-.415 1.6.42 1.184 1.185l-.159.292a1.873 1.873 0 0 0 1.116 2.692l.318.094c.835.246.835 1.428 0 1.674l-.319.094a1.873 1.873 0 0 0-1.115 2.693l.16.291c.415.764-.42 1.6-1.185 1.184l-.291-.159a1.873 1.873 0 0 0-2.693 1.116l-.094.318c-.246.835-1.428.835-1.674 0l-.094-.319a1.873 1.873 0 0 0-2.692-1.115l-.292.16c-.764.415-1.6-.42-1.184-1.185l.159-.291A1.873 1.873 0 0 0 1.945 8.93l-.319-.094c-.835-.246-.835-1.428 0-1.674l.319-.094A1.873 1.873 0 0 0 3.06 4.377l-.16-.292c-.415-.764.42-1.6 1.185-1.184l.292.159a1.873 1.873 0 0 0 2.692-1.115l.094-.319z"/>
          </svg>
          Configure
        </button>

        {/* Keyboard Shortcuts Help */}
        <button className="ab-btn ab-btn-ghost ab-btn-icon" title="Keyboard Shortcuts">
          <span className="ab-shortcut-hint">?</span>
        </button>
      </div>

      <style>{`
        .ab-board-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: var(--ab-spacing-md) var(--ab-spacing-lg);
          background: var(--ab-white);
          border-bottom: 1px solid var(--ab-gray-200);
          gap: var(--ab-spacing-md);
        }

        .ab-board-header-left {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-md);
        }

        .ab-board-type-badge .ab-badge {
          padding: var(--ab-spacing-xs) var(--ab-spacing-sm);
          font-size: var(--ab-font-size-xs);
          font-weight: 600;
          border-radius: var(--ab-radius-sm);
        }

        .ab-badge-scrum {
          background: var(--ab-primary-100);
          color: var(--ab-primary-700);
        }

        .ab-badge-kanban {
          background: var(--ab-accent-100);
          color: var(--ab-accent-700);
        }

        .ab-badge-badge {
          background: var(--ab-info-100);
          color: var(--ab-info-700);
        }

        .ab-board-title {
          font-size: var(--ab-font-size-lg);
          font-weight: 600;
          color: var(--ab-gray-900);
          margin: 0;
        }

        .ab-sprint-selector {
          padding: var(--ab-spacing-xs) var(--ab-spacing-sm);
          border: 1px solid var(--ab-gray-300);
          border-radius: var(--ab-radius-sm);
          font-size: var(--ab-font-size-sm);
          background: var(--ab-white);
          cursor: pointer;
        }

        .ab-board-header-center {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
        }

        .ab-view-toggle {
          display: flex;
          border: 1px solid var(--ab-gray-300);
          border-radius: var(--ab-radius-md);
          overflow: hidden;
        }

        .ab-toggle-btn {
          display: flex;
          align-items: center;
          justify-content: center;
          padding: var(--ab-spacing-sm);
          background: var(--ab-white);
          border: none;
          cursor: pointer;
          color: var(--ab-gray-500);
          transition: all var(--ab-transition-fast);
        }

        .ab-toggle-btn:hover {
          background: var(--ab-gray-50);
        }

        .ab-toggle-btn.active {
          background: var(--ab-primary-500);
          color: var(--ab-white);
        }

        .ab-toggle-btn:not(:last-child) {
          border-right: 1px solid var(--ab-gray-300);
        }

        .ab-board-header-right {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-md);
        }

        .ab-layout-selector {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-xs);
        }

        .ab-layout-label {
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-500);
        }

        .ab-btn-icon {
          display: inline-flex;
          align-items: center;
          gap: var(--ab-spacing-xs);
        }

        .ab-shortcut-hint {
          width: 20px;
          height: 20px;
          border-radius: 50%;
          border: 1px solid var(--ab-gray-400);
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 12px;
          font-weight: 600;
        }

        .ab-btn-ghost {
          background: transparent;
          border: none;
        }

        .ab-btn-ghost:hover {
          background: var(--ab-gray-100);
        }
      `}</style>
    </div>
  );
}