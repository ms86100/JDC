import { useState } from 'react';
import { useDependencies, useCreateDependency, useDeleteDependency } from '../../hooks/useDependencies';
import { CreateDependencyRequest } from '../../../../api/planApi';
import { appNotify } from '../../../../lib/appNotify';

interface DependenciesViewProps {
  planId: string;
}

export default function DependenciesView({ planId }: DependenciesViewProps) {
  const [showCreate, setShowCreate] = useState(false);
  const [blockingIssueId, setBlockingIssueId] = useState('');
  const [blockedIssueId, setBlockedIssueId] = useState('');
  const [zoom, setZoom] = useState<'Fit' | '100%' | '50%'>('Fit');
  const [filter, setFilter] = useState('');

  const { data: dependencies, isLoading } = useDependencies(planId);
  const createMutation = useCreateDependency();
  const deleteMutation = useDeleteDependency();

  const filtered = (dependencies ?? []).filter((dep) => {
    if (!filter.trim()) return true;
    const q = filter.toLowerCase();
    return (
      (dep.blockingIssueKey ?? '').toLowerCase().includes(q) ||
      (dep.blockedIssueKey ?? '').toLowerCase().includes(q) ||
      (dep.blockingIssueSummary ?? '').toLowerCase().includes(q) ||
      (dep.blockedIssueSummary ?? '').toLowerCase().includes(q)
    );
  });

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    const request: CreateDependencyRequest = {
      blockingIssueId,
      blockedIssueId,
      dependencyType: 'BLOCKS',
    };
    createMutation.mutate(
      { planId, data: request },
      {
        onSuccess: () => {
          setShowCreate(false);
          setBlockingIssueId('');
          setBlockedIssueId('');
        },
        onError: (error: Error) => {
          appNotify.error(error.message || 'Failed to create dependency.');
        },
      },
    );
  };

  const scale = zoom === '50%' ? 0.5 : zoom === '100%' ? 1 : 0.85;

  if (isLoading) {
    return (
      <div className="ab-loading">
        <div className="ab-spinner" />
      </div>
    );
  }

  return (
    <div className="jdc-dependencies-report">
      <div className="jdc-deps-toolbar">
        <h3>Dependencies report</h3>
        <input
          type="search"
          placeholder="Filter by key or summary"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
        <div className="jdc-deps-zoom">
          <span>Zoom:</span>
          {(['Fit', '100%', '50%'] as const).map((z) => (
            <button
              key={z}
              type="button"
              className={`jdc-btn ${zoom === z ? 'active' : ''}`}
              onClick={() => setZoom(z)}
            >
              {z}
            </button>
          ))}
        </div>
        <button type="button" className="jdc-btn jdc-btn-primary" onClick={() => setShowCreate(true)}>
          + Add dependency
        </button>
      </div>

      <div className="jdc-deps-report-wrap" style={{ transform: `scale(${scale})`, transformOrigin: 'top left' }}>
        <table className="jdc-deps-report-table">
          <thead>
            <tr>
              <th>Blocking</th>
              <th />
              <th>Blocked</th>
              <th>Type</th>
              <th>Status</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {filtered.map((dep) => (
              <tr key={dep.id} className={dep.isCircular ? 'circular' : ''}>
                <td>
                  <div className="jdc-dep-cell">
                    <strong>{dep.blockingIssueKey ?? dep.blockingIssueId}</strong>
                    <span>{dep.blockingIssueSummary}</span>
                    <span className="jdc-lozenge">{dep.blockingIssueStatus ?? '—'}</span>
                  </div>
                </td>
                <td className="jdc-dep-arrow">→</td>
                <td>
                  <div className="jdc-dep-cell">
                    <strong>{dep.blockedIssueKey ?? dep.blockedIssueId}</strong>
                    <span>{dep.blockedIssueSummary}</span>
                    <span className="jdc-lozenge">{dep.blockedIssueStatus ?? '—'}</span>
                  </div>
                </td>
                <td>{dep.dependencyType}</td>
                <td>{dep.isCircular ? <span className="jdc-badge-danger">Circular</span> : 'OK'}</td>
                <td>
                  <button type="button" className="jdc-btn" onClick={() => deleteMutation.mutate({ planId, dependencyId: dep.id })}>
                    Remove
                  </button>
                </td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr>
                <td colSpan={6} className="jdc-empty-cell">No dependencies match your filters.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {showCreate && (
        <div className="jdc-modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="jdc-modal" onClick={(e) => e.stopPropagation()}>
            <h3>Add dependency</h3>
            <form onSubmit={handleCreate}>
              <label>Blocking issue ID<input value={blockingIssueId} onChange={(e) => setBlockingIssueId(e.target.value)} required /></label>
              <label>Blocked issue ID<input value={blockedIssueId} onChange={(e) => setBlockedIssueId(e.target.value)} required /></label>
              <div className="jdc-modal-footer">
                <button type="button" className="jdc-btn" onClick={() => setShowCreate(false)}>Cancel</button>
                <button type="submit" className="jdc-btn jdc-btn-primary" disabled={createMutation.isPending}>Add</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
