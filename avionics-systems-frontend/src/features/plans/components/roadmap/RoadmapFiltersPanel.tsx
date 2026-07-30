interface RoadmapFiltersPanelProps {
  showFullHierarchy: boolean;
  onShowFullHierarchyChange: (v: boolean) => void;
  textFilter: string;
  onTextFilterChange: (v: string) => void;
  statusFilter: string;
  onStatusFilterChange: (v: string) => void;
  onClose: () => void;
}

const FILTER_ROWS = [
  'Releases',
  'Teams',
  'Assignees',
  'Sprints',
  'Projects',
  'Issue sources',
  'Issue types',
  'Components',
  'Labels',
] as const;

export default function RoadmapFiltersPanel({
  showFullHierarchy,
  onShowFullHierarchyChange,
  textFilter,
  onTextFilterChange,
  statusFilter,
  onStatusFilterChange,
  onClose,
}: RoadmapFiltersPanelProps) {
  return (
    <>
      <button
        type="button"
        className="sa-rm-popover-backdrop"
        aria-label="Close filters"
        onClick={onClose}
      />
      <div className="sa-rm-popover sa-rm-popover--filters" role="dialog" aria-labelledby="rm-filters-title">
        <div className="sa-rm-popover-header">
          <h3 id="rm-filters-title">Filters</h3>
          <button type="button" className="sa-rm-popover-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>

        <div className="sa-rm-popover-section sa-rm-field">
          <label htmlFor="rm-text-filter">Search issues</label>
          <input
            id="rm-text-filter"
            type="search"
            className="sa-rm-input sa-rm-input--search"
            placeholder="Filter by key or summary"
            value={textFilter}
            onChange={(e) => onTextFilterChange(e.target.value)}
          />
        </div>

        {FILTER_ROWS.map((f) => (
          <div key={f} className="sa-rm-filter-row sa-rm-filter-row--disabled">
            <span>{f}</span>
            <span className="sa-rm-coming-soon">Soon</span>
          </div>
        ))}

        <div className="sa-rm-filter-row">
          <span>Statuses</span>
          <select
            className="sa-rm-select"
            style={{ width: 'auto', minWidth: 120 }}
            value={statusFilter}
            onChange={(e) => onStatusFilterChange(e.target.value)}
          >
            <option value="All">All</option>
            <option value="TO DO">To Do</option>
            <option value="IN PROGRESS">In Progress</option>
            <option value="DONE">Done</option>
          </select>
        </div>

        <label className="sa-rm-checkbox-row">
          <input
            type="checkbox"
            checked={showFullHierarchy}
            onChange={(e) => onShowFullHierarchyChange(e.target.checked)}
          />
          Show full hierarchy
        </label>
      </div>
    </>
  );
}
