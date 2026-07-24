import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { techEventApi } from '../../../api/defectApi';
import { masterDataApi } from '../../../api/masterDataApi';
import '../AircraftDesignStyles.css';

const PIPELINE_STEPS = ['Open', 'Analysis', 'Resolver', 'Classified', 'Assessed', 'Resolved', 'Closed'];

interface TechEventDetail {
  id: string;
  issueKey?: string;
  summary?: string;
  description?: string;
  status?: string;
  projectId?: string;
  programId?: string;
  programName?: string;
  testMeanId?: string;
  testMeanName?: string;
  systemId?: string;
  systemName?: string;
  ataChapter?: string;
  reporterTeam?: string;
  assigneeName?: string;
  priority?: string;
  defectOrigin?: string;
  defectOriginSubItem?: string;
  supplierName?: string;
  supplierAnalysis?: string;
  supplierProjectId?: string;
  benchDefects?: { id: string; issueKey: string; status: string }[];
  problemReports?: { id: string; issueKey: string; status: string }[];
  createdAt?: string;
  updatedAt?: string;
}

export default function TechEventDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [event, setEvent] = useState<TechEventDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [transitions, setTransitions] = useState<string[]>([]);
  const [transitioning, setTransitioning] = useState(false);

  // Cascading dropdowns
  const [programs, setPrograms] = useState<any[]>([]);
  const [testMeans, setTestMeans] = useState<any[]>([]);
  const [systems, setSystems] = useState<any[]>([]);
  const [ataChapters, setAtaChapters] = useState<any[]>([]);

  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState<Partial<TechEventDetail>>({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (id) {
      loadEvent(id);
      loadTransitions(id);
    }
    loadPrograms();
  }, [id]);

  useEffect(() => {
    if (editForm.programId) {
      loadProgramData(editForm.programId);
    }
  }, [editForm.programId]);

  async function loadEvent(eventId: string) {
    setLoading(true);
    setError('');
    try {
      const res = await techEventApi.getById(eventId);
      setEvent(res.data);
      setEditForm(res.data);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Failed to load Tech Event');
    } finally {
      setLoading(false);
    }
  }

  async function loadTransitions(eventId: string) {
    try {
      const res = await techEventApi.getAvailableTransitions(eventId);
      setTransitions(Array.isArray(res.data) ? res.data : []);
    } catch {
      setTransitions([]);
    }
  }

  async function loadPrograms() {
    try {
      const res = await masterDataApi.getPrograms();
      setPrograms(Array.isArray(res.data) ? res.data : []);
    } catch {
      setPrograms([]);
    }
  }

  async function loadProgramData(programId: string) {
    try {
      const [tmRes, sysRes, ataRes] = await Promise.all([
        masterDataApi.getTestMeans(programId),
        masterDataApi.getSystems(programId),
        masterDataApi.getAtaChapters(programId),
      ]);
      setTestMeans(Array.isArray(tmRes.data) ? tmRes.data : []);
      setSystems(Array.isArray(sysRes.data) ? sysRes.data : []);
      setAtaChapters(Array.isArray(ataRes.data) ? ataRes.data : []);
    } catch {
      setTestMeans([]);
      setSystems([]);
      setAtaChapters([]);
    }
  }

  async function handleTransition(targetStatus: string) {
    if (!id) return;
    setTransitioning(true);
    try {
      await techEventApi.transition(id, targetStatus);
      await loadEvent(id);
      await loadTransitions(id);
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Transition failed');
    } finally {
      setTransitioning(false);
    }
  }

  async function handleSave() {
    if (!id) return;
    setSaving(true);
    try {
      const res = await techEventApi.update(id, editForm);
      setEvent(res.data);
      setEditing(false);
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Save failed');
    } finally {
      setSaving(false);
    }
  }

  async function handleShareWithSupplier() {
    if (!id || !event?.supplierProjectId) {
      alert('No supplier project configured');
      return;
    }
    try {
      await techEventApi.shareWithSupplier(id, event.supplierProjectId);
      alert('Shared with supplier successfully');
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Share failed');
    }
  }

  async function handleCreateBenchDefect() {
    if (!id) return;
    if (!confirm('Create Bench Defect from this Tech Event?')) return;
    try {
      const res = await techEventApi.createBenchDefect(id);
      alert(`Bench Defect created: ${res.data?.issueKey || 'success'}`);
      loadEvent(id);
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to create Bench Defect');
    }
  }

  async function handleCreateProblemReport() {
    if (!id) return;
    if (!confirm('Create Problem Report from this Tech Event?')) return;
    try {
      const res = await techEventApi.createProblemReport(id);
      alert(`Problem Report created: ${res.data?.issueKey || 'success'}`);
      loadEvent(id);
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Failed to create Problem Report');
    }
  }

  if (loading) {
    return (
      <div className="ads-page">
        <div className="ads-loading"><div className="ab-spinner" /> Loading Tech Event...</div>
      </div>
    );
  }

  if (error || !event) {
    return (
      <div className="ads-page">
        <div className="ads-error">{error || 'Tech Event not found'}</div>
      </div>
    );
  }

  const currentStepIdx = PIPELINE_STEPS.findIndex(s => s.toLowerCase() === (event.status || '').toLowerCase());

  return (
    <div className="ads-page">
      <Link className="ads-back-link" to="/aircraft-design/tech-events">&larr; Back to Tech Events</Link>

      {/* Header */}
      <div className="ads-detail-header">
        <div>
          <div className="ads-detail-meta">
            <span className="ads-detail-key">{event.issueKey || event.id.slice(0, 8)}</span>
            <span className={`ads-badge ads-badge--${(event.status || 'open').toLowerCase().replace(/\s+/g, '_')}`}>
              {event.status || 'OPEN'}
            </span>
          </div>
          <h1 className="ads-detail-summary">{event.summary || 'Untitled Tech Event'}</h1>
        </div>
        <div className="ads-detail-actions">
          {editing ? (
            <>
              <button className="ads-btn" onClick={() => { setEditing(false); setEditForm(event); }}>Cancel</button>
              <button className="ads-btn ads-btn--primary" onClick={handleSave} disabled={saving}>
                {saving ? 'Saving...' : 'Save'}
              </button>
            </>
          ) : (
            <>
              <button className="ads-btn" onClick={() => setEditing(true)}>Edit</button>
              <button className="ads-btn" onClick={handleShareWithSupplier}>Share with Supplier</button>
              <button className="ads-btn ads-btn--success" onClick={handleCreateBenchDefect}>Create Bench Defect</button>
              <button className="ads-btn ads-btn--danger" onClick={handleCreateProblemReport}>Create Problem Report</button>
            </>
          )}
        </div>
      </div>

      {/* Pipeline */}
      <div className="ads-pipeline">
        {PIPELINE_STEPS.map((step, i) => (
          <span key={step}>
            <span className={`ads-pipeline-step${i < currentStepIdx ? ' ads-pipeline-step--done' : i === currentStepIdx ? ' ads-pipeline-step--active' : ''}`}>
              {step}
            </span>
            {i < PIPELINE_STEPS.length - 1 && <span className="ads-pipeline-arrow"> &rarr; </span>}
          </span>
        ))}
      </div>

      {/* Transition buttons */}
      {transitions.length > 0 && !editing && (
        <div className="ads-transitions">
          <span style={{ fontSize: 12, color: '#6b778c', marginRight: 4, alignSelf: 'center' }}>Transitions:</span>
          {transitions.map(t => (
            <button key={t} className="ads-btn ads-btn--primary ads-btn--sm" onClick={() => handleTransition(t)} disabled={transitioning}>
              {t}
            </button>
          ))}
        </div>
      )}

      {/* General Info */}
      <div className="ads-section">
        <h3 className="ads-section-title">General Information</h3>
        <div className="ads-fields">
          <div className="ads-field">
            <span className="ads-field-label">Program</span>
            {editing ? (
              <select className="ads-select" value={editForm.programId || ''} onChange={e => setEditForm(f => ({ ...f, programId: e.target.value }))}>
                <option value="">-- Select Program --</option>
                {programs.map((p: any) => <option key={p.id} value={p.id}>{p.name || p.code}</option>)}
              </select>
            ) : (
              <span className="ads-field-value">{event.programName || '-'}</span>
            )}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Test Mean</span>
            {editing ? (
              <select className="ads-select" value={editForm.testMeanId || ''} onChange={e => setEditForm(f => ({ ...f, testMeanId: e.target.value }))}>
                <option value="">-- Select Test Mean --</option>
                {testMeans.map((tm: any) => <option key={tm.id} value={tm.id}>{tm.name}</option>)}
              </select>
            ) : (
              <span className="ads-field-value">{event.testMeanName || '-'}</span>
            )}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">System</span>
            {editing ? (
              <select className="ads-select" value={editForm.systemId || ''} onChange={e => setEditForm(f => ({ ...f, systemId: e.target.value }))}>
                <option value="">-- Select System --</option>
                {systems.map((s: any) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            ) : (
              <span className="ads-field-value">{event.systemName || '-'}</span>
            )}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">ATA Chapter</span>
            {editing ? (
              <select className="ads-select" value={editForm.ataChapter || ''} onChange={e => setEditForm(f => ({ ...f, ataChapter: e.target.value }))}>
                <option value="">-- Select ATA Chapter --</option>
                {ataChapters.map((a: any) => <option key={a.id || a.code} value={a.code}>{a.code} - {a.name}</option>)}
              </select>
            ) : (
              <span className="ads-field-value">{event.ataChapter || '-'}</span>
            )}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Reporter Team</span>
            <span className="ads-field-value">{event.reporterTeam || '-'}</span>
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Priority</span>
            <span className="ads-field-value">{event.priority || '-'}</span>
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Assignee</span>
            <span className="ads-field-value">{event.assigneeName || '-'}</span>
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Defect Origin</span>
            <span className="ads-field-value">{event.defectOrigin || '-'}</span>
          </div>
        </div>
      </div>

      {/* Description */}
      <div className="ads-section">
        <h3 className="ads-section-title">Description</h3>
        {editing ? (
          <textarea className="ads-field-textarea" style={{ width: '100%', minHeight: 80 }}
            value={editForm.description || ''} onChange={e => setEditForm(f => ({ ...f, description: e.target.value }))} />
        ) : (
          <p style={{ fontSize: 13, color: '#172b4d', whiteSpace: 'pre-wrap' }}>{event.description || 'No description.'}</p>
        )}
      </div>

      {/* Supplier Analysis */}
      <div className="ads-section">
        <h3 className="ads-section-title">Supplier Analysis</h3>
        <div className="ads-card">
          <div className="ads-fields">
            <div className="ads-field">
              <span className="ads-field-label">Supplier</span>
              <span className="ads-field-value">{event.supplierName || '-'}</span>
            </div>
            <div className="ads-field">
              <span className="ads-field-label">Analysis</span>
              <span className="ads-field-value">{event.supplierAnalysis || 'No analysis provided.'}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Linked Bench Defects */}
      <div className="ads-section">
        <h3 className="ads-section-title">Linked Bench Defects</h3>
        {event.benchDefects && event.benchDefects.length > 0 ? (
          <div className="ads-table-wrap">
            <table className="ads-table">
              <thead>
                <tr><th>Issue Key</th><th>Status</th></tr>
              </thead>
              <tbody>
                {event.benchDefects.map(bd => (
                  <tr key={bd.id}>
                    <td><span className="ads-table-link">{bd.issueKey}</span></td>
                    <td><span className={`ads-badge ads-badge--${(bd.status || 'open').toLowerCase().replace(/\s+/g, '_')}`}>{bd.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p style={{ color: '#97a0af', fontStyle: 'italic', fontSize: 13 }}>No linked Bench Defects.</p>
        )}
      </div>

      {/* Linked Problem Reports */}
      <div className="ads-section">
        <h3 className="ads-section-title">Linked Problem Reports</h3>
        {event.problemReports && event.problemReports.length > 0 ? (
          <div className="ads-table-wrap">
            <table className="ads-table">
              <thead>
                <tr><th>Issue Key</th><th>Status</th></tr>
              </thead>
              <tbody>
                {event.problemReports.map(pr => (
                  <tr key={pr.id}>
                    <td><span className="ads-table-link">{pr.issueKey}</span></td>
                    <td><span className={`ads-badge ads-badge--${(pr.status || 'open').toLowerCase().replace(/\s+/g, '_')}`}>{pr.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p style={{ color: '#97a0af', fontStyle: 'italic', fontSize: 13 }}>No linked Problem Reports.</p>
        )}
      </div>

      {/* Audit */}
      <div className="ads-section">
        <h3 className="ads-section-title">Audit</h3>
        <div className="ads-fields">
          <div className="ads-field">
            <span className="ads-field-label">Created</span>
            <span className="ads-field-value">{event.createdAt ? new Date(event.createdAt).toLocaleString() : '-'}</span>
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Updated</span>
            <span className="ads-field-value">{event.updatedAt ? new Date(event.updatedAt).toLocaleString() : '-'}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
