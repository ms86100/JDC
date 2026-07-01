import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { resolutionApi, Resolution } from '../../../api/issueApi';
import './AdminIssueConfig.css';

export default function ResolutionsPage() {
  const [search, setSearch] = useState('');
  const { data: resolutions, isLoading, isError, refetch } = useQuery({
    queryKey: ['admin', 'resolutions'],
    queryFn: () => resolutionApi.getAll().then((r) => r.data),
  });

  const filtered =
    resolutions?.filter((r) => r.name.toLowerCase().includes(search.toLowerCase())) ?? [];

  return (
    <div className="dc-page ab-issue-config-page">
      <header className="dc-page-header">
        <h1 className="dc-page-title">Resolutions</h1>
        <p className="dc-page-subtitle">
          Resolutions describe how issues were closed (Fixed, Won&apos;t fix, Duplicate, etc.) — Systems
          Data Center issue settings.
        </p>
      </header>

      <div className="ab-issue-config-toolbar">
        <input
          type="search"
          className="admin-search-input-toolbar"
          placeholder="Search resolutions…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <button type="button" className="dc-btn dc-btn-secondary" disabled>
          Add resolution
        </button>
      </div>

      {isLoading && <p className="ab-issue-config-muted">Loading…</p>}
      {isError && (
        <div className="ab-issue-config-error">
          <p>Failed to load resolutions.</p>
          <button type="button" className="dc-btn dc-btn-secondary" onClick={() => refetch()}>
            Retry
          </button>
        </div>
      )}

      {!isLoading && !isError && (
        <div className="ab-recent-table-wrap">
          <table className="ab-recent-table ab-issue-config-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Description</th>
                <th>Order</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={3} className="ab-issue-config-empty-cell">
                    No resolutions found.
                  </td>
                </tr>
              ) : (
                filtered.map((r: Resolution) => (
                  <tr key={r.id}>
                    <td><strong>{r.name}</strong></td>
                    <td>{r.description || '—'}</td>
                    <td>{r.sequence}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
