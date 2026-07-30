interface RoadmapViewSettingsProps {
  sortBy: string;
  onSortByChange: (v: string) => void;
  onClose: () => void;
}

const SORT_OPTIONS = [
  'Rank (default)',
  'Assignee',
  'Due date',
  'Priority',
  'Status',
  'Estimate (d)',
  'Target start',
  'Target end',
  'Team',
];

export default function RoadmapViewSettings({ sortBy, onSortByChange, onClose }: RoadmapViewSettingsProps) {
  return (
    <>
      <button
        type="button"
        className="sa-rm-popover-backdrop"
        aria-label="Close view settings"
        onClick={onClose}
      />
      <div className="sa-rm-popover sa-rm-popover--settings" role="dialog" aria-labelledby="rm-view-settings-title">
        <div className="sa-rm-popover-header">
          <h3 id="rm-view-settings-title">View settings</h3>
          <button type="button" className="sa-rm-popover-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>

        <div className="sa-rm-popover-section sa-rm-field sa-rm-field--disabled">
          <label htmlFor="rm-group-by">
            Group by <span className="sa-rm-coming-soon">Soon</span>
          </label>
          <select id="rm-group-by" className="sa-rm-select sa-rm-select--disabled" defaultValue="None" disabled>
            <option>None</option>
            <option>Team</option>
            <option>Sprint</option>
            <option>Release</option>
          </select>
        </div>

        <div className="sa-rm-popover-section sa-rm-field sa-rm-field--disabled">
          <label htmlFor="rm-color-by">
            Color by <span className="sa-rm-coming-soon">Soon</span>
          </label>
          <select id="rm-color-by" className="sa-rm-select sa-rm-select--disabled" defaultValue="None" disabled>
            <option>None</option>
            <option>Status</option>
            <option>Priority</option>
            <option>Team</option>
          </select>
        </div>

        <div className="sa-rm-popover-section sa-rm-field">
          <label id="rm-sort-by-label">Sort by</label>
          <div className="sa-rm-sort-list" role="listbox" aria-labelledby="rm-sort-by-label">
            {SORT_OPTIONS.map((opt) => (
              <button
                key={opt}
                type="button"
                role="option"
                aria-selected={sortBy === opt}
                className={`sa-rm-sort-option ${sortBy === opt ? 'sa-rm-sort-option--active' : ''}`}
                onClick={() => onSortByChange(opt)}
              >
                <span>{opt}</span>
                {sortBy === opt && <span aria-hidden="true">✓</span>}
              </button>
            ))}
          </div>
        </div>
      </div>
    </>
  );
}
