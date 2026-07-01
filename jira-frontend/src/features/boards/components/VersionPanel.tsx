import React, { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { versionApi } from '../../../api/versionApi';
import type { BoardIssue } from '../../../api/boardApi';

interface Props {
  projectId?: string;
  issues: BoardIssue[];
  selectedVersionId: string | null;
  onSelectVersion: (versionId: string | null) => void;
  collapsed: boolean;
  onToggleCollapsed: () => void;
}

interface VersionGroup {
  id: string;
  name: string;
  released: boolean;
  releaseDate: string | null;
  issues: BoardIssue[];
}

export default function VersionPanel({
  projectId,
  issues,
  selectedVersionId,
  onSelectVersion,
  collapsed,
  onToggleCollapsed,
}: Props) {
  const [search, setSearch] = useState('');
  const [showReleased, setShowReleased] = useState(false);

  const { data: allVersions = [], isLoading } = useQuery({
    queryKey: ['board-versions', projectId],
    queryFn: () => versionApi.getByProject(projectId || '').then((r) => r.data).catch(() => []),
    enabled: !!projectId,
  });

  const versionGroups = useMemo(() => {
    const groups: VersionGroup[] = [];
    const versionIssueMap = new Map<string, BoardIssue[]>();
    const unreleased: BoardIssue[] = [];

    issues.forEach((issue) => {
      const fixVersions = issue.fixVersions || [];
      if (fixVersions.length === 0) {
        unreleased.push(issue);
      } else {
        fixVersions.forEach((versionId) => {
          if (!versionIssueMap.has(versionId)) {
            versionIssueMap.set(versionId, []);
          }
          versionIssueMap.get(versionId)!.push(issue);
        });
      }
    });

    allVersions.forEach((version: { id: string; name: string; released: boolean; releaseDate?: string }) => {
      const versionIssues = versionIssueMap.get(version.id) || [];
      if (version.released && !showReleased) return;
      groups.push({
        id: version.id,
        name: version.name,
        released: version.released,
        releaseDate: version.releaseDate || null,
        issues: versionIssues,
      });
    });

    if (unreleased.length > 0) {
      groups.unshift({
        id: '__unreleased__',
        name: 'Unreleased',
        released: false,
        releaseDate: null,
        issues: unreleased,
      });
    }

    const q = search.trim().toLowerCase();
    if (!q) return groups;
    return groups.filter((g) => g.name.toLowerCase().includes(q));
  }, [allVersions, issues, search, showReleased]);

  const totalIssues = issues.length;
  const completedInVersions = versionGroups
    .filter((g) => g.released)
    .reduce((sum, g) => sum + g.issues.length, 0);

  if (collapsed) {
    return (
      <aside className="sa-board-version-panel sa-board-version-panel--collapsed" aria-label="Versions">
        <button
          type="button"
          className="sa-board-version-expand"
          onClick={onToggleCollapsed}
          title="Show versions panel"
          aria-expanded={false}
        >
          Versions
        </button>
      </aside>
    );
  }

  return (
    <aside className="sa-board-version-panel" aria-label="Versions filter">
      <div className="sa-board-version-head">
        <h2 className="sa-board-version-title">Versions</h2>
        <button
          type="button"
          className="sa-board-version-collapse"
          onClick={onToggleCollapsed}
          title="Hide versions panel"
          aria-label="Collapse versions panel"
        >
          ‹
        </button>
      </div>

      <input
        type="search"
        className="sa-board-version-search"
        placeholder="Search versions…"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        aria-label="Search versions"
      />

      <label className="sa-board-version-toggle">
        <input
          type="checkbox"
          checked={showReleased}
          onChange={(e) => setShowReleased(e.target.checked)}
        />
        Show released versions
      </label>

      <div className="sa-board-version-stats">
        <span className="sa-board-version-stat">
          <strong>{totalIssues}</strong> total issues
        </span>
        <span className="sa-board-version-stat">
          <strong>{completedInVersions}</strong> in released
        </span>
      </div>

      <div className="sa-board-version-list" role="list">
        <button
          type="button"
          role="listitem"
          className={`sa-board-version-item${selectedVersionId === null ? ' is-active' : ''}`}
          onClick={() => onSelectVersion(null)}
        >
          <span className="sa-board-version-dot" style={{ background: 'var(--sa-n600)' }} />
          <span className="sa-board-version-name">All versions</span>
          <span className="sa-board-version-count">{totalIssues}</span>
        </button>

        {isLoading ? (
          <p className="sa-board-version-muted">Loading versions…</p>
        ) : (
          versionGroups.map((group) => (
            <button
              key={group.id}
              type="button"
              role="listitem"
              className={`sa-board-version-item${selectedVersionId === group.id ? ' is-active' : ''}${group.released ? ' is-released' : ''}`}
              onClick={() => onSelectVersion(selectedVersionId === group.id ? null : group.id)}
            >
              <span
                className="sa-board-version-dot"
                style={{ background: group.released ? '#00875a' : 'var(--sa-brand-500)' }}
              />
              <span className="sa-board-version-name">
                {group.name}
                {group.releaseDate && (
                  <span className="sa-board-version-date">
                    ({new Date(group.releaseDate).toLocaleDateString()})
                  </span>
                )}
              </span>
              <span className="sa-board-version-count">{group.issues.length}</span>
            </button>
          ))
        )}
      </div>

      <Link to="/admin/versions" className="sa-board-version-link">
        View all versions
      </Link>
    </aside>
  );
}
