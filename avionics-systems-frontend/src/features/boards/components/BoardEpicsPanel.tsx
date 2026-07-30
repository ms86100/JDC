import React, { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { epicApi } from '../../../api/epicApi';
import type { BoardIssue } from '../../../api/boardApi';

interface Props {
  projectId?: string;
  issues: BoardIssue[];
  selectedEpicId: string | null;
  onSelectEpic: (epicId: string | null) => void;
  collapsed: boolean;
  onToggleCollapsed: () => void;
}

export default function BoardEpicsPanel({
  projectId,
  issues,
  selectedEpicId,
  onSelectEpic,
  collapsed,
  onToggleCollapsed,
}: Props) {
  const [search, setSearch] = useState('');

  const { data: allEpics = [], isLoading } = useQuery({
    queryKey: ['board-epics', projectId],
    queryFn: () => epicApi.getAll().then((r) => Array.isArray(r.data) ? r.data : []).catch(() => []),
    enabled: !!projectId,
  });

  const epicIdsOnBoard = useMemo(() => {
    const ids = new Set<string>();
    issues.forEach((i) => {
      if (i.epicId) ids.add(i.epicId);
    });
    return ids;
  }, [issues]);

  const epics = useMemo(() => {
    const list = allEpics.filter((e) => epicIdsOnBoard.has(e.id) || !projectId);
    const q = search.trim().toLowerCase();
    if (!q) return list;
    return list.filter(
      (e) =>
        e.name?.toLowerCase().includes(q) ||
        e.summary?.toLowerCase().includes(q),
    );
  }, [allEpics, epicIdsOnBoard, projectId, search]);

  const countForEpic = (epicId: string) =>
    issues.filter((i) => i.epicId === epicId).length;

  const noEpicCount = issues.filter((i) => !i.epicId).length;

  if (collapsed) {
    return (
      <aside className="sa-board-epics-panel sa-board-epics-panel--collapsed" aria-label="Epics">
        <button
          type="button"
          className="sa-board-epics-expand"
          onClick={onToggleCollapsed}
          title="Show epics panel"
          aria-expanded={false}
        >
          Epics
        </button>
      </aside>
    );
  }

  return (
    <aside className="sa-board-epics-panel" aria-label="Epics filter">
      <div className="sa-board-epics-head">
        <h2 className="sa-board-epics-title">Epics</h2>
        <button
          type="button"
          className="sa-board-epics-collapse"
          onClick={onToggleCollapsed}
          title="Hide epics panel"
          aria-label="Collapse epics panel"
        >
          ‹
        </button>
      </div>

      <div className="sa-board-epics-search-wrap">
        <svg className="sa-board-epics-search-icon" width="14" height="14" viewBox="0 0 16 16" aria-hidden>
          <path
            fill="currentColor"
            d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"
          />
        </svg>
        <input
          type="search"
          className="sa-board-epics-search"
          placeholder="Search epics…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          aria-label="Search epics"
        />
      </div>

      <div className="sa-board-epics-list" role="list">
        <button
          type="button"
          role="listitem"
          className={`sa-board-epic-item${selectedEpicId === null ? ' is-active' : ''}`}
          onClick={() => onSelectEpic(null)}
        >
          <span className="sa-board-epic-dot" style={{ background: 'var(--sa-n700)' }} />
          <span className="sa-board-epic-name">All epics</span>
          <span className="sa-board-epic-count">{issues.length}</span>
        </button>

        <button
          type="button"
          role="listitem"
          className={`sa-board-epic-item${selectedEpicId === '__none__' ? ' is-active' : ''}`}
          onClick={() => onSelectEpic('__none__')}
        >
          <span className="sa-board-epic-dot" style={{ background: 'var(--sa-n500)' }} />
          <span className="sa-board-epic-name">Issues without epic</span>
          <span className="sa-board-epic-count">{noEpicCount}</span>
        </button>

        {isLoading ? (
          <p className="sa-board-epics-muted">Loading epics…</p>
        ) : (
          epics.map((epic) => (
            <button
              key={epic.id}
              type="button"
              role="listitem"
              className={`sa-board-epic-item${selectedEpicId === epic.id ? ' is-active' : ''}`}
              onClick={() => onSelectEpic(selectedEpicId === epic.id ? null : epic.id)}
            >
              <span
                className="sa-board-epic-dot"
                style={{ background: epic.color || 'var(--sa-brand-500)' }}
              />
              <span className="sa-board-epic-name" title={epic.name}>
                {epic.name}
              </span>
              <span className="sa-board-epic-count">{countForEpic(epic.id)}</span>
            </button>
          ))
        )}
      </div>

      <Link to="/epics" className="sa-board-epics-link">
        View all epics
      </Link>
    </aside>
  );
}
