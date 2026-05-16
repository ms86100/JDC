import { useState } from 'react';
import { useDependencies, useCreateDependency, useDeleteDependency } from '../../hooks/useDependencies';
import { CreateDependencyRequest } from '../../../../api/planApi';

interface DependenciesViewProps {
  planId: string;
}

export default function DependenciesView({ planId }: DependenciesViewProps) {
  const [showCreate, setShowCreate] = useState(false);
  const [blockingIssueId, setBlockingIssueId] = useState('');
  const [blockedIssueId, setBlockedIssueId] = useState('');

  const { data: dependencies, isLoading } = useDependencies(planId);
  const createMutation = useCreateDependency();
  const deleteMutation = useDeleteDependency();

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
          alert(error.message || 'Failed to create dependency. Check for circular dependencies.');
        },
      }
    );
  };

  const handleDelete = (dependencyId: string) => {
    deleteMutation.mutate({ planId, dependencyId }, {
      onError: (error: Error) => {
        alert(error.message || 'Failed to delete dependency');
      },
    });
  };

  if (isLoading) {
    return (
      <div className="ab-loading">
        <div className="ab-spinner"></div>
      </div>
    );
  }

  return (
    <div className="ab-dependencies-view">
      <div className="ab-toolbar">
        <h3 className="ab-section-title">Dependencies ({dependencies?.length || 0})</h3>
        <button className="ab-btn ab-btn-primary" onClick={() => setShowCreate(true)}>
          <span className="ab-icon-plus"></span>
          Add Dependency
        </button>
      </div>

      {dependencies && dependencies.length > 0 ? (
        <div className="ab-dependencies-list">
          {dependencies.map((dep) => (
            <div key={dep.id} className="ab-card ab-dependency-card">
              <div className="ab-dependency-content">
                <div className="ab-dependency-issue ab-dependency-blocking">
                  <span className="ab-dependency-label">Blocks</span>
                  <span className="ab-dependency-key">{dep.blockingIssueKey || dep.blockingIssueId}</span>
                  {dep.blockingIssueSummary && (
                    <span className="ab-dependency-summary">{dep.blockingIssueSummary}</span>
                  )}
                </div>
                <div className="ab-dependency-arrow">→</div>
                <div className="ab-dependency-issue ab-dependency-blocked">
                  <span className="ab-dependency-label">Blocked</span>
                  <span className="ab-dependency-key">{dep.blockedIssueKey || dep.blockedIssueId}</span>
                  {dep.blockedIssueSummary && (
                    <span className="ab-dependency-summary">{dep.blockedIssueSummary}</span>
                  )}
                </div>
              </div>
              <div className="ab-dependency-actions">
                <span className="ab-badge ab-badge-secondary">{dep.dependencyType}</span>
                {dep.isCircular && (
                  <span className="ab-badge ab-badge-danger">Circular</span>
                )}
                <button className="ab-btn ab-btn-sm ab-btn-danger" onClick={() => handleDelete(dep.id)}>
                  Remove
                </button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="ab-empty-state">
          <div className="ab-empty-state-icon">🔗</div>
          <h3 className="ab-empty-state-title">No dependencies</h3>
          <p className="ab-empty-state-description">
            Add dependencies to track which issues are blocking others
          </p>
        </div>
      )}

      {showCreate && (
        <div className="ab-modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="ab-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ab-modal-header">
              <h2 className="ab-modal-title">Add Dependency</h2>
              <button className="ab-btn-icon" onClick={() => setShowCreate(false)}>
                <span className="ab-icon-close"></span>
              </button>
            </div>
            <form onSubmit={handleCreate}>
              <div className="ab-modal-body">
                <p className="ab-text-muted" style={{ marginBottom: '1rem' }}>
                  Define which issue blocks which other issue.
                </p>
                <div className="ab-form-group">
                  <label className="ab-label">Blocking Issue ID *</label>
                  <input
                    type="text"
                    className="ab-input"
                    value={blockingIssueId}
                    onChange={(e) => setBlockingIssueId(e.target.value)}
                    placeholder="Enter blocking issue ID"
                    required
                  />
                </div>
                <div className="ab-form-group">
                  <label className="ab-label">Blocked Issue ID *</label>
                  <input
                    type="text"
                    className="ab-input"
                    value={blockedIssueId}
                    onChange={(e) => setBlockedIssueId(e.target.value)}
                    placeholder="Enter blocked issue ID"
                    required
                  />
                </div>
              </div>
              <div className="ab-modal-footer">
                <button type="button" className="ab-btn ab-btn-secondary" onClick={() => setShowCreate(false)}>
                  Cancel
                </button>
                <button type="submit" className="ab-btn ab-btn-primary" disabled={createMutation.isPending}>
                  {createMutation.isPending ? 'Adding...' : 'Add Dependency'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
