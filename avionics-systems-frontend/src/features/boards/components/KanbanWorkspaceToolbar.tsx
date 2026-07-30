import React from 'react';
import type { AgileBoard } from '../../../api/boardApi';
import type { ProjectResponse } from '../../../api/projectApi';

export interface KanbanStatusBanner {
  tone: 'info' | 'warn';
  message: string;
  actionLabel?: string;
  onAction?: () => void;
}

interface KanbanWorkspaceToolbarProps {
  projects: ProjectResponse[];
  projectId: string;
  onProjectChange: (id: string) => void;
  boards?: AgileBoard[];
  boardId?: string;
  onBoardChange?: (id: string) => void;
  issueCount?: number;
  statusBanner?: KanbanStatusBanner | null;
  onCreateIssue?: () => void;
  onOpenConfig?: () => void;
  showEpicsPanel?: boolean;
  onToggleEpics?: () => void;
  cardLayout?: 'FULL' | 'COMPACT' | 'MINI';
  onCardLayoutChange?: (layout: 'FULL' | 'COMPACT' | 'MINI') => void;
  legacyDcVariant?: boolean;
}

/** Avionics Systems board chrome — flat header: "Board" + actions only in workspace mode. */
export default function KanbanWorkspaceToolbar({
  projects,
  projectId,
  onProjectChange,
  boards = [],
  boardId,
  onBoardChange,
  issueCount = 0,
  statusBanner,
  onCreateIssue,
  onOpenConfig,
  showEpicsPanel,
  onToggleEpics,
  cardLayout = 'FULL',
  onCardLayoutChange,
  legacyDcVariant = false,
}: KanbanWorkspaceToolbarProps) {
  const project = projects.find((p) => p.id === projectId);
  const board = boards.find((b) => b.id === boardId);

  return (
    <header className={`sa-kanban-chrome${legacyDcVariant ? ' sa-kanban-chrome--dc' : ''}`} aria-label="Kanban board controls">
      <div className="sa-kanban-chrome-primary">
        {!legacyDcVariant && (
          <div className="sa-kanban-chrome-context">
            <label className="sa-kanban-chrome-field">
              <span className="sa-kanban-chrome-field-label">Project</span>
              <select
                className="sa-kanban-chrome-select"
                value={projectId}
                onChange={(e) => onProjectChange(e.target.value)}
                aria-label="Select project"
              >
                {projects.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} ({p.projectKey})
                  </option>
                ))}
              </select>
            </label>
            {boards.length > 1 && onBoardChange && (
              <label className="sa-kanban-chrome-field">
                <span className="sa-kanban-chrome-field-label">Board</span>
                <select
                  className="sa-kanban-chrome-select"
                  value={boardId ?? ''}
                  onChange={(e) => onBoardChange(e.target.value)}
                  aria-label="Select board"
                >
                  {boards.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.name}
                    </option>
                  ))}
                </select>
              </label>
            )}
          </div>
        )}

        <div className="sa-kanban-chrome-title-block">
          {!legacyDcVariant && <span className="sa-kanban-chrome-badge">Kanban</span>}
          <h1 className="sa-kanban-chrome-title">
            {legacyDcVariant ? 'Board' : (board?.name ?? `${project?.name ?? 'Project'} board`)}
          </h1>
          {!legacyDcVariant && project?.projectKey && (
            <span className="sa-kanban-chrome-subtitle">{project.projectKey}</span>
          )}
          {!legacyDcVariant && (
            <span className="sa-kanban-chrome-meta">{issueCount} issues on board</span>
          )}
        </div>

        <div className="sa-kanban-chrome-actions">
          {onToggleEpics && (
            <button
              type="button"
              className={`sa-kanban-chrome-btn sa-kanban-chrome-btn--ghost${showEpicsPanel ? ' is-on' : ''}`}
              onClick={onToggleEpics}
              aria-pressed={showEpicsPanel}
            >
              Epics
            </button>
          )}
          {!legacyDcVariant && onCardLayoutChange && (
            <label className="sa-kanban-chrome-field sa-kanban-chrome-field--inline">
              <span className="sa-kanban-chrome-field-label">Cards</span>
              <select
                className="sa-kanban-chrome-select sa-kanban-chrome-select--sm"
                value={cardLayout}
                onChange={(e) => onCardLayoutChange(e.target.value as 'FULL' | 'COMPACT' | 'MINI')}
                aria-label="Card layout"
              >
                <option value="FULL">Full</option>
                <option value="COMPACT">Compact</option>
                <option value="MINI">Mini</option>
              </select>
            </label>
          )}
          {onOpenConfig && (
            <button
              type="button"
              className="sa-kanban-chrome-btn sa-kanban-chrome-btn--ghost"
              onClick={onOpenConfig}
            >
              Board settings
            </button>
          )}
          {onCreateIssue && (
            <button
              type="button"
              className="sa-kanban-chrome-btn sa-kanban-chrome-btn--primary"
              onClick={onCreateIssue}
            >
              Create issue
            </button>
          )}
        </div>
      </div>

      {statusBanner && (
        <div
          className={`sa-kanban-chrome-banner sa-kanban-chrome-banner--${statusBanner.tone}`}
          role="status"
        >
          <span>{statusBanner.message}</span>
          {statusBanner.actionLabel && statusBanner.onAction && (
            <button type="button" className="sa-kanban-chrome-banner-action" onClick={statusBanner.onAction}>
              {statusBanner.actionLabel}
            </button>
          )}
        </div>
      )}
    </header>
  );
}
