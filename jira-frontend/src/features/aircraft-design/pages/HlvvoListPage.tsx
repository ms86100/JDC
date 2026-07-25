import { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { hlvvoApi } from '../../../api/vvoApi';
import { DEMO_HLVVOS, DEMO_HLVVO_CHILDREN } from '../demoData';
import '../AircraftDesignStyles.css';

interface HlvvoRow {
  id: string;
  issueKey?: string;
  summary?: string;
  status?: string;
  projectId?: string;
  childVvoCount?: number;
  createdAt?: string;
  assigneeName?: string;
}

export default function HlvvoListPage() {
  const [searchParams] = useSearchParams();
  const projectId = searchParams.get('projectId') || '';

  const [hlvvos, setHlvvos] = useState<HlvvoRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState({ projectId, summary: '', description: '' });
  const [creating, setCreating] = useState(false);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [childVvos, setChildVvos] = useState<any[]>([]);
  const [childLoading, setChildLoading] = useState(false);

  useEffect(() => {
    loadHlvvos();
  }, [projectId]);

  async function loadHlvvos() {
    setLoading(true);
    setError('');
    try {
      const res = await hlvvoApi.getByProject(projectId || 'default');
      const data = res.data;
      setHlvvos(Array.isArray(data) ? data : data?.content ?? []);
    } catch {
      setHlvvos(DEMO_HLVVOS);
    } finally {
      setLoading(false);
    }
  }

  async function toggleExpand(id: string) {
    if (expandedId === id) {
      setExpandedId(null);
      setChildVvos([]);
      return;
    }
    setExpandedId(id);
    setChildLoading(true);
    try {
      const res = await hlvvoApi.getChildVvos(id);
      setChildVvos(Array.isArray(res.data) ? res.data : []);
    } catch {
      setChildVvos(DEMO_HLVVO_CHILDREN[id] || []);
    } finally {
      setChildLoading(false);
    }
  }

  async function handleCreate() {
    if (!createForm.summary.trim()) return;
    setCreating(true);
    try {
      await hlvvoApi.create({ ...createForm, projectId: createForm.projectId || projectId });
      setShowCreate(false);
      setCreateForm({ projectId, summary: '', description: '' });
      loadHlvvos();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Create failed');
    } finally {
      setCreating(false);
    }
  }

  const filtered = hlvvos.filter(h => {
    if (!search.trim()) return true;
    const q = search.toLowerCase();
    return (h.issueKey?.toLowerCase().includes(q)) || (h.summary?.toLowerCase().includes(q));
  });

  if (loading) {
    return (
      <div className="ads-page">
        <div className="ads-loading"><div className="ab-spinner" /> Loading HLVVOs...</div>
      </div>
    );
  }

  return (
    <div className="ads-page">
      <div className="ads-page-header">
        <div>
          <h1 className="ads-page-title">High Level VVOs</h1>
          <p className="ads-page-subtitle">{filtered.length} HLVVO{filtered.length !== 1 ? 's' : ''}</p>
        </div>
        <div className="ads-toolbar">
          <input
            className="ads-search-input"
            placeholder="Search HLVVOs..."
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
          <button className="ads-btn ads-btn--primary" onClick={() => setShowCreate(true)}>
            + Create HLVVO
          </button>
        </div>
      </div>

      {error && <div className="ads-alert ads-alert--error">{error}</div>}

      <div className="ads-table-wrap">
        <table className="ads-table">
          <thead>
            <tr>
              <th style={{ width: 30 }} />
              <th>Issue Key</th>
              <th>Summary</th>
              <th>Status</th>
              <th>Child VVOs</th>
              <th>Assignee</th>
              <th>Created</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={7} className="ads-table-empty">No HLVVOs found.</td>
              </tr>
            ) : (
              filtered.map(h => (
                <>
                  <tr key={h.id}>
                    <td>
                      <button
                        className="ads-btn ads-btn--sm ads-btn--ghost"
                        onClick={() => toggleExpand(h.id)}
                        style={{ padding: '2px 6px' }}
                      >
                        {expandedId === h.id ? '▼' : '▶'}
                      </button>
                    </td>
                    <td>
                      <span className="ads-table-link" style={{ cursor: 'default' }}>
                        {h.issueKey || h.id.slice(0, 8)}
                      </span>
                    </td>
                    <td>{h.summary || '-'}</td>
                    <td>
                      <span className={`ads-badge ads-badge--${(h.status || 'new').toLowerCase().replace(/\s+/g, '_')}`}>
                        {h.status || 'NEW'}
                      </span>
                    </td>
                    <td>{h.childVvoCount ?? '-'}</td>
                    <td>{h.assigneeName || '-'}</td>
                    <td>{h.createdAt ? new Date(h.createdAt).toLocaleDateString() : '-'}</td>
                  </tr>
                  {expandedId === h.id && (
                    <tr key={`${h.id}-children`}>
                      <td colSpan={7} style={{ background: '#fafbfc', padding: '8px 14px 8px 40px' }}>
                        {childLoading ? (
                          <div className="ads-loading" style={{ padding: '10px 0' }}>
                            <div className="ab-spinner" /> Loading child VVOs...
                          </div>
                        ) : childVvos.length === 0 ? (
                          <span style={{ color: '#97a0af', fontStyle: 'italic', fontSize: 13 }}>No child VVOs</span>
                        ) : (
                          <table className="ads-table" style={{ border: 'none' }}>
                            <thead>
                              <tr>
                                <th>Key</th>
                                <th>Summary</th>
                                <th>Status</th>
                              </tr>
                            </thead>
                            <tbody>
                              {childVvos.map((cv: any) => (
                                <tr key={cv.id}>
                                  <td>
                                    <Link className="ads-table-link" to={`/aircraft-design/vvos/${cv.id}`}>
                                      {cv.issueKey || cv.id.slice(0, 8)}
                                    </Link>
                                  </td>
                                  <td>{cv.summary || '-'}</td>
                                  <td>
                                    <span className={`ads-badge ads-badge--${(cv.status || 'new').toLowerCase().replace(/\s+/g, '_')}`}>
                                      {cv.status || 'NEW'}
                                    </span>
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        )}
                      </td>
                    </tr>
                  )}
                </>
              ))
            )}
          </tbody>
        </table>
      </div>

      {showCreate && (
        <div className="ads-modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="ads-modal" onClick={e => e.stopPropagation()}>
            <h2 className="ads-modal-title">Create HLVVO</h2>
            <div className="ads-field" style={{ marginBottom: 12 }}>
              <label className="ads-field-label">Project ID</label>
              <input
                className="ads-field-input"
                value={createForm.projectId}
                onChange={e => setCreateForm(f => ({ ...f, projectId: e.target.value }))}
              />
            </div>
            <div className="ads-field" style={{ marginBottom: 12 }}>
              <label className="ads-field-label">Summary *</label>
              <input
                className="ads-field-input"
                value={createForm.summary}
                onChange={e => setCreateForm(f => ({ ...f, summary: e.target.value }))}
              />
            </div>
            <div className="ads-field" style={{ marginBottom: 12 }}>
              <label className="ads-field-label">Description</label>
              <textarea
                className="ads-field-textarea"
                value={createForm.description}
                onChange={e => setCreateForm(f => ({ ...f, description: e.target.value }))}
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
