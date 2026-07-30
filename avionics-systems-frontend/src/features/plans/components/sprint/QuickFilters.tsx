import React from 'react';

type FilterMode = 'ALL' | 'MY_ISSUES' | 'RECENTLY_UPDATED';

interface QuickFiltersProps {
  activeFilter: FilterMode;
  onFilterChange: (filter: FilterMode) => void;
}

export default function QuickFilters({ activeFilter, onFilterChange }: QuickFiltersProps) {
  const filters: { key: FilterMode; label: string }[] = [
    { key: 'ALL', label: 'All Issues' },
    { key: 'MY_ISSUES', label: 'Only My Issues' },
    { key: 'RECENTLY_UPDATED', label: 'Recently Updated' },
  ];

  return (
    <div className="ab-quick-filters">
      <span className="ab-filters-label">QUICK FILTERS:</span>
      <div className="ab-filter-buttons">
        {filters.map((filter) => (
          <button
            key={filter.key}
            className={`ab-filter-btn ${activeFilter === filter.key ? 'ab-active' : ''}`}
            onClick={() => onFilterChange(filter.key)}
          >
            {filter.label}
          </button>
        ))}
      </div>
    </div>
  );
}
