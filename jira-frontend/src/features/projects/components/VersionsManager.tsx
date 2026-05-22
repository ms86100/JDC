import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  versionApi,
  VersionResponse,
  CreateVersionRequest,
  UpdateVersionRequest,
} from '../../../api/versionApi';
import { appNotify } from '../../../lib/appNotify';
import ProjectLoadError from './ProjectLoadError';
import '../styles/project-releases-components.css';

type FilterTab = 'all' | 'unreleased' | 'released' | 'archived';

function formatDate(value?: string): string {
  if (!value) return '—';
  try {
    return new Date(value).toLocaleDateString();
  } catch {
    return '—';
  }
}

function progressPct(v: VersionResponse): number {
  if (v.progressPercentage != null && !Number.isNaN(v.progressPercentage)) {
    return Math.min(100, Math.max(0, v.progressPercentage));
  }
  const total = v.issueCount ?? 0;
  const done = v.completedIssueCount ?? 0;
  if (total <= 0) return 0;
  return Math.round((done / total) * 100);
}

interface VersionFormState {
  name: string;
  description: string;
  startDate: string;
  releaseDate: string;
}

const emptyForm = (): VersionFormState => ({
  name: '',
  description: '',
  startDate: '',
  releaseDate: '',
});

function toDateInput(iso?: string): string {
  if (!iso) return '';
  return iso.slice(0, 10);
}

function dateToIso(date: string): string | undefined {
  if (!date) return undefined;
  return `${date}T00:00:00`;
}

interface VersionsManagerProps {
  projectId: string;
  projectKey?: string;
  /** Compact mode for settings sidebar */
  variant?: 'hub' | 'settings';
}

export default function VersionsManager({
  projectId,
  projectKey,
  variant = 'hub',
}: VersionsManagerProps) {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<FilterTab>('all');
  const [search, setSearch] = useState('');
  const [showArchived, setShowArchived] = useState(false);
  const [modal, setModal] = useState<'create' | 'edit' | null>(null);
  const [editing, setEditing] = useState<VersionResponse | null>(null);
  const [form, setForm] = useState<VersionFormState>(emptyForm());
  const [releaseTarget, setReleaseTarget] = useState<VersionResponse | null>(null);
  const [detailVersion, setDetailVersion] = useState<VersionResponse | null>(null);
  const [mergeSource, setMergeSource] = useState<string>('');
  const [mergeTarget, setMergeTarget] = useState<string>('');
  const [showMerge, setShowMerge] = useState(false);

  const { data: versions = [], isPending, isError, refetch } = useQuery({
    queryKey: ['project-versions', projectId, showArchived],
    queryFn: () => versionApi.getByProject(projectId, showArchived),
    enabled: !!projectId,
    retry: 1,
  });

  const filtered = useMemo(() => {
    let list = versions;
    if (filter === 'unreleased') list = list.filter((v) => !v.released && !v.archived);
    if (filter === 'released') list = list.filter((v) => v.released && !v.archived);
    if (filter === 'archived') list = list.filter((v) => v.archived);
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(
        (v) =>
          v.name.toLowerCase().includes(q) ||
          (v.description ?? '').toLowerCase().includes(q),
      );
    }
    return [...list].sort((a, b) => (a.sequence ?? 0) - (b.sequence ?? 0) || a.name.localeCompare(b.name));
  }, [versions, filter, search]);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['project-versions', projectId] });
  };

  const saveMutation = useMutation({
    mutationFn: async () => {
      if (modal === 'create') {
        const body: CreateVersionRequest = {
          projectId,
          name: form.name.trim(),
          description: form.description.trim() || undefined,
          startDate: dateToIso(form.startDate),
          releaseDate: dateToIso(form.releaseDate),
        };
        return versionApi.create(body);
      }
      if (editing) {
        const body: UpdateVersionRequest = {
          name: form.name.trim(),
          description: form.description.trim() || undefined,
          startDate: dateToIso(form.startDate),
          releaseDate: dateToIso(form.releaseDate),
        };
        return versionApi.update(editing.id, body);
      }
      throw new Error('No version to save');
    },
    onSuccess: () => {
      appNotify.success(modal === 'create' ? 'Version created' : 'Version updated');
      setModal(null);
      setEditing(null);
      setForm(emptyForm());
      invalidate();
    },
    onError: (e: Error) => appNotify.error(e.message || 'Failed to save version'),
  });

  const releaseMutation = useMutation({
    mutationFn: (id: string) => versionApi.release(id, {}),
    onSuccess: () => {
      appNotify.success('Version released');
      setReleaseTarget(null);
      invalidate();
    },
    onError: (e: Error) => appNotify.error(e.message || 'Failed to release version'),
  });

  const archiveMutation = useMutation({
    mutationFn: (id: string) => versionApi.archive(id),
    onSuccess: () => {
      appNotify.success('Version archived');
      invalidate();
    },
    onError: (e: Error) => appNotify.error(e.message || 'Failed to archive version'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => versionApi.delete(id),
    onSuccess: () => {
      appNotify.success('Version deleted');
      setDetailVersion(null);
      invalidate();
    },
    onError: (e: Error) => appNotify.error(e.message || 'Failed to delete version'),
  });

  const unarchiveMutation = useMutation({
    mutationFn: (id: string) => versionApi.unarchive(id),
    onSuccess: () => {
      appNotify.success('Version unarchived');
      invalidate();
    },
    onError: (e: Error) => appNotify.error(e.message || 'Failed to unarchive version'),
  });

  const mergeMutation = useMutation({
    mutationFn: () =>
      versionApi.merge({
        sourceVersionId: mergeSource,
        targetVersionId: mergeTarget,
      }),
    onSuccess: () => {
      appNotify.success('Versions merged');
      setShowMerge(false);
      setMergeSource('');
      setMergeTarget('');
      invalidate();
    },
    onError: (e: Error) => appNotify.error(e.message || 'Failed to merge versions'),
  });

  const metricsMutation = useMutation({
    mutationFn: (id: string) => versionApi.recordMetricsSnapshot(id),
    onSuccess: () => {
      appNotify.success('Progress snapshot recorded');
      invalidate();
      if (detailVersion) {
        versionApi.getById(detailVersion.id).then(setDetailVersion);
      }
    },
    onError: (e: Error) => appNotify.error(e.message || 'Failed to record metrics'),
  });

  const notesMutation = useMutation({
    mutationFn: (id: string) => versionApi.generateReleaseNotes(id),
    onSuccess: () => {
      appNotify.success('Release notes generated');
    },
    onError: (e: Error) => appNotify.error(e.message || 'Failed to generate release notes'),
  });

  const openCreate = () => {
    setForm(emptyForm());
    setEditing(null);
    setModal('create');
  };

  const openEdit = (v: VersionResponse) => {
    setEditing(v);
    setForm({
      name: v.name,
      description: v.description ?? '',
      startDate: toDateInput(v.startDate),
      releaseDate: toDateInput(v.releaseDate),
    });
    setModal('edit');
  };

  const issuesJql = (versionName: string) =>
    `/projects/${projectId}/issues?jql=${encodeURIComponent(
      `project = ${projectKey ?? projectId} AND fixVersion = "${versionName}"`,
    )}`;

  if (isPending) {
    return <div className="ab-loading"><div className="ab-spinner" /></div>;
  }

  if (isError) {
    return (
      <ProjectLoadError
        title="Releases could not be loaded"
        message="Start jira-version-service (port 8096) and restart the gateway, then retry."
        onRetry={() => refetch()}
      />
    );
  }

  return (
    <>
      <div className="jdc-rc-toolbar">
        <input
          type="search"
          className="jdc-input"
          placeholder="Search versions…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          aria-label="Search versions"
        />
        <div className="jdc-rc-filter-tabs" role="tablist">
          {(['all', 'unreleased', 'released', 'archived'] as FilterTab[]).map((tab) => (
            <button
              key={tab}
              type="button"
              role="tab"
              className={`jdc-rc-filter-tab${filter === tab ? ' active' : ''}`}
              onClick={() => setFilter(tab)}
            >
              {tab === 'all' ? 'All' : tab.charAt(0).toUpperCase() + tab.slice(1)}
            </button>
          ))}
        </div>
        <label className="jdc-muted" style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13 }}>
          <input
            type="checkbox"
            checked={showArchived}
            onChange={(e) => setShowArchived(e.target.checked)}
          />
          Include archived
        </label>
        <button
          type="button"
          className="jdc-btn jdc-btn-secondary"
          onClick={() => setShowMerge(true)}
          disabled={versions.filter((v) => !v.archived).length < 2}
        >
          Merge versions
        </button>
        <button type="button" className="jdc-btn jdc-btn-primary" onClick={openCreate}>
          Create version
        </button>
      </div>

      {filtered.length === 0 ? (
        <div className="sa-project-subpage-empty">
          <p>
            {versions.length === 0
              ? 'No versions yet. Create a version to track releases and fix versions on issues.'
              : 'No versions match your filters.'}
          </p>
          {versions.length === 0 && (
            <button type="button" className="jdc-btn jdc-btn-primary" onClick={openCreate}>
              Create version
            </button>
          )}
        </div>
      ) : (
        <div className="sa-project-subpage-table-wrap jdc-rc-table">
          <table className="jdc-settings-table">
            <thead>
              <tr>
                <th>Version</th>
                <th>Status</th>
                <th>Progress</th>
                <th>Start</th>
                <th>Release date</th>
                <th>Issues</th>
                <th style={{ width: 220 }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((v) => {
                const pct = progressPct(v);
                return (
                  <tr key={v.id}>
                    <td>
                      <button
                        type="button"
                        className="jdc-link"
                        style={{ fontWeight: 600, background: 'none', border: 'none', padding: 0, cursor: 'pointer' }}
                        onClick={() => setDetailVersion(v)}
                      >
                        {v.name}
                      </button>
                      {v.description && (
                        <div className="jdc-muted" style={{ fontSize: 12, marginTop: 2 }}>
                          {v.description}
                        </div>
                      )}
                    </td>
                    <td>
                      {v.archived ? (
                        <span className="jdc-rc-status jdc-rc-status--archived">Archived</span>
                      ) : v.released ? (
                        <span className="jdc-rc-status jdc-rc-status--released">Released</span>
                      ) : (
                        <span className="jdc-rc-status jdc-rc-status--unreleased">Unreleased</span>
                      )}
                    </td>
                    <td className="jdc-rc-progress">
                      <span style={{ fontSize: 12 }}>{pct}%</span>
                      <div className="jdc-rc-progress-bar">
                        <div
                          className={`jdc-rc-progress-fill${pct >= 100 ? ' jdc-rc-progress-fill--done' : ''}`}
                          style={{ width: `${pct}%` }}
                        />
                      </div>
                    </td>
                    <td>{formatDate(v.startDate)}</td>
                    <td>{formatDate(v.releaseDate ?? v.actualReleaseDate)}</td>
                    <td>
                      {v.completedIssueCount ?? 0} / {v.issueCount ?? 0}
                      {v.unresolvedIssueCount != null && v.unresolvedIssueCount > 0 && (
                        <span className="jdc-muted" style={{ fontSize: 11 }}>
                          {' '}
                          ({v.unresolvedIssueCount} open)
                        </span>
                      )}
                    </td>
                    <td>
                        <div className="jdc-rc-actions">
                          {!v.released && !v.archived && (
                            <button
                              type="button"
                              className="jdc-btn jdc-btn-primary jdc-btn-sm"
                              onClick={() => setReleaseTarget(v)}
                            >
                              Release
                            </button>
                          )}
                          <button
                            type="button"
                            className="jdc-btn jdc-btn-secondary jdc-btn-sm"
                            onClick={() => openEdit(v)}
                          >
                            Edit
                          </button>
                          {v.archived ? (
                            <button
                              type="button"
                              className="jdc-btn jdc-btn-secondary jdc-btn-sm"
                              onClick={() => unarchiveMutation.mutate(v.id)}
                            >
                              Unarchive
                            </button>
                          ) : (
                            <button
                              type="button"
                              className="jdc-btn jdc-btn-secondary jdc-btn-sm"
                              onClick={() => archiveMutation.mutate(v.id)}
                            >
                              Archive
                            </button>
                          )}
                          {projectKey && (
                            <Link to={issuesJql(v.name)} className="jdc-btn jdc-btn-secondary jdc-btn-sm">
                              Issues
                            </Link>
                          )}
                          <button
                            type="button"
                            className="jdc-btn jdc-btn-secondary jdc-btn-sm"
                            onClick={() => {
                              if (window.confirm(`Delete version "${v.name}"?`)) {
                                deleteMutation.mutate(v.id);
                              }
                            }}
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {modal && (
        <div className="jdc-rc-modal-overlay" role="dialog" aria-modal="true">
          <div className="jdc-rc-modal">
            <div className="jdc-rc-modal-head">
              <h2>{modal === 'create' ? 'Create version' : 'Edit version'}</h2>
            </div>
            <div className="jdc-rc-modal-body">
              <div className="jdc-rc-form-row">
                <label htmlFor="ver-name">Name *</label>
                <input
                  id="ver-name"
                  className="jdc-input"
                  value={form.name}
                  onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                />
              </div>
              <div className="jdc-rc-form-row">
                <label htmlFor="ver-desc">Description</label>
                <textarea
                  id="ver-desc"
                  className="jdc-input"
                  value={form.description}
                  onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                />
              </div>
              <div className="jdc-rc-form-row">
                <label htmlFor="ver-start">Start date</label>
                <input
                  id="ver-start"
                  type="date"
                  className="jdc-input"
                  value={form.startDate}
                  onChange={(e) => setForm((f) => ({ ...f, startDate: e.target.value }))}
                />
              </div>
              <div className="jdc-rc-form-row">
                <label htmlFor="ver-release">Release date</label>
                <input
                  id="ver-release"
                  type="date"
                  className="jdc-input"
                  value={form.releaseDate}
                  onChange={(e) => setForm((f) => ({ ...f, releaseDate: e.target.value }))}
                />
              </div>
            </div>
            <div className="jdc-rc-modal-foot">
              <button
                type="button"
                className="jdc-btn jdc-btn-secondary"
                onClick={() => {
                  setModal(null);
                  setEditing(null);
                }}
              >
                Cancel
              </button>
              <button
                type="button"
                className="jdc-btn jdc-btn-primary"
                disabled={!form.name.trim() || saveMutation.isPending}
                onClick={() => saveMutation.mutate()}
              >
                {saveMutation.isPending ? 'Saving…' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}

      {showMerge && (
        <div className="jdc-rc-modal-overlay" role="dialog" aria-modal="true">
          <div className="jdc-rc-modal">
            <div className="jdc-rc-modal-head"><h2>Merge versions</h2></div>
            <div className="jdc-rc-modal-body">
              <p className="jdc-muted">Move fix-version links from source into target, then remove source.</p>
              <div className="jdc-rc-form-row">
                <label>Source (will be merged away)</label>
                <select className="jdc-input" value={mergeSource} onChange={(e) => setMergeSource(e.target.value)}>
                  <option value="">Select…</option>
                  {versions.filter((v) => !v.archived && v.id !== mergeTarget).map((v) => (
                    <option key={v.id} value={v.id}>{v.name}</option>
                  ))}
                </select>
              </div>
              <div className="jdc-rc-form-row">
                <label>Target</label>
                <select className="jdc-input" value={mergeTarget} onChange={(e) => setMergeTarget(e.target.value)}>
                  <option value="">Select…</option>
                  {versions.filter((v) => !v.archived && v.id !== mergeSource).map((v) => (
                    <option key={v.id} value={v.id}>{v.name}</option>
                  ))}
                </select>
              </div>
            </div>
            <div className="jdc-rc-modal-foot">
              <button type="button" className="jdc-btn jdc-btn-secondary" onClick={() => setShowMerge(false)}>Cancel</button>
              <button
                type="button"
                className="jdc-btn jdc-btn-primary"
                disabled={!mergeSource || !mergeTarget || mergeMutation.isPending}
                onClick={() => mergeMutation.mutate()}
              >
                {mergeMutation.isPending ? 'Merging…' : 'Merge'}
              </button>
            </div>
          </div>
        </div>
      )}

      {detailVersion && (
        <div className="jdc-rc-modal-overlay" role="dialog" aria-modal="true">
          <div className="jdc-rc-modal" style={{ maxWidth: 520 }}>
            <div className="jdc-rc-modal-head">
              <h2>{detailVersion.name}</h2>
            </div>
            <div className="jdc-rc-modal-body">
              <p className="jdc-muted">{detailVersion.description || 'No description'}</p>
              <div className="ws-entity-card-metrics">
                <div className="ws-mini-metric"><span>Issues</span><strong>{detailVersion.issueCount ?? 0}</strong></div>
                <div className="ws-mini-metric"><span>Progress</span><strong>{progressPct(detailVersion)}%</strong></div>
                <div className="ws-mini-metric"><span>Status</span><strong>{detailVersion.released ? 'Released' : 'Unreleased'}</strong></div>
              </div>
            </div>
            <div className="jdc-rc-modal-foot" style={{ flexWrap: 'wrap' }}>
              <button type="button" className="jdc-btn jdc-btn-secondary" onClick={() => setDetailVersion(null)}>Close</button>
              <button type="button" className="jdc-btn jdc-btn-secondary" onClick={() => metricsMutation.mutate(detailVersion.id)}>Refresh progress</button>
              <button type="button" className="jdc-btn jdc-btn-secondary" onClick={() => notesMutation.mutate(detailVersion.id)}>Generate release notes</button>
              <button type="button" className="jdc-btn jdc-btn-secondary" onClick={() => { openEdit(detailVersion); setDetailVersion(null); }}>Edit</button>
            </div>
          </div>
        </div>
      )}

      {releaseTarget && (
        <div className="jdc-rc-modal-overlay" role="dialog" aria-modal="true">
          <div className="jdc-rc-modal">
            <div className="jdc-rc-modal-head">
              <h2>Release version</h2>
            </div>
            <div className="jdc-rc-modal-body">
              <p>
                Release <strong>{releaseTarget.name}</strong>? Unresolved issues can remain linked;
                you can move them later from the version settings.
              </p>
            </div>
            <div className="jdc-rc-modal-foot">
              <button
                type="button"
                className="jdc-btn jdc-btn-secondary"
                onClick={() => setReleaseTarget(null)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="jdc-btn jdc-btn-primary"
                disabled={releaseMutation.isPending}
                onClick={() => releaseMutation.mutate(releaseTarget.id)}
              >
                {releaseMutation.isPending ? 'Releasing…' : 'Release'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
