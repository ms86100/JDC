import { useState, useEffect, useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { techEventApi } from '../../../api/defectApi';
import { DEMO_TECH_EVENTS } from '../demoData';
import '../AircraftDesignStyles.css';

const PIPELINE_STEPS = ['Open', 'Analysis', 'Resolver', 'Classified', 'Assessed', 'Resolved', 'Closed'] as const;

interface TechEventRow {
  id: string;
  issueKey?: string;
  summary?: string;
  status?: string;
  reporterTeam?: string;
  program?: string;
  priority?: string;
  createdAt?: string;
  assigneeName?: string;
}

export default function TechEventListPage() {
  const [searchParams] = useSearchParams();
  const projectId = searchParams.get('projectId') || '';

  const [events, setEvents] = useState<TechEventRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState({
    projectId,
    summary: '',
    description: '',
    reporterTeam: '',
    program: '',
  });
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    loadEvents();
  }, [projectId]);

  async function loadEvents() {
    setLoading(true);
    setError('');
    try {
      const res = await techEventApi.getByProject(projectId || 'default');
      const data = res.data;
      setEvents(Array.isArray(data) ? data : data?.content ?? []);
    } catch {
      setEvents(DEMO_TECH_EVENTS);
    } finally {
      setLoading(false);
    }
  }

  const filtered = useMemo(() => {
    let result = events;
    if (statusFilter !== 'ALL') {
      result = result.filter(e => e.status?.toLowerCase() === statusFilter.toLowerCase());
    }
    if (search.trim()) {
      const q = search.toLowerCase();
      result = result.filter(e =>
        (e.issueKey?.toLowerCase().includes(q)) ||
        (e.summary?.toLowerCase().includes(q)) ||
        (e.reporterTeam?.toLowerCase().includes(q))
      );
    }
    return result;
  }, [events, statusFilter, search]);

  function pipelineStepStatus(step: string, currentStatus?: string) {
    if (!currentStatus) return '';
    const currentIdx = PIPELINE_STEPS.findIndex(s => s.toLowerCase() === currentStatus.toLowerCase());
    const stepIdx = PIPELINE_STEPS.indexOf(step as typeof PIPELINE_STEPS[number]);
    if (stepIdx < 0 || currentIdx < 0) return '';
    if (stepIdx < currentIdx) return 'ads-pipeline-step--done';
    if (stepIdx === currentIdx) return 'ads-pipeline-step--active';
    return '';
  }

  async function handleCreate() {
    if (!createForm.summary.trim()) return;
    setCreating(true);
    try {
      await techEventApi.create({ ...createForm, projectId: createForm.projectId || projectId });
      setShowCreate(false);
      setCreateForm({ projectId, summary: '', description: '', reporterTeam: '', program: '' });
      loadEvents();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Create failed');
    } finally {
      setCreating(false);
    }
  }

  if (loading) {
    return (
      <div className="ads-page">
        <div className="ads-loading"><div className="ab-spinner" /> Loading Tech Events...</div>
      </div>
    );
  }

  return (
    <div className="ads-page">
      <div className="ads-page-header">
        <div>
          <h1 className="ads-page-title">Tech Events (M1668)</h1>
          <p className="ads-page-subtitle">{filtered.length} event{filtered.length !== 1 ? 's' : ''}</p>
        </div>
        <div className="ads-toolbar">
          <input
            className="ads-search-input"
            placeholder="Search events..."
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
          <button className="ads-btn ads-btn--primary" onClick={() => setShowCreate(true)}>
            + Create Tech Event
          </button>
        </div>
      </div>

      {error && <div className="ads-alert ads-alert--error">{error}</div>}

      {/* Pipeline visualization */}
      <div className="ads-pipeline">
        {PIPELINE_STEPS.map((step, i) => (
          <span key={step}>
            <span
              className={`ads-pipeline-step${statusFilter.toLowerCase() === step.toLowerCase() ? ' ads-pipeline-step--active' : ''}`}
              style={{ cursor: 'pointer' }}
              onClick={() => setStatusFilter(statusFilter.toLowerCase() === step.toLowerCase() ? 'ALL' : step)}
            >
              {step}
            </span>
            {i < PIPELINE_STEPS.length - 1 && <span className="ads-pipeline-arrow"> &rarr; </span>}
          </span>
        ))}
        <button
          className={`ads-filter-pill${statusFilter === 'ALL' ? ' ads-filter-pill--active' : ''}`}
          style={{ marginLeft: 12 }}
          onClick={() => setStatusFilter('ALL')}
        >
          Show All
        </button>
      </div>

      <div className="ads-table-wrap">
        <table className="ads-table">
          <thead>
            <tr>
              <th>Issue Key</th>
              <th>Summary</th>
              <th>Status</th>
              <th>Reporter Team</th>
              <th>Program</th>
              <th>Priority</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={8} className="ads-table-empty">No Tech Events match current filters.</td>
              </tr>
            ) : (
              filtered.map(ev => (
                <tr key={ev.id}>
                  <td>
                    <Link className="ads-table-link" to={`/aircraft-design/tech-events/${ev.id}`}>
                      {ev.issueKey || ev.id.slice(0, 8)}
                    </Link>
                  </td>
                  <td>{ev.summary || '-'}</td>
                  <td>
                    <span className={`ads-badge ads-badge--${(ev.status || 'open').toLowerCase().replace(/\s+/g, '_')}`}>
                      {ev.status || 'OPEN'}
                    </span>
                  </td>
                  <td>{ev.reporterTeam || '-'}</td>
                  <td>{ev.program || '-'}</td>
                  <td>{ev.priority || '-'}</td>
                  <td>{ev.createdAt ? new Date(ev.createdAt).toLocaleDateString() : '-'}</td>
                  <td>
                    <Link className="ads-btn ads-btn--sm ads-btn--ghost" to={`/aircraft-design/tech-events/${ev.id}`}>
                      View
                    </Link>
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
            <h2 className="ads-modal-title">Create Tech Event</h2>
            <div className="ads-field" style={{ marginBottom: 12 }}>
              <label className="ads-field-label">Project ID</label>
              <input className="ads-field-input" value={createForm.projectId}
                onChange={e => setCreateForm(f => ({ ...f, projectId: e.target.value }))} />
            </div>
            <div className="ads-field" style={{ marginBottom: 12 }}>
              <label className="ads-field-label">Summary *</label>
              <input className="ads-field-input" value={createForm.summary}
                onChange={e => setCreateForm(f => ({ ...f, summary: e.target.value }))} />
            </div>
            <div className="ads-field" style={{ marginBottom: 12 }}>
              <label className="ads-field-label">Reporter Team</label>
              <input className="ads-field-input" value={createForm.reporterTeam}
                onChange={e => setCreateForm(f => ({ ...f, reporterTeam: e.target.value }))} />
            </div>
            <div className="ads-field" style={{ marginBottom: 12 }}>
              <label className="ads-field-label">Program</label>
              <input className="ads-field-input" value={createForm.program}
                onChange={e => setCreateForm(f => ({ ...f, program: e.target.value }))} />
            </div>
            <div className="ads-field" style={{ marginBottom: 12 }}>
              <label className="ads-field-label">Description</label>
              <textarea className="ads-field-textarea" value={createForm.description}
                onChange={e => setCreateForm(f => ({ ...f, description: e.target.value }))} />
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
