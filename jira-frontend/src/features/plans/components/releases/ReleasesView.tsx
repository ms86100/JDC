import { useMemo, useState } from 'react';
import { useReleases, useCreateRelease, useApproveRelease, useReleaseVersion, useDeleteRelease } from '../../hooks/useReleases';
import { useBacklog } from '../../hooks/useBacklog';
import { CreateReleaseRequest } from '../../../../api/planApi';
import { appNotify } from '../../../../lib/appNotify';

interface ReleasesViewProps {
  planId: string;
}

function projectKeyFromIssueKey(key?: string) {
  if (!key) return 'Unknown';
  const idx = key.indexOf('-');
  return idx > 0 ? key.slice(0, idx) : key;
}

export default function ReleasesView({ planId }: ReleasesViewProps) {
  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState('');
  const [version, setVersion] = useState('');
  const [releaseDate, setReleaseDate] = useState('');

  const { data: releases, isLoading } = useReleases(planId);
  const { data: backlog } = useBacklog(planId);
  const createMutation = useCreateRelease();
  const approveMutation = useApproveRelease();
  const releaseMutation = useReleaseVersion();
  const deleteMutation = useDeleteRelease();

  const projectRows = useMemo(() => {
    const byProject = new Map<string, { key: string; releases: typeof releases; issueCount: number }>();
    const items = backlog?.items ?? [];
    const projectsFromIssues = new Set(items.map((i) => projectKeyFromIssueKey(i.issueKey)));

    projectsFromIssues.forEach((pk) => {
      byProject.set(pk, {
        key: pk,
        releases: [],
        issueCount: items.filter((i) => projectKeyFromIssueKey(i.issueKey) === pk).length,
      });
    });

    (releases ?? []).forEach((r) => {
      const pk = projectKeyFromIssueKey(r.name) || 'Plan';
      const row = byProject.get(pk) ?? { key: pk, releases: [], issueCount: 0 };
      row.releases = [...(row.releases ?? []), r];
      byProject.set(pk, row);
    });

    if (byProject.size === 0 && (releases?.length ?? 0) > 0) {
      byProject.set('Plan', { key: 'Plan', releases: releases ?? [], issueCount: items.length });
    }

    return Array.from(byProject.values());
  }, [releases, backlog]);

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
        onError: (error: Error) => {
          appNotify.error(error.message || 'Failed to create release');
        },
      },
    );
  };

  if (isLoading) {
    return (
      <div className="ab-loading">
        <div className="ab-spinner" />
      </div>
    );
  }

  return (
    <div className="jdc-releases-dc">
      <div className="jdc-releases-toolbar">
        <h3>Releases</h3>
        <button type="button" className="jdc-btn jdc-btn-primary" onClick={() => setShowCreate(true)}>
          + Create release
        </button>
      </div>

      <table className="jdc-releases-cross-table">
        <thead>
          <tr>
            <th>Project</th>
            <th>Release name</th>
            <th>Release date</th>
            <th>Status</th>
            <th>Issues</th>
            <th>Progress</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {projectRows.flatMap((row) => {
            const list = row.releases?.length ? row.releases : [];
            if (list.length === 0) {
              return (
                <tr key={`empty-${row.key}`}>
                  <td><strong>{row.key}</strong></td>
                  <td colSpan={6} className="jdc-muted">No releases for this project</td>
                </tr>
              );
            }
            return list.map((release) => (
              <tr key={`${row.key}-${release.id}`}>
                <td><strong>{row.key}</strong></td>
                <td>{release.name}{release.version ? ` v${release.version}` : ''}</td>
                <td>
                  {release.releaseDate
                    ? new Date(release.releaseDate).toLocaleDateString()
                    : '—'}
                </td>
                <td>
                  <span className={`jdc-lozenge jdc-lozenge-${release.status.toLowerCase()}`}>
                    {release.status}
                  </span>
                </td>
                <td>{row.issueCount}</td>
                <td>
                  {release.progress != null ? (
                    <div className="jdc-progress-mini">
                      <div><div style={{ width: `${release.progress}%` }} /></div>
                      <span>{Math.round(release.progress)}%</span>
                    </div>
                  ) : (
                    '—'
                  )}
                </td>
                <td>
                  <div className="jdc-releases-actions">
                    {release.status === 'DRAFT' && (
                      <button type="button" className="jdc-btn" onClick={() => approveMutation.mutate({ planId, releaseId: release.id, approvedBy: 'user' })}>
                        Approve
                      </button>
                    )}
                    {release.status === 'APPROVED' && (
                      <button type="button" className="jdc-btn" onClick={() => releaseMutation.mutate({ planId, releaseId: release.id })}>
                        Release
                      </button>
                    )}
                    <button type="button" className="jdc-btn" onClick={() => deleteMutation.mutate({ planId, releaseId: release.id })}>
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ));
          })}
          {projectRows.length === 0 && (
            <tr>
              <td colSpan={7} className="jdc-empty-cell">
                No releases or cross-project data yet. Create a release or add issues from multiple projects.
              </td>
            </tr>
          )}
        </tbody>
      </table>

      {showCreate && (
        <div className="jdc-modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="jdc-modal" onClick={(e) => e.stopPropagation()}>
            <h3>Create release</h3>
            <form onSubmit={handleCreate}>
              <label>Name *<input value={name} onChange={(e) => setName(e.target.value)} required /></label>
              <label>Version<input value={version} onChange={(e) => setVersion(e.target.value)} /></label>
              <label>Date<input type="date" value={releaseDate} onChange={(e) => setReleaseDate(e.target.value)} /></label>
              <div className="jdc-modal-footer">
                <button type="button" className="jdc-btn" onClick={() => setShowCreate(false)}>Cancel</button>
                <button type="submit" className="jdc-btn jdc-btn-primary" disabled={createMutation.isPending}>Create</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
