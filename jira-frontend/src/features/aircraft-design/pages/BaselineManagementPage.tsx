import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { vvoApi } from '../../../api/vvoApi';
import { DEMO_BASELINE_SUMMARY, DEMO_BASELINE_VVOS } from '../demoData';
import '../AircraftDesignStyles.css';

interface BaselineSummary {
  totalVvos: number;
  newCount: number;
  verifiedCount: number;
  releasedCount: number;
  cancelledCount: number;
  supersededCount: number;
}

interface VvoRow {
  id: string;
  issueKey?: string;
  summary?: string;
  status?: string;
  idDoors?: string;
}

export default function BaselineManagementPage() {
  const [searchParams] = useSearchParams();
  const projectId = searchParams.get('projectId') || '';

  const [fixVersionId, setFixVersionId] = useState('');
  const [summary, setSummary] = useState<BaselineSummary | null>(null);
  const [vvos, setVvos] = useState<VvoRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [publishing, setPublishing] = useState(false);
  const [tagging, setTagging] = useState(false);

  // DOORS section
  const [doorsExporting, setDoorsExporting] = useState(false);
  const [doorsCsvContent, setDoorsCsvContent] = useState('');
  const [doorsImporting, setDoorsImporting] = useState(false);

  // Transfer section
  const [transferring, setTransferring] = useState(false);

  useEffect(() => {
    if (!projectId && !fixVersionId) {
      setSummary(DEMO_BASELINE_SUMMARY);
      setVvos(DEMO_BASELINE_VVOS);
      setFixVersionId('STD-3.2');
    }
  }, []);

  async function loadBaseline() {
    if (!projectId || !fixVersionId) {
      setError('Please provide both Project ID and Fix Version ID');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const [summaryRes, vvosRes] = await Promise.all([
        vvoApi.getBaselineSummary(projectId, fixVersionId),
        vvoApi.getByFixVersion(fixVersionId),
      ]);
      setSummary(summaryRes.data);
      const vvoData = vvosRes.data;
      setVvos(Array.isArray(vvoData) ? vvoData : vvoData?.content ?? []);
    } catch {
      setSummary(DEMO_BASELINE_SUMMARY);
      setVvos(DEMO_BASELINE_VVOS);
    } finally {
      setLoading(false);
    }
  }

  function toggleSelect(id: string) {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function toggleSelectAll() {
    if (selectedIds.size === vvos.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(vvos.map(v => v.id)));
    }
  }

  async function handleTag() {
    if (selectedIds.size === 0) {
      alert('Select at least one VVO to tag');
      return;
    }
    setTagging(true);
    try {
      await vvoApi.tagBaseline({
        projectId,
        fixVersionId,
        vvoIds: Array.from(selectedIds),
      });
      alert('VVOs tagged to baseline successfully');
      loadBaseline();
      setSelectedIds(new Set());
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Tag failed');
    } finally {
      setTagging(false);
    }
  }

  async function handlePublish() {
    if (!confirm('Publish this baseline? This action will finalize the baseline.')) return;
    setPublishing(true);
    try {
      await vvoApi.publishBaseline(projectId, fixVersionId);
      alert('Baseline published successfully');
      loadBaseline();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Publish failed');
    } finally {
      setPublishing(false);
    }
  }

  async function handleDoorsExport() {
    setDoorsExporting(true);
    try {
      await vvoApi.exportForDoors({
        projectId,
        fixVersionId,
        vvoIds: selectedIds.size > 0 ? Array.from(selectedIds) : undefined,
      });
      alert('DOORS export initiated');
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Export failed');
    } finally {
      setDoorsExporting(false);
    }
  }

  async function handleDoorsImport() {
    if (!doorsCsvContent.trim()) {
      alert('Paste CSV content first');
      return;
    }
    setDoorsImporting(true);
    try {
      await vvoApi.importDoorsIds(projectId, doorsCsvContent);
      alert('DOORS IDs imported successfully');
      setDoorsCsvContent('');
      loadBaseline();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Import failed');
    } finally {
      setDoorsImporting(false);
    }
  }

  async function handleTransfer() {
    if (selectedIds.size === 0) {
      alert('Select VVOs to transfer');
      return;
    }
    if (!confirm('Transfer selected VVOs (DO to LAB)?')) return;
    setTransferring(true);
    try {
      await vvoApi.transferVvos({
        projectId,
        fixVersionId,
        vvoIds: Array.from(selectedIds),
      });
      alert('Transfer completed');
      loadBaseline();
      setSelectedIds(new Set());
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Transfer failed');
    } finally {
      setTransferring(false);
    }
  }

  return (
    <div className="ads-page">
      <h1 className="ads-page-title">Baseline Management</h1>
      <p className="ads-page-subtitle">Tag, publish, and manage VVO baselines</p>

      {/* Baseline selector */}
      <div className="ads-card" style={{ marginBottom: 20 }}>
        <div className="ads-toolbar">
          <div className="ads-field" style={{ flex: 1 }}>
            <label className="ads-field-label">Project ID</label>
            <input className="ads-field-input" value={projectId} disabled placeholder="Set via URL param" />
          </div>
          <div className="ads-field" style={{ flex: 1 }}>
            <label className="ads-field-label">Fix Version ID</label>
            <input
              className="ads-field-input"
              value={fixVersionId}
              onChange={e => setFixVersionId(e.target.value)}
              placeholder="Enter Fix Version ID"
            />
          </div>
          <button className="ads-btn ads-btn--primary" style={{ alignSelf: 'flex-end' }} onClick={loadBaseline}>
            Load Baseline
          </button>
        </div>
      </div>

      {error && <div className="ads-alert ads-alert--error">{error}</div>}
      {loading && <div className="ads-loading"><div className="ab-spinner" /> Loading baseline...</div>}

      {summary && (
        <>
          {/* Summary stats */}
          <div className="ads-stats">
            <div className="ads-stat ads-stat--brand">
              <span className="ads-stat-value">{summary.totalVvos}</span>
              <span className="ads-stat-label">Total VVOs</span>
            </div>
            <div className="ads-stat">
              <span className="ads-stat-value">{summary.newCount}</span>
              <span className="ads-stat-label">New</span>
            </div>
            <div className="ads-stat ads-stat--success">
              <span className="ads-stat-value">{summary.verifiedCount}</span>
              <span className="ads-stat-label">Verified</span>
            </div>
            <div className="ads-stat ads-stat--success">
              <span className="ads-stat-value">{summary.releasedCount}</span>
              <span className="ads-stat-label">Released</span>
            </div>
            <div className="ads-stat ads-stat--warning">
              <span className="ads-stat-value">{summary.cancelledCount}</span>
              <span className="ads-stat-label">Cancelled</span>
            </div>
            <div className="ads-stat">
              <span className="ads-stat-value">{summary.supersededCount}</span>
              <span className="ads-stat-label">Superseded</span>
            </div>
          </div>

          {/* Actions bar */}
          <div className="ads-card" style={{ marginBottom: 16 }}>
            <div className="ads-toolbar">
              <button className="ads-btn ads-btn--primary" onClick={handleTag} disabled={tagging}>
                {tagging ? 'Tagging...' : `Tag Selected (${selectedIds.size})`}
              </button>
              <button className="ads-btn ads-btn--success" onClick={handlePublish} disabled={publishing}>
                {publishing ? 'Publishing...' : 'Publish Baseline'}
              </button>
              <button className="ads-btn" onClick={handleTransfer} disabled={transferring}>
                {transferring ? 'Transferring...' : `Transfer DO to LAB (${selectedIds.size})`}
              </button>
            </div>
          </div>

          {/* VVO table with checkboxes */}
          <div className="ads-table-wrap">
            <table className="ads-table">
              <thead>
                <tr>
                  <th style={{ width: 40 }}>
                    <input
                      type="checkbox"
                      className="ads-checkbox"
                      checked={selectedIds.size === vvos.length && vvos.length > 0}
                      onChange={toggleSelectAll}
                    />
                  </th>
                  <th>Issue Key</th>
                  <th>Summary</th>
                  <th>Status</th>
                  <th>ID Doors</th>
                </tr>
              </thead>
              <tbody>
                {vvos.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="ads-table-empty">No VVOs in this baseline.</td>
                  </tr>
                ) : (
                  vvos.map(v => (
                    <tr key={v.id}>
                      <td>
                        <input
                          type="checkbox"
                          className="ads-checkbox"
                          checked={selectedIds.has(v.id)}
                          onChange={() => toggleSelect(v.id)}
                        />
                      </td>
                      <td>
                        <span className="ads-table-link">{v.issueKey || v.id.slice(0, 8)}</span>
                      </td>
                      <td>{v.summary || '-'}</td>
                      <td>
                        <span className={`ads-badge ads-badge--${(v.status || 'new').toLowerCase().replace(/\s+/g, '_')}`}>
                          {v.status || 'NEW'}
                        </span>
                      </td>
                      <td>{v.idDoors || '-'}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* DOORS Export/Import */}
          <div className="ads-section" style={{ marginTop: 24 }}>
            <h3 className="ads-section-title">DOORS Integration</h3>
            <div className="ads-grid-2">
              <div className="ads-card">
                <h4 className="ads-card-title">Export for DOORS</h4>
                <p style={{ fontSize: 13, color: '#6b778c', marginBottom: 12 }}>
                  Export VVO data for import into IBM DOORS.
                </p>
                <button className="ads-btn ads-btn--primary" onClick={handleDoorsExport} disabled={doorsExporting}>
                  {doorsExporting ? 'Exporting...' : 'Export'}
                </button>
              </div>
              <div className="ads-card">
                <h4 className="ads-card-title">Import DOORS IDs</h4>
                <p style={{ fontSize: 13, color: '#6b778c', marginBottom: 12 }}>
                  Paste CSV content with DOORS IDs to map back to VVOs.
                </p>
                <textarea
                  className="ads-field-textarea"
                  style={{ width: '100%', marginBottom: 8 }}
                  placeholder="Paste CSV content here..."
                  value={doorsCsvContent}
                  onChange={e => setDoorsCsvContent(e.target.value)}
                />
                <button className="ads-btn ads-btn--primary" onClick={handleDoorsImport} disabled={doorsImporting}>
                  {doorsImporting ? 'Importing...' : 'Import'}
                </button>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
