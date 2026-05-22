import React from 'react';
import type { QuickFilter } from '../../../api/boardApi';

type SwimlanField = 'none' | 'epic' | 'assignee' | 'priority' | 'labels' | 'sprint';

interface KanbanFilterStripProps {
  quickFilters: QuickFilter[];
  activeFilter: string | null;
  onFilterChange: (filterId: string | null) => void;
  searchQuery: string;
  onSearchChange: (query: string) => void;
  swimlaneField: SwimlanField;
  onSwimlaneChange: (field: SwimlanField) => void;
  viewMode: 'board' | 'swimlane';
  onToggleView: () => void;
}

/** Filters + search + swimlanes in one compact row (Jira DC board filter bar). */
export default function KanbanFilterStrip({
  quickFilters,
  activeFilter,
  onFilterChange,
  searchQuery,
  onSearchChange,
  swimlaneField,
  onSwimlaneChange,
  viewMode,
  onToggleView,
}: KanbanFilterStripProps) {
  return (
    <div className="sa-kanban-filter-strip" role="toolbar" aria-label="Board filters">
      <div className="sa-kanban-filter-chips" aria-label="Quick filters">
        {quickFilters.map((filter) => (
          <button
            key={filter.id}
            type="button"
            className={`sa-kanban-filter-chip${activeFilter === filter.id ? ' is-active' : ''}`}
            onClick={() => onFilterChange(activeFilter === filter.id ? null : filter.id)}
            title={filter.jql}
          >
            {filter.name}
          </button>
        ))}
      </div>

      <div className="sa-kanban-filter-strip-right">
        <label className="sa-kanban-filter-swimlane">
          <span className="sa-kanban-chrome-field-label">Swimlanes</span>
          <select
            className="sa-kanban-chrome-select sa-kanban-chrome-select--sm"
            value={swimlaneField}
            onChange={(e) => onSwimlaneChange(e.target.value as SwimlanField)}
            aria-label="Swimlane grouping"
          >
            <option value="none">None</option>
            <option value="epic">Epic</option>
            <option value="assignee">Assignee</option>
            <option value="priority">Priority</option>
            <option value="labels">Labels</option>
          </select>
        </label>

        <div className="sa-kanban-view-toggle" role="group" aria-label="Board layout">
          <button
            type="button"
            className={`sa-kanban-view-toggle-btn${viewMode === 'board' ? ' is-active' : ''}`}
            onClick={() => viewMode !== 'board' && onToggleView()}
            title="Columns"
          >
            Board
          </button>
          <button
            type="button"
            className={`sa-kanban-view-toggle-btn${viewMode === 'swimlane' ? ' is-active' : ''}`}
            onClick={() => viewMode !== 'swimlane' && onToggleView()}
            title="Swimlanes"
          >
            Swimlanes
          </button>
        </div>

        <div className="sa-kanban-search">
          <svg className="sa-kanban-search-icon" width="14" height="14" viewBox="0 0 16 16" aria-hidden>
            <path
              fill="currentColor"
              d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"
            />
          </svg>
          <input
            type="search"
            className="sa-kanban-search-input"
            placeholder="Filter issues on board…"
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            aria-label="Filter issues on board"
          />
          {searchQuery && (
            <button
              type="button"
              className="sa-kanban-search-clear"
              onClick={() => onSearchChange('')}
              aria-label="Clear search"
            >
              ×
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
