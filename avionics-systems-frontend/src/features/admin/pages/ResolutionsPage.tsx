import React, { useState } from 'react';
import {
  useResolutions,
  useCreateResolution,
  useUpdateResolution,
  useDeleteResolution,
} from '../hooks/useAdminApi';
import { Resolution } from '../../../api/issueApi';
import './AdminIssueConfig.css';

interface ResolutionFormData {
  name: string;
  resolutionKey: string;
  description: string;
  sequence: number;
  isDefault: boolean;
}

const emptyForm: ResolutionFormData = {
  name: '',
  resolutionKey: '',
  description: '',
  sequence: 0,
  isDefault: false,
};

function toKey(name: string): string {
  return name
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_|_$/g, '');
}

export default function ResolutionsPage() {
  const [search, setSearch] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  const [selectedResolution, setSelectedResolution] = useState<Resolution | null>(null);
  const [formData, setFormData] = useState<ResolutionFormData>(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const { data: resolutions, isLoading, isError, refetch } = useResolutions();
  const createResolution = useCreateResolution();
  const updateResolution = useUpdateResolution();
  const deleteResolution = useDeleteResolution();

  const filtered =
    resolutions?.filter((r) =>
      r.name.toLowerCase().includes(search.toLowerCase())
    ) ?? [];

  const showMessage = (msg: string, isErr = false) => {
    if (isErr) {
      setError(msg);
      setSuccess(null);
    } else {
      setSuccess(msg);
      setError(null);
    }
    setTimeout(() => {
      setError(null);
      setSuccess(null);
    }, 3000);
  };

  const openCreateModal = () => {
    setModalMode('create');
    setSelectedResolution(null);
    const nextSequence =
      resolutions && resolutions.length > 0
        ? Math.max(...resolutions.map((r) => r.sequence)) + 1
        : 1;
    setFormData({ ...emptyForm, sequence: nextSequence });
    setShowModal(true);
  };

  const openEditModal = (resolution: Resolution) => {
    setModalMode('edit');
    setSelectedResolution(resolution);
    setFormData({
      name: resolution.name,
      resolutionKey: resolution.resolutionKey || toKey(resolution.name),
      description: resolution.description || '',
      sequence: resolution.sequence,
      isDefault: resolution.isDefault || false,
    });
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setSelectedResolution(null);
    setFormData(emptyForm);
    setError(null);
  };

  const handleNameChange = (value: string) => {
    const updates: Partial<ResolutionFormData> = { name: value };
    if (modalMode === 'create') {
      updates.resolutionKey = toKey(value);
    }
    setFormData((prev) => ({ ...prev, ...updates }));
  };

  const handleCreate = async () => {
    if (!formData.name.trim()) {
      setError('Resolution name is required');
      return;
    }
    try {
      await createResolution.mutateAsync({
        name: formData.name.trim(),
        resolutionKey: formData.resolutionKey || toKey(formData.name),
        description: formData.description.trim() || undefined,
        sequence: formData.sequence,
        isDefault: formData.isDefault,
      });
      showMessage(`Resolution "${formData.name}" created successfully`);
      closeModal();
    } catch (err: any) {
      showMessage(err?.message || 'Failed to create resolution', true);
    }
  };

  const handleUpdate = async () => {
    if (!formData.name.trim()) {
      setError('Resolution name is required');
      return;
    }
    try {
      await updateResolution.mutateAsync({
        id: selectedResolution!.id,
        data: {
          name: formData.name.trim(),
          resolutionKey: formData.resolutionKey || toKey(formData.name),
          description: formData.description.trim() || undefined,
          sequence: formData.sequence,
          isDefault: formData.isDefault,
        },
      });
      showMessage(`Resolution "${formData.name}" updated successfully`);
      closeModal();
    } catch (err: any) {
      showMessage(err?.message || 'Failed to update resolution', true);
    }
  };

  const handleDelete = async (resolution: Resolution) => {
    if (resolution.isDefault) {
      showMessage('Cannot delete the default resolution. Change the default first.', true);
      return;
    }
    if (
      !confirm(
        `Are you sure you want to delete the resolution "${resolution.name}"?\n\nThis cannot be undone.`
      )
    )
      return;
    try {
      await deleteResolution.mutateAsync(resolution.id);
      showMessage(`Resolution "${resolution.name}" deleted successfully`);
    } catch (err: any) {
      showMessage(err?.message || 'Failed to delete resolution', true);
    }
  };

  const defaultCount = resolutions?.filter((r) => r.isDefault).length || 0;

  return (
    <div className="dc-page ab-issue-config-page">
      <header className="dc-page-header">
        <h1 className="dc-page-title">Resolutions</h1>
        <p className="dc-page-subtitle">
          Resolutions describe how issues were closed (Fixed, Won&apos;t fix, Duplicate, etc.) —
          Systems Data Center issue settings.
        </p>
      </header>

      {error && <div className="admin-alert admin-alert-error">{error}</div>}
      {success && <div className="admin-alert admin-alert-success">{success}</div>}

      <div className="admin-stats-grid">
        <div className="admin-stat-card">
          <div className="admin-stat-value">{resolutions?.length || 0}</div>
          <div className="admin-stat-label">Total Resolutions</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-value">{defaultCount}</div>
          <div className="admin-stat-label">Default</div>
        </div>
      </div>

      <div className="admin-toolbar">
        <div className="admin-toolbar-left">
          <input
            type="search"
            className="admin-search-input-toolbar"
            placeholder="Search resolutions..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="admin-toolbar-right">
          <button type="button" className="admin-btn-primary" onClick={openCreateModal}>
            Add resolution
          </button>
        </div>
      </div>

      {isLoading && <p className="ab-issue-config-muted">Loading...</p>}
      {isError && (
        <div className="ab-issue-config-error">
          <p>Failed to load resolutions.</p>
          <button type="button" className="admin-btn-secondary" onClick={() => refetch()}>
            Retry
          </button>
        </div>
      )}

      {!isLoading && !isError && (
        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Key</th>
                <th>Description</th>
                <th>Order</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={5} className="ab-issue-config-empty-cell">
                    No resolutions found.
                  </td>
                </tr>
              ) : (
                filtered.map((r: Resolution) => (
                  <tr key={r.id}>
                    <td>
                      <div className="role-cell">
                        <span className="role-name">{r.name}</span>
                        {r.isDefault && <span className="role-default-badge">Default</span>}
                      </div>
                    </td>
                    <td>{r.resolutionKey || '—'}</td>
                    <td>{r.description || '—'}</td>
                    <td>{r.sequence}</td>
                    <td>
                      <div className="action-buttons">
                        <button
                          className="admin-btn-secondary admin-btn-sm"
                          onClick={() => openEditModal(r)}
                        >
                          Edit
                        </button>
                        {!r.isDefault && (
                          <button
                            className="admin-btn-danger admin-btn-sm"
                            onClick={() => handleDelete(r)}
                          >
                            Delete
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {showModal && (
        <div className="admin-modal-overlay" onClick={closeModal}>
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h2 className="admin-modal-title">
                {modalMode === 'create' ? 'Add Resolution' : 'Edit Resolution'}
              </h2>
              <button className="admin-modal-close" onClick={closeModal}>
                &times;
              </button>
            </div>
            <div className="admin-modal-body">
              <div className="admin-form-group">
                <label className="admin-form-label admin-form-label-required">Name</label>
                <input
                  type="text"
                  className="admin-form-input"
                  value={formData.name}
                  onChange={(e) => handleNameChange(e.target.value)}
                  placeholder='e.g., Fixed, Won&#39;t Fix, Duplicate'
                />
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label">Key</label>
                <input
                  type="text"
                  className="admin-form-input"
                  value={formData.resolutionKey}
                  onChange={(e) => setFormData({ ...formData, resolutionKey: e.target.value })}
                  placeholder="Auto-generated from name"
                  readOnly={modalMode === 'create'}
                  style={modalMode === 'create' ? { background: 'var(--sa-n20, #f4f5f7)', cursor: 'default' } : undefined}
                />
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label">Description</label>
                <textarea
                  className="admin-form-textarea"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Describe this resolution..."
                  rows={3}
                />
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label">Sort Order</label>
                <input
                  type="number"
                  className="admin-form-input"
                  value={formData.sequence}
                  onChange={(e) =>
                    setFormData({ ...formData, sequence: parseInt(e.target.value, 10) || 0 })
                  }
                  min={0}
                />
              </div>
              <div className="admin-form-group">
                <label
                  className="admin-form-label"
                  style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}
                >
                  <input
                    type="checkbox"
                    checked={formData.isDefault}
                    onChange={(e) => setFormData({ ...formData, isDefault: e.target.checked })}
                  />
                  Is Default
                </label>
              </div>
            </div>
            <div className="admin-modal-footer">
              <button className="admin-btn-secondary" onClick={closeModal}>
                Cancel
              </button>
              <button
                className="admin-btn-primary"
                onClick={modalMode === 'create' ? handleCreate : handleUpdate}
                disabled={
                  modalMode === 'create'
                    ? createResolution.isPending
                    : updateResolution.isPending
                }
              >
                {modalMode === 'create'
                  ? createResolution.isPending
                    ? 'Creating...'
                    : 'Create'
                  : updateResolution.isPending
                    ? 'Updating...'
                    : 'Update'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
