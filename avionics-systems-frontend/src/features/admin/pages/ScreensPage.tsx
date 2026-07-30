import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AdminLayout from '../components/AdminLayout';
import {
  useScreens,
  useCreateScreen,
  useUpdateScreen,
  useDeleteScreen,
  useScreenSchemes,
  useCreateScreenScheme,
  useUpdateScreenScheme,
  useDeleteScreenScheme,
  Screen,
  ScreenScheme,
} from '../hooks/useAdminApi';
import './ScreensPage.css';
import '../styles/admin-shared.css';

/* ------------------------------------------------------------------ */
/*  Screen Create/Edit Modal                                          */
/* ------------------------------------------------------------------ */

interface ScreenModalProps {
  screen: Partial<Screen> | null;   // null = create mode
  onClose: () => void;
  onSave: (data: { name: string; description: string }) => void;
  isPending: boolean;
}

function ScreenModal({ screen, onClose, onSave, isPending }: ScreenModalProps) {
  const [name, setName] = useState(screen?.name ?? '');
  const [description, setDescription] = useState(screen?.description ?? '');

  const isEdit = !!screen?.id;

  const handleSubmit = () => {
    if (!name.trim()) return;
    onSave({ name: name.trim(), description: description.trim() });
  };

  return (
    <div className="admin-modal-overlay" onClick={onClose}>
      <div className="admin-modal" onClick={e => e.stopPropagation()}>
        <div className="admin-modal-header">
          <h3>{isEdit ? 'Edit Screen' : 'Add Screen'}</h3>
          <button onClick={onClose}>&times;</button>
        </div>
        <div className="admin-modal-body">
          <div className="admin-form-group">
            <label className="admin-form-label">
              Name <span style={{ color: 'var(--sa-danger-500, #d73a49)' }}>*</span>
            </label>
            <input
              className="admin-form-input"
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="e.g., Default Screen"
              autoFocus
            />
          </div>
          <div className="admin-form-group">
            <label className="admin-form-label">Description</label>
            <textarea
              className="admin-form-input"
              value={description}
              onChange={e => setDescription(e.target.value)}
              placeholder="Describe the purpose of this screen"
              rows={3}
              style={{ resize: 'vertical' }}
            />
          </div>
        </div>
        <div className="admin-modal-footer">
          <button className="admin-btn-secondary" onClick={onClose}>Cancel</button>
          <button
            className="admin-btn-primary"
            onClick={handleSubmit}
            disabled={!name.trim() || isPending}
          >
            {isPending ? 'Saving...' : isEdit ? 'Save Changes' : 'Add Screen'}
          </button>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Screen Scheme Create/Edit Modal                                   */
/* ------------------------------------------------------------------ */

interface SchemeModalProps {
  scheme: Partial<ScreenScheme> | null;
  onClose: () => void;
  onSave: (data: { name: string; description: string }) => void;
  isPending: boolean;
}

function SchemeModal({ scheme, onClose, onSave, isPending }: SchemeModalProps) {
  const [name, setName] = useState(scheme?.name ?? '');
  const [description, setDescription] = useState(scheme?.description ?? '');

  const isEdit = !!scheme?.id;

  const handleSubmit = () => {
    if (!name.trim()) return;
    onSave({ name: name.trim(), description: description.trim() });
  };

  return (
    <div className="admin-modal-overlay" onClick={onClose}>
      <div className="admin-modal" onClick={e => e.stopPropagation()}>
        <div className="admin-modal-header">
          <h3>{isEdit ? 'Edit Screen Scheme' : 'Add Screen Scheme'}</h3>
          <button onClick={onClose}>&times;</button>
        </div>
        <div className="admin-modal-body">
          <div className="admin-form-group">
            <label className="admin-form-label">
              Name <span style={{ color: 'var(--sa-danger-500, #d73a49)' }}>*</span>
            </label>
            <input
              className="admin-form-input"
              value={name}
              onChange={e => setName(e.target.value)}
              placeholder="e.g., Default Screen Scheme"
              autoFocus
            />
          </div>
          <div className="admin-form-group">
            <label className="admin-form-label">Description</label>
            <textarea
              className="admin-form-input"
              value={description}
              onChange={e => setDescription(e.target.value)}
              placeholder="Describe this screen scheme"
              rows={3}
              style={{ resize: 'vertical' }}
            />
          </div>
        </div>
        <div className="admin-modal-footer">
          <button className="admin-btn-secondary" onClick={onClose}>Cancel</button>
          <button
            className="admin-btn-primary"
            onClick={handleSubmit}
            disabled={!name.trim() || isPending}
          >
            {isPending ? 'Saving...' : isEdit ? 'Save Changes' : 'Add Screen Scheme'}
          </button>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Delete Confirmation Modal                                         */
/* ------------------------------------------------------------------ */

interface DeleteConfirmProps {
  entityName: string;
  entityLabel: string;
  onCancel: () => void;
  onConfirm: () => void;
  isPending: boolean;
}

function DeleteConfirmModal({ entityName, entityLabel, onCancel, onConfirm, isPending }: DeleteConfirmProps) {
  return (
    <div className="admin-modal-overlay" onClick={onCancel}>
      <div className="admin-modal" onClick={e => e.stopPropagation()}>
        <div className="admin-modal-header">
          <h3>Delete {entityLabel}</h3>
          <button onClick={onCancel}>&times;</button>
        </div>
        <div className="admin-modal-body">
          <p>
            Are you sure you want to delete <strong>{entityName}</strong>? This action cannot be undone.
          </p>
        </div>
        <div className="admin-modal-footer">
          <button className="admin-btn-secondary" onClick={onCancel}>Cancel</button>
          <button className="admin-btn-danger" onClick={onConfirm} disabled={isPending}>
            {isPending ? 'Deleting...' : 'Delete'}
          </button>
        </div>
      </div>
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Main Page                                                         */
/* ------------------------------------------------------------------ */

export default function ScreensPage() {
  const navigate = useNavigate();

  // ---- tab state ----
  const [activeTab, setActiveTab] = useState<'screens' | 'schemes'>('screens');
  const [search, setSearch] = useState('');

  // ---- screens data ----
  const { data: screens, isLoading } = useScreens();
  const createScreen = useCreateScreen();
  const updateScreen = useUpdateScreen();
  const deleteScreen = useDeleteScreen();

  // ---- screen schemes data ----
  const { data: screenSchemes, isLoading: schemesLoading } = useScreenSchemes();
  const createScheme = useCreateScreenScheme();
  const updateScheme = useUpdateScreenScheme();
  const deleteScheme = useDeleteScreenScheme();

  // ---- screen modal state ----
  const [showScreenModal, setShowScreenModal] = useState(false);
  const [editingScreen, setEditingScreen] = useState<Screen | null>(null);

  // ---- screen delete state ----
  const [deletingScreen, setDeletingScreen] = useState<Screen | null>(null);

  // ---- scheme modal state ----
  const [showSchemeModal, setShowSchemeModal] = useState(false);
  const [editingScheme, setEditingScheme] = useState<ScreenScheme | null>(null);

  // ---- scheme delete state ----
  const [deletingScheme, setDeletingScheme] = useState<ScreenScheme | null>(null);

  // ---- alerts ----
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const showSuccess = (msg: string) => {
    setSuccessMsg(msg);
    setErrorMsg(null);
    setTimeout(() => setSuccessMsg(null), 3000);
  };

  const showError = (msg: string) => {
    setErrorMsg(msg);
    setSuccessMsg(null);
  };

  // ---- filtered lists ----
  const filteredScreens = screens?.filter(s =>
    s.name.toLowerCase().includes(search.toLowerCase()) ||
    (s.description || '').toLowerCase().includes(search.toLowerCase())
  ) || [];

  const filteredSchemes = screenSchemes?.filter(s =>
    s.name.toLowerCase().includes(search.toLowerCase()) ||
    (s.description || '').toLowerCase().includes(search.toLowerCase())
  ) || [];

  // ---- screen handlers ----
  const handleOpenCreateScreen = () => {
    setEditingScreen(null);
    setShowScreenModal(true);
  };

  const handleOpenEditScreen = (screen: Screen) => {
    setEditingScreen(screen);
    setShowScreenModal(true);
  };

  const handleSaveScreen = (data: { name: string; description: string }) => {
    if (editingScreen) {
      updateScreen.mutate(
        { id: editingScreen.id, data },
        {
          onSuccess: () => {
            setShowScreenModal(false);
            setEditingScreen(null);
            showSuccess(`Screen "${data.name}" updated successfully.`);
          },
          onError: (err: unknown) => {
            const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
              || (err instanceof Error ? err.message : 'Failed to update screen');
            showError(msg);
          },
        },
      );
    } else {
      createScreen.mutate(
        data,
        {
          onSuccess: () => {
            setShowScreenModal(false);
            showSuccess(`Screen "${data.name}" created successfully.`);
          },
          onError: (err: unknown) => {
            const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
              || (err instanceof Error ? err.message : 'Failed to create screen');
            showError(msg);
          },
        },
      );
    }
  };

  const handleDeleteScreen = () => {
    if (!deletingScreen) return;
    deleteScreen.mutate(deletingScreen.id, {
      onSuccess: () => {
        showSuccess(`Screen "${deletingScreen.name}" deleted.`);
        setDeletingScreen(null);
      },
      onError: (err: unknown) => {
        const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
          || (err instanceof Error ? err.message : 'Failed to delete screen');
        showError(msg);
        setDeletingScreen(null);
      },
    });
  };

  const handleConfigureTabs = (screen: Screen) => {
    navigate(`/admin/screens/${screen.id}/tabs`);
  };

  // ---- scheme handlers ----
  const handleOpenCreateScheme = () => {
    setEditingScheme(null);
    setShowSchemeModal(true);
  };

  const handleOpenEditScheme = (scheme: ScreenScheme) => {
    setEditingScheme(scheme);
    setShowSchemeModal(true);
  };

  const handleSaveScheme = (data: { name: string; description: string }) => {
    if (editingScheme) {
      updateScheme.mutate(
        { id: editingScheme.id, data },
        {
          onSuccess: () => {
            setShowSchemeModal(false);
            setEditingScheme(null);
            showSuccess(`Screen scheme "${data.name}" updated successfully.`);
          },
          onError: (err: unknown) => {
            const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
              || (err instanceof Error ? err.message : 'Failed to update screen scheme');
            showError(msg);
          },
        },
      );
    } else {
      createScheme.mutate(
        data,
        {
          onSuccess: () => {
            setShowSchemeModal(false);
            showSuccess(`Screen scheme "${data.name}" created successfully.`);
          },
          onError: (err: unknown) => {
            const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
              || (err instanceof Error ? err.message : 'Failed to create screen scheme');
            showError(msg);
          },
        },
      );
    }
  };

  const handleDeleteScheme = () => {
    if (!deletingScheme) return;
    deleteScheme.mutate(deletingScheme.id, {
      onSuccess: () => {
        showSuccess(`Screen scheme "${deletingScheme.name}" deleted.`);
        setDeletingScheme(null);
      },
      onError: (err: unknown) => {
        const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
          || (err instanceof Error ? err.message : 'Failed to delete screen scheme');
        showError(msg);
        setDeletingScheme(null);
      },
    });
  };

  /* ================================================================ */
  /*  Screens Tab                                                     */
  /* ================================================================ */
  const renderScreensTab = () => (
    <>
      <div className="admin-toolbar">
        <div className="admin-toolbar-left">
          <input
            type="text"
            placeholder="Search screens..."
            className="admin-search-input-toolbar"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>
        <div className="admin-toolbar-right">
          <button className="admin-btn-primary" onClick={handleOpenCreateScreen}>
            Add Screen
          </button>
        </div>
      </div>

      <div className="admin-table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Screen Name</th>
              <th>Description</th>
              <th>Tabs</th>
              <th>Fields</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <>
                {[...Array(6)].map((_, i) => (
                  <tr key={i}>
                    <td style={{ padding: '12px 16px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <div className="ab-skeleton" style={{ height: 24, width: 24, borderRadius: 'var(--sa-radius-sm)', flexShrink: 0 }} />
                        <div className="ab-skeleton" style={{ height: 16, width: '60%', borderRadius: 'var(--sa-radius-sm)' }} />
                      </div>
                    </td>
                    <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: '70%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 50, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 60, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 120, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                  </tr>
                ))}
              </>
            ) : filteredScreens.length === 0 ? (
              <tr>
                <td colSpan={5} style={{ textAlign: 'center', padding: '24px' }}>
                  {search ? 'No screens match your search.' : 'No screens found. Click "Add Screen" to create one.'}
                </td>
              </tr>
            ) : (
              filteredScreens.map(screen => (
                <tr key={screen.id}>
                  <td>
                    <div className="screen-cell">
                      <span className="screen-icon">S</span>
                      <span className="screen-name">{screen.name}</span>
                    </div>
                  </td>
                  <td className="description-cell">{screen.description || 'No description'}</td>
                  <td>
                    <span className="tab-count">{screen.tabs?.length || 0} tabs</span>
                  </td>
                  <td>
                    <span className="field-count">
                      {screen.tabs?.reduce((sum, tab) => sum + (tab.fieldIds?.length || 0), 0) || 0} fields
                    </span>
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="admin-btn-secondary admin-btn-sm"
                        onClick={() => handleOpenEditScreen(screen)}
                      >
                        Edit
                      </button>
                      <button
                        className="admin-btn-secondary admin-btn-sm"
                        onClick={() => handleConfigureTabs(screen)}
                      >
                        Configure Tabs
                      </button>
                      <button
                        className="admin-btn-danger admin-btn-sm"
                        onClick={() => setDeletingScreen(screen)}
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </>
  );

  /* ================================================================ */
  /*  Screen Schemes Tab                                              */
  /* ================================================================ */
  const renderSchemesTab = () => (
    <>
      <div className="admin-toolbar">
        <div className="admin-toolbar-left">
          <input
            type="text"
            placeholder="Search screen schemes..."
            className="admin-search-input-toolbar"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>
        <div className="admin-toolbar-right">
          <button className="admin-btn-primary" onClick={handleOpenCreateScheme}>
            Add Screen Scheme
          </button>
        </div>
      </div>

      <div className="admin-table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Scheme Name</th>
              <th>Description</th>
              <th>Screen Mappings</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {schemesLoading ? (
              <>
                {[...Array(4)].map((_, i) => (
                  <tr key={i}>
                    <td style={{ padding: '12px 16px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <div className="ab-skeleton" style={{ height: 24, width: 32, borderRadius: 'var(--sa-radius-sm)', flexShrink: 0 }} />
                        <div className="ab-skeleton" style={{ height: 16, width: '60%', borderRadius: 'var(--sa-radius-sm)' }} />
                      </div>
                    </td>
                    <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: '70%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 80, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 120, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                  </tr>
                ))}
              </>
            ) : filteredSchemes.length === 0 ? (
              <tr>
                <td colSpan={4} style={{ textAlign: 'center', padding: '24px' }}>
                  {search ? 'No screen schemes match your search.' : 'No screen schemes found. Click "Add Screen Scheme" to create one.'}
                </td>
              </tr>
            ) : (
              filteredSchemes.map(scheme => (
                <tr key={scheme.id}>
                  <td>
                    <div className="screen-cell">
                      <span className="screen-icon scheme">SC</span>
                      <span className="screen-name">{scheme.name}</span>
                    </div>
                  </td>
                  <td className="description-cell">{scheme.description || 'No description'}</td>
                  <td>
                    <span className="tab-count">
                      {scheme.screenMappings?.length || 0} mappings
                    </span>
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button
                        className="admin-btn-secondary admin-btn-sm"
                        onClick={() => handleOpenEditScheme(scheme)}
                      >
                        Edit
                      </button>
                      <button
                        className="admin-btn-danger admin-btn-sm"
                        onClick={() => setDeletingScheme(scheme)}
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </>
  );

  /* ================================================================ */
  /*  Render                                                          */
  /* ================================================================ */
  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">Screens</h1>
          <p className="admin-page-description">
            Manage screens and screen schemes for issue operations.
          </p>
        </div>

        {/* Alerts */}
        {successMsg && (
          <div className="admin-alert admin-alert-success">
            {successMsg}
            <button
              style={{ marginLeft: 'auto', background: 'none', border: 'none', cursor: 'pointer', fontSize: 16 }}
              onClick={() => setSuccessMsg(null)}
            >
              &times;
            </button>
          </div>
        )}
        {errorMsg && (
          <div className="admin-alert admin-alert-error">
            {errorMsg}
            <button
              style={{ marginLeft: 'auto', background: 'none', border: 'none', cursor: 'pointer', fontSize: 16 }}
              onClick={() => setErrorMsg(null)}
            >
              &times;
            </button>
          </div>
        )}

        {/* Stat cards */}
        <div className="admin-stats-grid">
          <div className="admin-stat-card">
            <div className="admin-stat-value">{screens?.length ?? 0}</div>
            <div className="admin-stat-label">Screens</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-value">{screenSchemes?.length ?? 0}</div>
            <div className="admin-stat-label">Screen Schemes</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-value">
              {screens?.reduce((sum, s) => sum + (s.tabs?.length || 0), 0) ?? 0}
            </div>
            <div className="admin-stat-label">Total Tabs</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-value">
              {screens?.reduce((sum, s) => sum + (s.tabs?.reduce((t, tab) => t + (tab.fieldIds?.length || 0), 0) || 0), 0) ?? 0}
            </div>
            <div className="admin-stat-label">Total Fields</div>
          </div>
        </div>

        {/* Tab navigation */}
        <div className="screens-tabs">
          <button
            className={`screens-tab ${activeTab === 'screens' ? 'active' : ''}`}
            onClick={() => { setActiveTab('screens'); setSearch(''); }}
          >
            Screens
          </button>
          <button
            className={`screens-tab ${activeTab === 'schemes' ? 'active' : ''}`}
            onClick={() => { setActiveTab('schemes'); setSearch(''); }}
          >
            Screen Schemes
          </button>
        </div>

        <div className="screens-content">
          {activeTab === 'screens' && renderScreensTab()}
          {activeTab === 'schemes' && renderSchemesTab()}
        </div>
      </div>

      {/* Screen create/edit modal */}
      {showScreenModal && (
        <ScreenModal
          screen={editingScreen}
          onClose={() => { setShowScreenModal(false); setEditingScreen(null); }}
          onSave={handleSaveScreen}
          isPending={createScreen.isPending || updateScreen.isPending}
        />
      )}

      {/* Screen delete confirmation */}
      {deletingScreen && (
        <DeleteConfirmModal
          entityName={deletingScreen.name}
          entityLabel="Screen"
          onCancel={() => setDeletingScreen(null)}
          onConfirm={handleDeleteScreen}
          isPending={deleteScreen.isPending}
        />
      )}

      {/* Scheme create/edit modal */}
      {showSchemeModal && (
        <SchemeModal
          scheme={editingScheme}
          onClose={() => { setShowSchemeModal(false); setEditingScheme(null); }}
          onSave={handleSaveScheme}
          isPending={createScheme.isPending || updateScheme.isPending}
        />
      )}

      {/* Scheme delete confirmation */}
      {deletingScheme && (
        <DeleteConfirmModal
          entityName={deletingScheme.name}
          entityLabel="Screen Scheme"
          onCancel={() => setDeletingScheme(null)}
          onConfirm={handleDeleteScheme}
          isPending={deleteScheme.isPending}
        />
      )}
    </AdminLayout>
  );
}
