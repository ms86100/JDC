import { useMemo, useState } from 'react';
import type { ScriptDefinition } from '../../../api/scriptApi';

interface ScriptTreeSidebarProps {
  scripts: ScriptDefinition[];
  selectedId?: string;
  onSelect: (scriptId: string) => void;
  onCategoryFilter: (category: string | null) => void;
}

/** Friendly labels for script types used as fallback category names. */
const TYPE_LABELS: Record<string, string> = {
  CONDITION: 'Conditions',
  VALIDATOR: 'Validators',
  POST_FUNCTION: 'Post-Functions',
  LISTENER: 'Listeners',
  SCHEDULED: 'Scheduled',
  LIBRARY: 'Libraries',
  FIELD_BEHAVIOR: 'Field Behaviors',
  CALCULATED_FIELD: 'Calculated Fields',
  CONSOLE: 'Console',
};

/** Preferred display order for category folders. */
const CATEGORY_ORDER: string[] = [
  'Conditions',
  'Validators',
  'Post-Functions',
  'Listeners',
  'Scheduled',
  'Libraries',
  'Field Behaviors',
  'Calculated Fields',
  'Console',
];

function resolveCategory(script: ScriptDefinition): string {
  if (script.category && script.category !== 'Uncategorized') {
    return script.category;
  }
  return TYPE_LABELS[script.scriptType] || 'Uncategorized';
}

export default function ScriptTreeSidebar({
  scripts,
  selectedId,
  onSelect,
  onCategoryFilter,
}: ScriptTreeSidebarProps) {
  const [search, setSearch] = useState('');
  const [expandedCategories, setExpandedCategories] = useState<Set<string>>(new Set());
  const [activeCategory, setActiveCategory] = useState<string | null>(null);

  // Filter scripts by search
  const filtered = useMemo(() => {
    if (!search.trim()) return scripts;
    const q = search.toLowerCase();
    return scripts.filter(
      (s) =>
        s.name.toLowerCase().includes(q) ||
        s.scriptKey.toLowerCase().includes(q)
    );
  }, [scripts, search]);

  // Group by category
  const grouped = useMemo(() => {
    const map = new Map<string, ScriptDefinition[]>();
    for (const s of filtered) {
      const cat = resolveCategory(s);
      const list = map.get(cat) || [];
      list.push(s);
      map.set(cat, list);
    }

    // Sort categories: known order first, then alphabetical remainder
    const sorted = Array.from(map.entries()).sort(([a], [b]) => {
      const ia = CATEGORY_ORDER.indexOf(a);
      const ib = CATEGORY_ORDER.indexOf(b);
      if (ia >= 0 && ib >= 0) return ia - ib;
      if (ia >= 0) return -1;
      if (ib >= 0) return 1;
      return a.localeCompare(b);
    });

    return sorted;
  }, [filtered]);

  const toggleCategory = (cat: string) => {
    setExpandedCategories((prev) => {
      const next = new Set(prev);
      if (next.has(cat)) {
        next.delete(cat);
      } else {
        next.add(cat);
      }
      return next;
    });
  };

  const handleCategoryClick = (cat: string) => {
    toggleCategory(cat);
  };

  const handleCategoryFilterClick = (cat: string) => {
    if (activeCategory === cat) {
      setActiveCategory(null);
      onCategoryFilter(null);
    } else {
      setActiveCategory(cat);
      onCategoryFilter(cat);
    }
  };

  const handleAllClick = () => {
    setActiveCategory(null);
    onCategoryFilter(null);
  };

  return (
    <aside className="st-sidebar" aria-label="Script tree navigation">
      {/* Search */}
      <div className="st-search-wrapper">
        <input
          type="text"
          className="st-search-input"
          placeholder="Search scripts..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <nav className="st-tree">
        {/* All scripts node */}
        <button
          type="button"
          className={`st-tree-root ${activeCategory === null ? 'st-tree-root--active' : ''}`}
          onClick={handleAllClick}
        >
          <span className="st-tree-chevron st-tree-chevron--open" />
          <span className="st-tree-label">All Scripts</span>
          <span className="st-tree-count">{filtered.length}</span>
        </button>

        {/* Category folders */}
        {grouped.map(([category, categoryScripts]) => {
          const isExpanded = expandedCategories.has(category);
          return (
            <div key={category} className="st-tree-folder">
              <button
                type="button"
                className={`st-tree-folder-header ${activeCategory === category ? 'st-tree-folder-header--active' : ''}`}
                onClick={() => handleCategoryClick(category)}
                onDoubleClick={() => handleCategoryFilterClick(category)}
              >
                <span
                  className={`st-tree-chevron ${isExpanded ? 'st-tree-chevron--open' : ''}`}
                />
                <span className="st-tree-label">{category}</span>
                <span className="st-tree-count">{categoryScripts.length}</span>
              </button>

              {isExpanded && (
                <ul className="st-tree-items">
                  {categoryScripts.map((script) => (
                    <li key={script.id}>
                      <button
                        type="button"
                        className={`st-tree-item ${selectedId === script.id ? 'st-tree-item--selected' : ''}`}
                        onClick={() => onSelect(script.id)}
                        title={`${script.name} (${script.scriptKey})`}
                      >
                        <span className="st-tree-item-name">{script.name}</span>
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          );
        })}
      </nav>

      <style>{`
        .st-sidebar {
          width: 240px;
          min-width: 240px;
          flex-shrink: 0;
          background: var(--jdc-bg-panel, #fff);
          border-right: 1px solid var(--jdc-border, #dfe1e6);
          display: flex;
          flex-direction: column;
          overflow-x: hidden;
          overflow-y: auto;
          box-sizing: border-box;
        }

        .st-search-wrapper {
          padding: 12px 12px 8px;
          border-bottom: 1px solid var(--jdc-border, #dfe1e6);
        }

        .st-search-input {
          width: 100%;
          box-sizing: border-box;
          border: 1px solid var(--sa-n300, #c9ced6);
          border-radius: 4px;
          padding: 6px 10px;
          font-size: 13px;
          background: var(--sa-n50, #f7f8fa);
        }

        .st-search-input:focus {
          outline: none;
          border-color: var(--sa-brand-500);
          background: #fff;
          box-shadow: 0 0 0 2px var(--sa-brand-50, #eef3fb);
        }

        .st-tree {
          flex: 1;
          padding: 6px 0;
        }

        /* Chevron */
        .st-tree-chevron {
          display: inline-block;
          width: 16px;
          height: 16px;
          flex-shrink: 0;
          position: relative;
        }

        .st-tree-chevron::before {
          content: '';
          position: absolute;
          top: 4px;
          left: 5px;
          width: 0;
          height: 0;
          border-left: 5px solid var(--sa-n600, #6b778c);
          border-top: 4px solid transparent;
          border-bottom: 4px solid transparent;
          transition: transform 0.12s ease;
        }

        .st-tree-chevron--open::before {
          transform: rotate(90deg);
        }

        /* All scripts root */
        .st-tree-root {
          display: flex;
          align-items: center;
          gap: 4px;
          width: 100%;
          box-sizing: border-box;
          padding: 8px 12px;
          background: none;
          border: none;
          border-left: 3px solid transparent;
          cursor: pointer;
          font-size: 14px;
          font-weight: 600;
          color: var(--jdc-text, #172b4d);
          text-align: left;
          transition: background 0.12s ease;
        }

        .st-tree-root:hover {
          background: var(--sa-n100);
        }

        .st-tree-root--active {
          background: var(--sa-brand-50, #e6edfa);
          border-left-color: var(--jdc-primary, #0052cc);
          color: var(--jdc-primary, #0052cc);
        }

        /* Folder header */
        .st-tree-folder-header {
          display: flex;
          align-items: center;
          gap: 4px;
          width: 100%;
          box-sizing: border-box;
          padding: 7px 12px 7px 20px;
          background: none;
          border: none;
          border-left: 3px solid transparent;
          cursor: pointer;
          font-size: 13px;
          font-weight: 500;
          color: var(--jdc-text, #172b4d);
          text-align: left;
          transition: background 0.12s ease;
        }

        .st-tree-folder-header:hover {
          background: var(--sa-n100);
        }

        .st-tree-folder-header--active {
          background: var(--sa-brand-50, #e6edfa);
          border-left-color: var(--jdc-primary, #0052cc);
          color: var(--jdc-primary, #0052cc);
          font-weight: 600;
        }

        .st-tree-label {
          flex: 1;
          min-width: 0;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .st-tree-count {
          font-size: 11px;
          font-weight: 600;
          color: var(--sa-n500, #8993a4);
          background: var(--sa-n100, #f4f5f7);
          padding: 1px 6px;
          border-radius: 10px;
          flex-shrink: 0;
        }

        /* Script items */
        .st-tree-items {
          list-style: none;
          margin: 0;
          padding: 0;
        }

        .st-tree-item {
          display: block;
          width: 100%;
          box-sizing: border-box;
          padding: 6px 12px 6px 44px;
          background: none;
          border: none;
          border-left: 3px solid transparent;
          cursor: pointer;
          font-size: 13px;
          color: var(--jdc-text, #172b4d);
          text-align: left;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          transition: background 0.12s ease;
        }

        .st-tree-item:hover {
          background: var(--sa-n100);
        }

        .st-tree-item--selected {
          background: var(--sa-brand-50, #e6edfa);
          border-left-color: var(--jdc-primary, #0052cc);
          font-weight: 600;
          color: var(--jdc-primary, #0052cc);
        }

        .st-tree-item-name {
          display: block;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      `}</style>
    </aside>
  );
}
