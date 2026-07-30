import { useState, useRef, useEffect } from 'react';

const HIERARCHY_LEVELS = ['Initiative', 'Epic', 'Story', 'Sub-task'] as const;

interface RoadmapToolbarProps {
  hierarchyFrom: string;
  hierarchyTo: string;
  onHierarchyFromChange: (v: string) => void;
  onHierarchyToChange: (v: string) => void;
  zoom: '3M' | '1Y' | 'Fit';
  onZoomChange: (z: '3M' | '1Y' | 'Fit') => void;
  onOpenFilters: () => void;
  onOpenViewSettings: () => void;
  filtersOpen: boolean;
  viewSettingsOpen: boolean;
}

export default function RoadmapToolbar({
  hierarchyFrom,
  hierarchyTo,
  onHierarchyFromChange,
  onHierarchyToChange,
  zoom,
  onZoomChange,
  onOpenFilters,
  onOpenViewSettings,
  filtersOpen,
  viewSettingsOpen,
}: RoadmapToolbarProps) {
  const [hierarchyOpen, setHierarchyOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const close = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setHierarchyOpen(false);
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  return (
    <div className="sa-roadmap-toolbar">
      <div className="sa-roadmap-toolbar__left">
        <div className="jdc-hierarchy-dropdown" ref={ref} style={{ position: 'relative' }}>
          <button
            type="button"
            className={`sa-rm-btn ${hierarchyOpen ? 'sa-rm-btn--active' : ''}`}
            onClick={() => setHierarchyOpen((o) => !o)}
            aria-expanded={hierarchyOpen}
          >
            Hierarchy: <strong>{hierarchyFrom}</strong> to <strong>{hierarchyTo}</strong> ▾
          </button>
          {hierarchyOpen && (
            <div className="sa-rm-hierarchy-menu">
              <div className="sa-rm-field">
                <label htmlFor="rm-hierarchy-from">From</label>
                <select
                  id="rm-hierarchy-from"
                  className="sa-rm-select"
                  value={hierarchyFrom}
                  onChange={(e) => onHierarchyFromChange(e.target.value)}
                >
                  {HIERARCHY_LEVELS.map((l) => (
                    <option key={l} value={l}>{l}</option>
                  ))}
                </select>
              </div>
              <div className="sa-rm-field">
                <label htmlFor="rm-hierarchy-to">To</label>
                <select
                  id="rm-hierarchy-to"
                  className="sa-rm-select"
                  value={hierarchyTo}
                  onChange={(e) => onHierarchyToChange(e.target.value)}
                >
                  {HIERARCHY_LEVELS.map((l) => (
                    <option key={l} value={l}>{l}</option>
                  ))}
                </select>
              </div>
            </div>
          )}
        </div>
        <button
          type="button"
          className={`sa-rm-btn ${filtersOpen ? 'sa-rm-btn--active' : ''}`}
          onClick={onOpenFilters}
          aria-expanded={filtersOpen}
        >
          Filters ▾
        </button>
      </div>

      <div className="sa-roadmap-toolbar__right">
        <div className="sa-rm-segment-group" role="group" aria-label="Timeline zoom">
          {(['3M', '1Y', 'Fit'] as const).map((z) => (
            <button
              key={z}
              type="button"
              className={`sa-rm-btn sa-rm-btn--segment ${zoom === z ? 'sa-rm-btn--on' : ''}`}
              onClick={() => onZoomChange(z)}
            >
              {z}
            </button>
          ))}
        </div>
        <button
          type="button"
          className={`sa-rm-btn sa-rm-btn--primary ${viewSettingsOpen ? 'sa-rm-btn--active' : ''}`}
          onClick={onOpenViewSettings}
          aria-expanded={viewSettingsOpen}
        >
          View settings ▾
        </button>
      </div>
    </div>
  );
}
