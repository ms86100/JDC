import React, { useState } from 'react';
import type { QuickFilter } from '../../../api/boardApi';

type SwimlanField = 'none' | 'epic' | 'assignee' | 'priority' | 'labels' | 'sprint';
export type GroupByField = SwimlanField;

export interface AssigneeQuickFilter {
  id: string;
  name: string;
}

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
  assignees?: AssigneeQuickFilter[];
  activeAssigneeId?: string | null;
  onAssigneeFilterChange?: (assigneeId: string | null) => void;
  epicOptions?: { id: string; name: string }[];
  activeEpicId?: string | null;
  onEpicFilterChange?: (epicId: string | null) => void;
  groupBy?: GroupByField;
  onGroupByChange?: (field: GroupByField) => void;
  jiraDcLayout?: boolean;
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
  assignees = [],
  activeAssigneeId = null,
  onAssigneeFilterChange,
  epicOptions = [],
  activeEpicId = null,
  onEpicFilterChange,
  groupBy = 'none',
  onGroupByChange,
  jiraDcLayout = false,
}: KanbanFilterStripProps) {
  const [showInsights, setShowInsights] = useState(false);

  const visibleAssignees = assignees.slice(0, 5);
  const extraAssignees = assignees.length - visibleAssignees.length;

  return (
    <>
      <div className="sa-kanban-filter-strip" role="toolbar" aria-label="Board filters">
        <div className="sa-kanban-filter-strip-left">
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
              placeholder="Search board"
              value={searchQuery}
              onChange={(e) => onSearchChange(e.target.value)}
              aria-label="Search board"
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

          {jiraDcLayout && assignees.length > 0 && onAssigneeFilterChange && (
            <div className="sa-kanban-avatar-filters" aria-label="Filter by assignee">
              {visibleAssignees.map((a) => (
                <button
                  key={a.id}
                  type="button"
                  className={`sa-kanban-avatar-filter${activeAssigneeId === a.id ? ' is-active' : ''}`}
                  title={a.name}
                  onClick={() =>
                    onAssigneeFilterChange(activeAssigneeId === a.id ? null : a.id)
                  }
                >
                  {a.name.charAt(0).toUpperCase()}
                </button>
              ))}
              {extraAssignees > 0 && (
                <span className="sa-kanban-avatar-filter sa-kanban-avatar-filter--more" title="More assignees">
                  +{extraAssignees}
                </span>
              )}
            </div>
          )}

          {!jiraDcLayout && (
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
          )}
        </div>

        <div className="sa-kanban-filter-strip-right">
          {jiraDcLayout && epicOptions.length > 0 && onEpicFilterChange && (
            <label className="sa-kanban-filter-swimlane">
              <span className="sa-kanban-chrome-field-label">Epic</span>
              <select
                className="sa-kanban-chrome-select sa-kanban-chrome-select--sm"
                value={activeEpicId ?? ''}
                onChange={(e) => onEpicFilterChange(e.target.value || null)}
                aria-label="Filter by epic"
              >
                <option value="">All epics</option>
                {epicOptions.map((ep) => (
                  <option key={ep.id} value={ep.id}>
                    {ep.name}
                  </option>
                ))}
              </select>
            </label>
          )}

          {jiraDcLayout ? (
            <label className="sa-kanban-filter-swimlane">
              <span className="sa-kanban-chrome-field-label">Group by</span>
              <select
                className="sa-kanban-chrome-select sa-kanban-chrome-select--sm"
                value={groupBy}
                onChange={(e) => onGroupByChange?.(e.target.value as GroupByField)}
                aria-label="Group by"
              >
                <option value="none">None</option>
                <option value="epic">Epic</option>
                <option value="assignee">Assignee</option>
                <option value="priority">Priority</option>
                <option value="labels">Labels</option>
              </select>
            </label>
          ) : (
            <>
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
            </>
          )}

          {jiraDcLayout && (
            <button
              type="button"
              className="sa-kanban-insights-btn"
              onClick={() => setShowInsights((v) => !v)}
              aria-expanded={showInsights}
            >
              <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden>
                <path
                  fill="currentColor"
                  d="M2 12h12v1H2v-1zm1-2h2V4H3v6zm3 0h2V2H6v8zm3 0h2V6H9v4z"
                />
              </svg>
              Insights
            </button>
          )}
        </div>
      </div>

      {jiraDcLayout && showInsights && (
        <div className="sa-kanban-insights-placeholder" role="status">
          Board insights coming soon! (cycle time, CFD, control chart)
        </div>
      )}
    </>
  );
}
