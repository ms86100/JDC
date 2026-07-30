import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { vvoApi } from '../../../api/vvoApi';
import { DEMO_VVO_DETAILS } from '../demoData';
import '../AircraftDesignStyles.css';

interface VvoDetail {
  id: string;
  issueKey?: string;
  summary?: string;
  description?: string;
  status?: string;
  projectId?: string;
  hlvvoId?: string;
  hlvvoKey?: string;
  fixVersionId?: string;
  fixVersionName?: string;
  idDoors?: string;
  applicability?: string;
  assigneeId?: string;
  assigneeName?: string;
  reporterId?: string;
  reporterName?: string;
  priority?: string;
  // Classification
  vvoType?: string;
  verificationMethod?: string;
  testLevel?: string;
  testCategory?: string;
  // Test Means
  testMeanId?: string;
  testMeanName?: string;
  benchName?: string;
  // Systems
  systemId?: string;
  systemName?: string;
  ataChapter?: string;
  functionName?: string;
  // Content
  preConditions?: string;
  passFailCriteria?: string;
  testProcedureRef?: string;
  // Planning
  plannedDate?: string;
  executionDate?: string;
  duration?: string;
  // Traceability
  requirementLinks?: { id: string; requirementKey: string; coverageStatus: string }[];
  linkedTests?: { id: string; issueKey: string; name: string; status: string }[];
  // Clone
  cloneSourceId?: string;
  cloneSourceKey?: string;
  // Audit
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
}

export default function VvoDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [vvo, setVvo] = useState<VvoDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(false);
  const [editForm, setEditForm] = useState<Partial<VvoDetail>>({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (id) loadVvo(id);
  }, [id]);

  async function loadVvo(vvoId: string) {
    setLoading(true);
    setError('');
    try {
      const res = await vvoApi.getById(vvoId);
      setVvo(res.data);
      setEditForm(res.data);
    } catch {
      const demo = DEMO_VVO_DETAILS[vvoId];
      if (demo) { setVvo(demo); setEditForm(demo); }
      else setError('VVO not found');
    } finally {
      setLoading(false);
    }
  }

  async function handleSave() {
    if (!id) return;
    setSaving(true);
    try {
      const res = await vvoApi.update(id, editForm);
      setVvo(res.data);
      setEditing(false);
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Save failed');
    } finally {
      setSaving(false);
    }
  }

  async function handleClone() {
    if (!id) return;
    if (!confirm('Clone this VVO?')) return;
    try {
      const res = await vvoApi.clone(id);
      alert(`Cloned successfully. New VVO: ${res.data?.issueKey || res.data?.id}`);
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Clone failed');
    }
  }

  if (loading) {
    return (
      <div className="ads-page">
        <div className="ads-loading"><div className="ab-spinner" /> Loading VVO...</div>
      </div>
    );
  }

  if (error || !vvo) {
    return (
      <div className="ads-page">
        <div className="ads-error">{error || 'VVO not found'}</div>
      </div>
    );
  }

  const fieldVal = (key: keyof VvoDetail) => {
    if (editing) {
      return (
        <input
          className="ads-field-input"
          value={(editForm[key] as string) || ''}
          onChange={e => setEditForm(f => ({ ...f, [key]: e.target.value }))}
        />
      );
    }
    return <span className="ads-field-value">{(vvo[key] as string) || '-'}</span>;
  };

  return (
    <div className="ads-page">
      <Link className="ads-back-link" to="/aircraft-design/vvos">
        &larr; Back to VVO List
      </Link>

      {/* Header */}
      <div className="ads-detail-header">
        <div>
          <div className="ads-detail-meta">
            <span className="ads-detail-key">{vvo.issueKey || vvo.id.slice(0, 8)}</span>
            <span className={`ads-badge ads-badge--${(vvo.status || 'new').toLowerCase().replace(/\s+/g, '_')}`}>
              {vvo.status || 'NEW'}
            </span>
          </div>
          {editing ? (
            <input
              className="ads-field-input"
              style={{ fontSize: 18, fontWeight: 600, marginTop: 8, width: '100%' }}
              value={editForm.summary || ''}
              onChange={e => setEditForm(f => ({ ...f, summary: e.target.value }))}
            />
          ) : (
            <h1 className="ads-detail-summary">{vvo.summary || 'Untitled VVO'}</h1>
          )}
        </div>
        <div className="ads-detail-actions">
          {editing ? (
            <>
              <button className="ads-btn" onClick={() => { setEditing(false); setEditForm(vvo); }}>Cancel</button>
              <button className="ads-btn ads-btn--primary" onClick={handleSave} disabled={saving}>
                {saving ? 'Saving...' : 'Save'}
              </button>
            </>
          ) : (
            <>
              <button className="ads-btn" onClick={() => setEditing(true)}>Edit</button>
              <button className="ads-btn" onClick={handleClone}>Clone</button>
            </>
          )}
        </div>
      </div>

      {/* General Info */}
      <div className="ads-section">
        <h3 className="ads-section-title">General Information</h3>
        <div className="ads-fields">
          <div className="ads-field">
            <span className="ads-field-label">Project</span>
            {fieldVal('projectId')}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Fix Version</span>
            {fieldVal('fixVersionName')}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Priority</span>
            {fieldVal('priority')}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Assignee</span>
            {fieldVal('assigneeName')}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Reporter</span>
            {fieldVal('reporterName')}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Applicability</span>
            {fieldVal('applicability')}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">ID Doors</span>
            {fieldVal('idDoors')}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">HLVVO</span>
            {vvo.hlvvoId ? (
              <Link className="ads-table-link" to={`/aircraft-design/hlvvos?id=${vvo.hlvvoId}`}>
                {vvo.hlvvoKey || vvo.hlvvoId.slice(0, 8)}
              </Link>
            ) : (
              <span className="ads-field-value">-</span>
            )}
          </div>
        </div>
      </div>

      {/* Description */}
      <div className="ads-section">
        <h3 className="ads-section-title">Description</h3>
        {editing ? (
          <textarea
            className="ads-field-textarea"
            style={{ width: '100%', minHeight: 100 }}
            value={editForm.description || ''}
            onChange={e => setEditForm(f => ({ ...f, description: e.target.value }))}
          />
        ) : (
          <p style={{ fontSize: 13, color: '#172b4d', whiteSpace: 'pre-wrap' }}>{vvo.description || 'No description provided.'}</p>
        )}
      </div>

      {/* Classification */}
      <div className="ads-section">
        <h3 className="ads-section-title">Classification</h3>
        <div className="ads-fields">
          <div className="ads-field">
            <span className="ads-field-label">VVO Type</span>
            {fieldVal('vvoType')}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Verification Method</span>
            {fieldVal('verificationMethod')}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Test Level</span>
            {fieldVal('testLevel')}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Test Category</span>
            {fieldVal('testCategory')}
          </div>
        </div>
      </div>

      {/* Test Means */}
      <div className="ads-section">
        <h3 className="ads-section-title">Test Means</h3>
        <div className="ads-fields">
          <div className="ads-field">
            <span className="ads-field-label">Test Mean</span>
            {fieldVal('testMeanName')}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Bench Name</span>
            {fieldVal('benchName')}
          </div>
        </div>
      </div>

      {/* Systems */}
      <div className="ads-section">
        <h3 className="ads-section-title">Systems</h3>
        <div className="ads-fields">
          <div className="ads-field">
            <span className="ads-field-label">System</span>
            {fieldVal('systemName')}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">ATA Chapter</span>
            {fieldVal('ataChapter')}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Function</span>
            {fieldVal('functionName')}
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="ads-section">
        <h3 className="ads-section-title">Content</h3>
        <div className="ads-fields">
          <div className="ads-field">
            <span className="ads-field-label">Pre-Conditions</span>
            {fieldVal('preConditions')}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Pass/Fail Criteria</span>
            {fieldVal('passFailCriteria')}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Test Procedure Ref</span>
            {fieldVal('testProcedureRef')}
          </div>
        </div>
      </div>

      {/* Planning */}
      <div className="ads-section">
        <h3 className="ads-section-title">Planning</h3>
        <div className="ads-fields">
          <div className="ads-field">
            <span className="ads-field-label">Planned Date</span>
            {fieldVal('plannedDate')}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Execution Date</span>
            {fieldVal('executionDate')}
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Duration</span>
            {fieldVal('duration')}
          </div>
        </div>
      </div>

      {/* Requirements & Traceability */}
      <div className="ads-section">
        <h3 className="ads-section-title">Requirements & Traceability</h3>
        {vvo.requirementLinks && vvo.requirementLinks.length > 0 ? (
          <div className="ads-table-wrap">
            <table className="ads-table">
              <thead>
                <tr>
                  <th>Requirement Key</th>
                  <th>Coverage Status</th>
                </tr>
              </thead>
              <tbody>
                {vvo.requirementLinks.map(rl => (
                  <tr key={rl.id}>
                    <td>{rl.requirementKey}</td>
                    <td>
                      <span className={`ads-badge ads-badge--${rl.coverageStatus === 'COVERED' ? 'verified' : rl.coverageStatus === 'PARTIALLY_COVERED' ? 'to_be_verified' : 'new'}`}>
                        {rl.coverageStatus}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p style={{ color: '#97a0af', fontStyle: 'italic', fontSize: 13 }}>No requirement links.</p>
        )}
      </div>

      {/* Linked Tests */}
      <div className="ads-section">
        <h3 className="ads-section-title">Linked Tests</h3>
        {vvo.linkedTests && vvo.linkedTests.length > 0 ? (
          <div className="ads-table-wrap">
            <table className="ads-table">
              <thead>
                <tr>
                  <th>Issue Key</th>
                  <th>Name</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {vvo.linkedTests.map(t => (
                  <tr key={t.id}>
                    <td><Link className="ads-table-link" to={`/tests/${t.id}`}>{t.issueKey}</Link></td>
                    <td>{t.name}</td>
                    <td>
                      <span className={`ads-badge ads-badge--${(t.status || 'draft').toLowerCase()}`}>{t.status}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p style={{ color: '#97a0af', fontStyle: 'italic', fontSize: 13 }}>No linked tests.</p>
        )}
      </div>

      {/* Clone History */}
      {vvo.cloneSourceId && (
        <div className="ads-section">
          <h3 className="ads-section-title">Clone History</h3>
          <div className="ads-alert ads-alert--info">
            This VVO was cloned from{' '}
            <Link className="ads-table-link" to={`/aircraft-design/vvos/${vvo.cloneSourceId}`}>
              {vvo.cloneSourceKey || vvo.cloneSourceId.slice(0, 8)}
            </Link>
          </div>
        </div>
      )}

      {/* Audit */}
      <div className="ads-section">
        <h3 className="ads-section-title">Audit</h3>
        <div className="ads-fields">
          <div className="ads-field">
            <span className="ads-field-label">Created At</span>
            <span className="ads-field-value">{vvo.createdAt ? new Date(vvo.createdAt).toLocaleString() : '-'}</span>
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Updated At</span>
            <span className="ads-field-value">{vvo.updatedAt ? new Date(vvo.updatedAt).toLocaleString() : '-'}</span>
          </div>
          <div className="ads-field">
            <span className="ads-field-label">Created By</span>
            <span className="ads-field-value">{vvo.createdBy || '-'}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
