import React, { useState } from 'react';
import { QuickFilter } from '../../../api/boardApi';

interface QuickFilterBarProps {
  quickFilters: QuickFilter[];
  activeFilter: string | null;
  onFilterChange: (filterId: string | null) => void;
  searchQuery: string;
  onSearchChange: (query: string) => void;
}

export default function QuickFilterBar({
  quickFilters,
  activeFilter,
  onFilterChange,
  searchQuery,
  onSearchChange,
}: QuickFilterBarProps) {
  const [isSearchFocused, setIsSearchFocused] = useState(false);

  return (
    <div className="ab-quick-filter-bar">
      <div className="ab-quick-filters">
        <span className="ab-filter-label">Quick Filters:</span>
        <div className="ab-filter-chips">
          {quickFilters.map((filter) => (
            <button
              key={filter.id}
              className={`ab-filter-chip ${activeFilter === filter.id ? 'active' : ''}`}
              onClick={() => onFilterChange(activeFilter === filter.id ? null : filter.id)}
              title={filter.jql}
            >
              {filter.icon && <span className="ab-filter-icon">{filter.icon}</span>}
              {filter.name}
            </button>
          ))}
        </div>
      </div>

      <div className="ab-search-container">
        <div className={`ab-search-box ${isSearchFocused ? 'focused' : ''}`}>
          <svg className="ab-search-icon" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
            <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"/>
          </svg>
          <input
            type="text"
            placeholder="Search issues..."
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            onFocus={() => setIsSearchFocused(true)}
            onBlur={() => setIsSearchFocused(false)}
            className="ab-search-input"
          />
          {searchQuery && (
            <button
              className="ab-search-clear"
              onClick={() => onSearchChange('')}
              title="Clear search"
            >
              ×
            </button>
          )}
        </div>
      </div>

      <style>{`
        .ab-quick-filter-bar {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: var(--ab-spacing-sm) var(--ab-spacing-lg);
          background: var(--ab-white);
          border-bottom: 1px solid var(--ab-gray-200);
          gap: var(--ab-spacing-md);
        }

        .ab-quick-filters {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-sm);
          flex-wrap: wrap;
        }

        .ab-filter-label {
          font-size: var(--ab-font-size-sm);
          font-weight: 500;
          color: var(--ab-gray-600);
        }

        .ab-filter-chips {
          display: flex;
          gap: var(--ab-spacing-xs);
          flex-wrap: wrap;
        }

        .ab-filter-chip {
          display: inline-flex;
          align-items: center;
          gap: var(--ab-spacing-xs);
          padding: var(--ab-spacing-xs) var(--ab-spacing-sm);
          background: var(--ab-gray-100);
          border: 1px solid transparent;
          border-radius: var(--ab-radius-full);
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-700);
          cursor: pointer;
          transition: all var(--ab-transition-fast);
        }

        .ab-filter-chip:hover {
          background: var(--ab-gray-200);
        }

        .ab-filter-chip.active {
          background: var(--ab-primary-100);
          border-color: var(--ab-primary-300);
          color: var(--ab-primary-700);
        }

        .ab-filter-icon {
          font-size: var(--ab-font-size-sm);
        }

        .ab-search-container {
          display: flex;
          align-items: center;
        }

        .ab-search-box {
          display: flex;
          align-items: center;
          gap: var(--ab-spacing-xs);
          padding: var(--ab-spacing-xs) var(--ab-spacing-sm);
          background: var(--ab-gray-50);
          border: 1px solid var(--ab-gray-300);
          border-radius: var(--ab-radius-md);
          transition: all var(--ab-transition-fast);
        }

        .ab-search-box.focused {
          background: var(--ab-white);
          border-color: var(--ab-primary-500);
          box-shadow: 0 0 0 2px var(--ab-primary-100);
        }

        .ab-search-icon {
          color: var(--ab-gray-400);
          flex-shrink: 0;
        }

        .ab-search-input {
          border: none;
          background: transparent;
          font-size: var(--ab-font-size-sm);
          color: var(--ab-gray-800);
          width: 200px;
          outline: none;
        }

        .ab-search-input::placeholder {
          color: var(--ab-gray-400);
        }

        .ab-search-clear {
          background: none;
          border: none;
          font-size: 18px;
          color: var(--ab-gray-400);
          cursor: pointer;
          padding: 0;
          line-height: 1;
        }

        .ab-search-clear:hover {
          color: var(--ab-gray-600);
        }
      `}</style>
    </div>
  );
}