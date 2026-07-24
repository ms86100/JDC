import { useState, useEffect, useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { vvoApi } from '../../../api/vvoApi';
import '../AircraftDesignStyles.css';

const VVO_STATUSES = ['ALL', 'NEW', 'TO_BE_VERIFIED', 'VERIFIED', 'RELEASED', 'CANCELLED', 'SUPERSEDED'] as const;

interface VvoRow {
  id: string;
  issueKey?: string;
  summary?: string;
  status?: string;
  fixVersionName?: string;
  idDoors?: string;
  applicability?: string;
  assigneeName?: string;
  hlvvoId?: string;
  createdAt?: string;
}

export default function VvoListPage() {
  const [searchParams] = useSearchParams();
  const projectId = searchParams.get('projectId') || '';

  const [vvos, setVvos] = useState<VvoRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [search, setSearch] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState({ projectId, summary: '', description: '' });
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    loadVvos();
  }, [projectId]);

  async function loadVvos() {
    setLoading(true);
    setError('');
    try {
      const res = projectId
        ? await vvoApi.getByProject(projectId)
        : await vvoApi.getByProject('default');
      const data = res.data;
      setVvos(Array.isArray(data) ? data : data?.content ?? []);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load VVOs');
    } finally {
      setLoading(false);
    }
  }

  const filtered = useMemo(() => {
    let result = vvos;
    if (statusFilter !== 'ALL') {
      result = result.filter(v => v.status?.toUpperCase() === statusFilter);
    }
    if (search.trim()) {
      const q = search.toLowerCase();
      result = result.filter(v =>
        (v.issueKey?.toLowerCase().includes(q)) ||
        (v.summary?.toLowerCase().includes(q)) ||
        (v.idDoors?.toLowerCase().includes(q))
      );
    }
    return result;
  }, [vvos, statusFilter, search]);

  async function handleClone(id: string) {
    if (!confirm('Clone this VVO?')) return;
    try {
      await vvoApi.clone(id);
      loadVvos();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Clone failed');
    }
  }

  async function handleArchive(id: string) {
    if (!confirm('Archive this VVO? This action cannot be undone.')) return;
    try {
      await vvoApi.archive(id);
      loadVvos();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Archive failed');
    }
  }

  async function handleCreate() {
    if (!createForm.summary.trim()) return;
    setCreating(true);
    try {
      await vvoApi.create({ ...createForm, projectId: createForm.projectId || projectId });
      setShowCreate(false);
      setCreateForm({ projectId, summary: '', description: '' });
      loadVvos();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Create failed');
    } finally {
      setCreating(false);
    }
  }

  if (loading) {
    return (
      <div className="ads-page">
        <div className="ads-loading"><div className="ab-spinner" /> Loading VVOs...</div>
      </div>
    );
  }

  return (
    <div className="ads-page">
      <div className="ads-page-header">
        <div>
          <h1 className="ads-page-title">Verification & Validation Objectives</h1>
          <p className="ads-page-subtitle">{filtered.length} VVO{filtered.length !== 1 ? 's' : ''} found</p>
        </div>
        <div className="ads-toolbar">
          <input
            className="ads-search-input"
            placeholder="Search by key, summary, or DOORS ID..."
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
          <button className="ads-btn ads-btn--primary" onClick={() => setShowCreate(true)}>
            + Create VVO
          </button>
        </div>
      </div>

      {error && <div className="ads-alert ads-alert--error">{error}</div>}

      <div className="ads-filters">
        {VVO_STATUSES.map(s => (
          <button
            key={s}
            className={`ads-filter-pill${statusFilter === s ? ' ads-filter-pill--active' : ''}`}
            onClick={() => setStatusFilter(s)}
          >
            {s.replace(/_/g, ' ')}
          </button>
        ))}
      </div>

      <div className="ads-table-wrap">
        <table className="ads-table">
          <thead>
            <tr>
              <th>Issue Key</th>
              <th>Summary</th>
              <th>Status</th>
              <th>Version</th>
              <th>ID Doors</th>
              <th>Applicability</th>
              <th>Assignee</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={8} className="ads-table-empty">No VVOs match current filters.</td>
              </tr>
            ) : (
              filtered.map(v => (
                <tr key={v.id}>
                  <td>
                    <Link className="ads-table-link" to={`/aircraft-design/vvos/${v.id}`}>
                      {v.issueKey || v.id.slice(0, 8)}
                    </Link>
                  </td>
                  <td>{v.summary || '-'}</td>
                  <td>
                    <span className={`ads-badge ads-badge--${(v.status || 'new').toLowerCase().replace(/\s+/g, '_')}`}>
                      {v.status || 'NEW'}
                    </span>
                  </td>
                  <td>{v.fixVersionName || '-'}</td>
                  <td>{v.idDoors || '-'}</td>
                  <td>{v.applicability || '-'}</td>
                  <td>{v.assigneeName || '-'}</td>
                  <td>
                    <div className="ads-table-actions">
                      <button className="ads-btn ads-btn--sm ads-btn--ghost" onClick={() => handleClone(v.id)} title="Clone">
                        Clone
                      </button>
                      <button className="ads-btn ads-btn--sm ads-btn--ghost" onClick={() => handleArchive(v.id)} title="Archive">
                        Archive
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {showCreate && (
        <div className="ads-modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="ads-modal" onClick={e => e.stopPropagation()}>
            <h2 className="ads-modal-title">Create VVO</h2>
            <div className="ads-field" style={{ marginBottom: 12 }}>
              <label className="ads-field-label">Project ID</label>
              <input
                className="ads-field-input"
                value={createForm.projectId}
                onChange={e => setCreateForm(f => ({ ...f, projectId: e.target.value }))}
                placeholder="Enter project ID"
              />
            </div>
            <div className="ads-field" style={{ marginBottom: 12 }}>
              <label className="ads-field-label">Summary *</label>
              <input
                className="ads-field-input"
                value={createForm.summary}
                onChange={e => setCreateForm(f => ({ ...f, summary: e.target.value }))}
                placeholder="VVO summary"
              />
            </div>
            <div className="ads-field" style={{ marginBottom: 12 }}>
              <label className="ads-field-label">Description</label>
              <textarea
                className="ads-field-textarea"
                value={createForm.description}
                onChange={e => setCreateForm(f => ({ ...f, description: e.target.value }))}
                placeholder="Optional description"
              />
            </div>
            <div className="ads-modal-actions">
              <button className="ads-btn" onClick={() => setShowCreate(false)}>Cancel</button>
              <button className="ads-btn ads-btn--primary" onClick={handleCreate} disabled={creating}>
                {creating ? 'Creating...' : 'Create'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
