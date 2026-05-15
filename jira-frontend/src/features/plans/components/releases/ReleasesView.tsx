import { useState } from 'react';
import { useReleases, useCreateRelease, useApproveRelease, useReleaseVersion, useDeleteRelease } from '../../hooks/useReleases';
import { CreateReleaseRequest } from '../../../../api/planApi';

interface ReleasesViewProps {
  planId: string;
}

export default function ReleasesView({ planId }: ReleasesViewProps) {
  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState('');
  const [version, setVersion] = useState('');
  const [releaseDate, setReleaseDate] = useState('');

  const { data: releases, isLoading } = useReleases(planId);
  const createMutation = useCreateRelease();
  const approveMutation = useApproveRelease();
  const releaseMutation = useReleaseVersion();
  const deleteMutation = useDeleteRelease();

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    const request: CreateReleaseRequest = {
      name,
      version: version || undefined,
      releaseDate: releaseDate || undefined,
    };
    createMutation.mutate(
      { planId, data: request },
      {
        onSuccess: () => {
          setShowCreate(false);
          setName('');
          setVersion('');
          setReleaseDate('');
        },
      }
    );
  };

  const handleApprove = (releaseId: string) => {
    approveMutation.mutate({ planId, releaseId, approvedBy: 'current-user' });
  };

  const handleRelease = (releaseId: string) => {
    releaseMutation.mutate({ planId, releaseId });
  };

  const handleDelete = (releaseId: string) => {
    if (confirm('Delete this release?')) {
      deleteMutation.mutate({ planId, releaseId });
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'DRAFT': return 'ab-badge-secondary';
      case 'APPROVED': return 'ab-badge-warning';
      case 'RELEASED': return 'ab-badge-success';
      default: return 'ab-badge-secondary';
    }
  };

  if (isLoading) {
    return (
      <div className="ab-loading">
        <div className="ab-spinner"></div>
      </div>
    );
  }

  return (
    <div className="ab-releases-view">
      <div className="ab-toolbar">
        <h3 className="ab-section-title">Releases ({releases?.length || 0})</h3>
        <button className="ab-btn ab-btn-primary" onClick={() => setShowCreate(true)}>
          <span className="ab-icon-plus"></span>
          Create Release
        </button>
      </div>

      {releases && releases.length > 0 ? (
        <div className="ab-releases-list">
          {releases.map((release) => (
            <div key={release.id} className="ab-card ab-release-card">
              <div className="ab-release-header">
                <div className="ab-release-info">
                  <h4 className="ab-release-name">
                    {release.name}
                    {release.version && <span className="ab-release-version">v{release.version}</span>}
                  </h4>
                  <span className={`ab-badge ${getStatusColor(release.status)}`}>
                    {release.status}
                  </span>
                </div>
                <div className="ab-release-actions">
                  {release.status === 'DRAFT' && (
                    <button className="ab-btn ab-btn-sm" onClick={() => handleApprove(release.id)}>
                      Approve
                    </button>
                  )}
                  {release.status === 'APPROVED' && (
                    <button className="ab-btn ab-btn-sm ab-btn-primary" onClick={() => handleRelease(release.id)}>
                      Release
                    </button>
                  )}
                  <button className="ab-btn ab-btn-sm ab-btn-danger" onClick={() => handleDelete(release.id)}>
                    Delete
                  </button>
                </div>
              </div>
              <div className="ab-release-details">
                {release.releaseDate && (
                  <div className="ab-release-date">
                    <span className="ab-label">Release Date:</span>
                    <span>{new Date(release.releaseDate).toLocaleDateString()}</span>
                  </div>
                )}
                {release.approvedBy && (
                  <div className="ab-release-approved">
                    <span className="ab-label">Approved by:</span>
                    <span>{release.approvedByName || release.approvedBy}</span>
                  </div>
                )}
                {release.progress !== undefined && (
                  <div className="ab-release-progress">
                    <div className="ab-progress-bar">
                      <div className="ab-progress-fill" style={{ width: `${release.progress}%` }}></div>
                    </div>
                    <span>{Math.round(release.progress)}% complete</span>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="ab-empty-state">
          <div className="ab-empty-state-icon">🚀</div>
          <h3 className="ab-empty-state-title">No releases yet</h3>
          <p className="ab-empty-state-description">Create releases to track your planned versions</p>
        </div>
      )}

      {showCreate && (
        <div className="ab-modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="ab-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ab-modal-header">
              <h2 className="ab-modal-title">Create Release</h2>
              <button className="ab-btn-icon" onClick={() => setShowCreate(false)}>
                <span className="ab-icon-close"></span>
              </button>
            </div>
            <form onSubmit={handleCreate}>
              <div className="ab-modal-body">
                <div className="ab-form-group">
                  <label className="ab-label">Release Name *</label>
                  <input
                    type="text"
                    className="ab-input"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="e.g., Q1 2026 Release"
                    required
                  />
                </div>
                <div className="ab-form-group">
                  <label className="ab-label">Version</label>
                  <input
                    type="text"
                    className="ab-input"
                    value={version}
                    onChange={(e) => setVersion(e.target.value)}
                    placeholder="e.g., 1.0.0"
                  />
                </div>
                <div className="ab-form-group">
                  <label className="ab-label">Release Date</label>
                  <input
                    type="date"
                    className="ab-input"
                    value={releaseDate}
                    onChange={(e) => setReleaseDate(e.target.value)}
                  />
                </div>
              </div>
              <div className="ab-modal-footer">
                <button type="button" className="ab-btn ab-btn-secondary" onClick={() => setShowCreate(false)}>
                  Cancel
                </button>
                <button type="submit" className="ab-btn ab-btn-primary" disabled={createMutation.isPending}>
                  {createMutation.isPending ? 'Creating...' : 'Create Release'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
