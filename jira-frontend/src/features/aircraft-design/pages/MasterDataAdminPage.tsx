import { useState, useEffect } from 'react';
import { masterDataApi } from '../../../api/masterDataApi';
import '../AircraftDesignStyles.css';

const TABS = ['Programs', 'Test Means', 'Systems', 'ATA Chapters', 'Suppliers', 'Functions', 'Reporter Teams', 'Defect Origins'] as const;
type TabName = typeof TABS[number];

interface MasterDataItem {
  id: string;
  name?: string;
  code?: string;
  description?: string;
  active?: boolean;
  programId?: string;
  systemId?: string;
  parentId?: string;
  [key: string]: any;
}

export default function MasterDataAdminPage() {
  const [activeTab, setActiveTab] = useState<TabName>('Programs');
  const [items, setItems] = useState<MasterDataItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Context selectors
  const [programs, setPrograms] = useState<MasterDataItem[]>([]);
  const [selectedProgramId, setSelectedProgramId] = useState('');
  const [systems, setSystems] = useState<MasterDataItem[]>([]);
  const [selectedSystemId, setSelectedSystemId] = useState('');

  // Create modal
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState<Record<string, string>>({});
  const [creating, setCreating] = useState(false);

  // Edit modal
  const [editItem, setEditItem] = useState<MasterDataItem | null>(null);
  const [editForm, setEditForm] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadPrograms();
  }, []);

  useEffect(() => {
    loadTabData();
  }, [activeTab, selectedProgramId, selectedSystemId]);

  async function loadPrograms() {
    try {
      const res = await masterDataApi.getPrograms();
      const data = Array.isArray(res.data) ? res.data : [];
      setPrograms(data);
      if (data.length > 0 && !selectedProgramId) {
        setSelectedProgramId(data[0].id);
      }
    } catch {
      setPrograms([]);
    }
  }

  async function loadSystems(programId: string) {
    try {
      const res = await masterDataApi.getSystems(programId);
      setSystems(Array.isArray(res.data) ? res.data : []);
    } catch {
      setSystems([]);
    }
  }

  async function loadTabData() {
    setLoading(true);
    setError('');
    try {
      let res;
      switch (activeTab) {
        case 'Programs':
          res = await masterDataApi.getPrograms();
          break;
        case 'Test Means':
          if (!selectedProgramId) { setItems([]); setLoading(false); return; }
          res = await masterDataApi.getTestMeans(selectedProgramId);
          break;
        case 'Systems':
          if (!selectedProgramId) { setItems([]); setLoading(false); return; }
          res = await masterDataApi.getSystems(selectedProgramId);
          break;
        case 'ATA Chapters':
          if (!selectedProgramId) { setItems([]); setLoading(false); return; }
          res = await masterDataApi.getAtaChapters(selectedProgramId);
          break;
        case 'Suppliers':
          if (!selectedProgramId || !selectedSystemId) { setItems([]); setLoading(false); return; }
          res = await masterDataApi.getSuppliers(selectedProgramId, selectedSystemId);
          break;
        case 'Functions':
          if (!selectedSystemId) { setItems([]); setLoading(false); return; }
          res = await masterDataApi.getFunctions(selectedSystemId);
          break;
        case 'Reporter Teams':
          res = await masterDataApi.getReporterTeams(selectedProgramId || undefined);
          break;
        case 'Defect Origins':
          res = await masterDataApi.getDefectOrigins();
          break;
        default:
          setItems([]);
          setLoading(false);
          return;
      }
      setItems(Array.isArray(res.data) ? res.data : []);
    } catch (err: any) {
      setError(err?.response?.data?.message || `Failed to load ${activeTab}`);
      setItems([]);
    } finally {
      setLoading(false);
    }
  }

  function handleTabChange(tab: TabName) {
    setActiveTab(tab);
    setItems([]);
    if (tab === 'Systems' || tab === 'Test Means' || tab === 'ATA Chapters') {
      if (selectedProgramId) loadSystems(selectedProgramId);
    }
  }

  function handleProgramChange(programId: string) {
    setSelectedProgramId(programId);
    setSelectedSystemId('');
    if (programId) loadSystems(programId);
  }

  async function handleCreate() {
    setCreating(true);
    try {
      switch (activeTab) {
        case 'Programs':
          await masterDataApi.createProgram(createForm);
          loadPrograms();
          break;
        case 'Test Means':
          await masterDataApi.createTestMean({ ...createForm, programId: selectedProgramId });
          break;
        case 'Systems':
          await masterDataApi.createSystem({ ...createForm, programId: selectedProgramId });
          break;
        case 'Suppliers':
          await masterDataApi.createSupplier({ ...createForm, programId: selectedProgramId, systemId: selectedSystemId });
          break;
        case 'Functions':
          await masterDataApi.createFunction({ ...createForm, systemId: selectedSystemId });
          break;
        case 'Reporter Teams':
          await masterDataApi.createReporterTeam({ ...createForm, programId: selectedProgramId || undefined });
          break;
        default:
          alert('Create not supported for this tab');
          setCreating(false);
          return;
      }
      setShowCreate(false);
      setCreateForm({});
      loadTabData();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Create failed');
    } finally {
      setCreating(false);
    }
  }

  async function handleSaveEdit() {
    if (!editItem) return;
    setSaving(true);
    try {
      if (activeTab === 'Programs') {
        await masterDataApi.updateProgram(editItem.id, editForm);
        loadPrograms();
      }
      // Other updates can reuse the same pattern when API supports it
      setEditItem(null);
      setEditForm({});
      loadTabData();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Update failed');
    } finally {
      setSaving(false);
    }
  }

  async function handleDeactivate(item: MasterDataItem) {
    if (!confirm(`Deactivate "${item.name || item.code}"?`)) return;
    try {
      if (activeTab === 'Programs') {
        await masterDataApi.deleteProgram(item.id);
        loadPrograms();
      }
      loadTabData();
    } catch (err: any) {
      alert(err?.response?.data?.message || 'Deactivate failed');
    }
  }

  const needsProgramSelector = ['Test Means', 'Systems', 'ATA Chapters', 'Suppliers', 'Reporter Teams'].includes(activeTab);
  const needsSystemSelector = ['Suppliers', 'Functions'].includes(activeTab);

  return (
    <div className="ads-page">
      <div className="ads-page-header">
        <div>
          <h1 className="ads-page-title">Master Data Administration</h1>
          <p className="ads-page-subtitle">Manage configurable master data for the Aircraft Design System</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="ads-tabs">
        {TABS.map(tab => (
          <button
            key={tab}
            className={`ads-tab${activeTab === tab ? ' ads-tab--active' : ''}`}
            onClick={() => handleTabChange(tab)}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* Context selectors */}
      {(needsProgramSelector || needsSystemSelector) && (
        <div className="ads-card" style={{ marginBottom: 16 }}>
          <div className="ads-toolbar">
            {needsProgramSelector && (
              <div className="ads-field">
                <label className="ads-field-label">Program</label>
                <select
                  className="ads-select"
                  value={selectedProgramId}
                  onChange={e => handleProgramChange(e.target.value)}
                >
                  <option value="">-- Select Program --</option>
                  {programs.map(p => (
                    <option key={p.id} value={p.id}>{p.name || p.code || p.id.slice(0, 8)}</option>
                  ))}
                </select>
              </div>
            )}
            {needsSystemSelector && (
              <div className="ads-field">
                <label className="ads-field-label">System</label>
                <select
                  className="ads-select"
                  value={selectedSystemId}
                  onChange={e => setSelectedSystemId(e.target.value)}
                >
                  <option value="">-- Select System --</option>
                  {systems.map(s => (
                    <option key={s.id} value={s.id}>{s.name || s.code || s.id.slice(0, 8)}</option>
                  ))}
                </select>
              </div>
            )}
          </div>
        </div>
      )}

      {error && <div className="ads-alert ads-alert--error">{error}</div>}

      {/* Actions */}
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 12 }}>
        <button className="ads-btn ads-btn--primary" onClick={() => { setShowCreate(true); setCreateForm({}); }}>
          + Add {activeTab.replace(/s$/, '')}
        </button>
      </div>

      {loading ? (
        <div className="ads-loading"><div className="ab-spinner" /> Loading {activeTab}...</div>
      ) : (
        <div className="ads-table-wrap">
          <table className="ads-table">
            <thead>
              <tr>
                <th>Name / Code</th>
                <th>Description</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {items.length === 0 ? (
                <tr>
                  <td colSpan={4} className="ads-table-empty">
                    {needsProgramSelector && !selectedProgramId
                      ? 'Select a program first'
                      : needsSystemSelector && !selectedSystemId
                        ? 'Select a system first'
                        : `No ${activeTab.toLowerCase()} found.`
                    }
                  </td>
                </tr>
              ) : (
                items.map(item => (
                  <tr key={item.id}>
                    <td style={{ fontWeight: 500 }}>{item.name || item.code || item.id.slice(0, 8)}</td>
                    <td>{item.description || '-'}</td>
                    <td>
                      <span className={`ads-badge ${item.active === false ? 'ads-badge--cancelled' : 'ads-badge--verified'}`}>
                        {item.active === false ? 'Inactive' : 'Active'}
                      </span>
                    </td>
                    <td>
                      <div className="ads-table-actions">
                        <button className="ads-btn ads-btn--sm ads-btn--ghost" onClick={() => { setEditItem(item); setEditForm({ name: item.name || '', description: item.description || '', code: item.code || '' }); }}>
                          Edit
                        </button>
                        <button className="ads-btn ads-btn--sm ads-btn--ghost" style={{ color: '#de350b' }} onClick={() => handleDeactivate(item)}>
                          Deactivate
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Create Modal */}
      {showCreate && (
        <div className="ads-modal-overlay" onClick={() => setShowCreate(false)}>
          <div className="ads-modal" onClick={e => e.stopPropagation()}>
            <h2 className="ads-modal-title">Add {activeTab.replace(/s$/, '')}</h2>
            <div className="ads-field" style={{ marginBottom: 12 }}>
              <label className="ads-field-label">Name</label>
              <input className="ads-field-input" value={createForm.name || ''}
                onChange={e => setCreateForm(f => ({ ...f, name: e.target.value }))} />
            </div>
            <div className="ads-field" style={{ marginBottom: 12 }}>
              <label className="ads-field-label">Code</label>
              <input className="ads-field-input" value={createForm.code || ''}
                onChange={e => setCreateForm(f => ({ ...f, code: e.target.value }))} />
            </div>
            <div className="ads-field" style={{ marginBottom: 12 }}>
              <label className="ads-field-label">Description</label>
              <textarea className="ads-field-textarea" value={createForm.description || ''}
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

      {/* Edit Modal */}
      {editItem && (
        <div className="ads-modal-overlay" onClick={() => setEditItem(null)}>
          <div className="ads-modal" onClick={e => e.stopPropagation()}>
            <h2 className="ads-modal-title">Edit {activeTab.replace(/s$/, '')}</h2>
            <div className="ads-field" style={{ marginBottom: 12 }}>
              <label className="ads-field-label">Name</label>
              <input className="ads-field-input" value={editForm.name || ''}
                onChange={e => setEditForm(f => ({ ...f, name: e.target.value }))} />
            </div>
            <div className="ads-field" style={{ marginBottom: 12 }}>
              <label className="ads-field-label">Code</label>
              <input className="ads-field-input" value={editForm.code || ''}
                onChange={e => setEditForm(f => ({ ...f, code: e.target.value }))} />
            </div>
            <div className="ads-field" style={{ marginBottom: 12 }}>
              <label className="ads-field-label">Description</label>
              <textarea className="ads-field-textarea" value={editForm.description || ''}
                onChange={e => setEditForm(f => ({ ...f, description: e.target.value }))} />
            </div>
            <div className="ads-modal-actions">
              <button className="ads-btn" onClick={() => setEditItem(null)}>Cancel</button>
              <button className="ads-btn ads-btn--primary" onClick={handleSaveEdit} disabled={saving}>
                {saving ? 'Saving...' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
