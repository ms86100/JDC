import React, { useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import {
  useStatuses,
  useCreateStatus,
  useUpdateStatus,
  useDeleteStatus,
  Status,
} from '../hooks/useAdminApi';
import './StatusesPage.css';

interface StatusFormData {
  name: string;
  statusKey: string;
  description: string;
  statusCategory: string;
  statusColor: string;
  iconUrl: string;
  sequence: number;
}

const EMPTY_FORM: StatusFormData = {
  name: '',
  statusKey: '',
  description: '',
  statusCategory: 'TODO',
  statusColor: '#4a9df8',
  iconUrl: '',
  sequence: 0,
};

function generateKey(name: string): string {
  return name
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_|_$/g, '');
}

export default function StatusesPage() {
  const [search, setSearch] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  const [selectedStatus, setSelectedStatus] = useState<Status | null>(null);
  const [formData, setFormData] = useState<StatusFormData>({ ...EMPTY_FORM });
  const [deleteConfirm, setDeleteConfirm] = useState<Status | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const { data: statuses, isLoading } = useStatuses();
  const createStatus = useCreateStatus();
  const updateStatus = useUpdateStatus();
  const deleteStatus = useDeleteStatus();

  const filteredStatuses = statuses?.filter(s =>
    s.name.toLowerCase().includes(search.toLowerCase())
  ) || [];

  const showMessage = (msg: string, isError = false) => {
    if (isError) {
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
    setSelectedStatus(null);
    setFormData({ ...EMPTY_FORM, sequence: (statuses?.length || 0) + 1 });
    setShowModal(true);
  };

  const openEditModal = (status: Status) => {
    setModalMode('edit');
    setSelectedStatus(status);
    setFormData({
      name: status.name,
      statusKey: status.statusKey || generateKey(status.name),
      description: status.description || '',
      statusCategory: status.statusCategory,
      statusColor: status.statusColor || '#4a9df8',
      iconUrl: status.iconUrl || '',
      sequence: status.sequence,
    });
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setSelectedStatus(null);
    setFormData({ ...EMPTY_FORM });
    setError(null);
  };

  const handleNameChange = (name: string) => {
    const update: Partial<StatusFormData> = { name };
    if (modalMode === 'create') {
      update.statusKey = generateKey(name);
    }
    setFormData(prev => ({ ...prev, ...update }));
  };

  const handleCreate = async () => {
    if (!formData.name.trim()) {
      setError('Status name is required');
      return;
    }
    try {
      await createStatus.mutateAsync({
        name: formData.name,
        statusKey: formData.statusKey,
        description: formData.description,
        statusCategory: formData.statusCategory,
        statusColor: formData.statusColor,
        iconUrl: formData.iconUrl,
        sequence: formData.sequence,
      });
      showMessage(`Status "${formData.name}" created successfully`);
      closeModal();
    } catch (err: any) {
      const msg =
        err?.response?.data?.message || err?.message || 'Failed to create status';
      showMessage(msg, true);
    }
  };

  const handleUpdate = async () => {
    if (!formData.name.trim()) {
      setError('Status name is required');
      return;
    }
    try {
      await updateStatus.mutateAsync({
        id: selectedStatus!.id,
        data: {
          name: formData.name,
          statusKey: formData.statusKey,
          description: formData.description,
          statusCategory: formData.statusCategory,
          statusColor: formData.statusColor,
          iconUrl: formData.iconUrl,
          sequence: formData.sequence,
        },
      });
      showMessage(`Status "${formData.name}" updated successfully`);
      closeModal();
    } catch (err: any) {
      const msg =
        err?.response?.data?.message || err?.message || 'Failed to update status';
      showMessage(msg, true);
    }
  };

  const handleDelete = async () => {
    if (!deleteConfirm) return;
    try {
      await deleteStatus.mutateAsync(deleteConfirm.id);
      showMessage(`Status "${deleteConfirm.name}" deleted successfully`);
      setDeleteConfirm(null);
    } catch (err: any) {
      const msg =
        err?.response?.data?.message || err?.message || 'Failed to delete status';
      showMessage(msg, true);
      setDeleteConfirm(null);
    }
  };

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">Statuses</h1>
          <p className="admin-page-description">
            Manage issue statuses and their categories.
          </p>
        </div>

        {error && <div className="admin-alert admin-alert-error">{error}</div>}
        {success && <div className="admin-alert admin-alert-success">{success}</div>}

        <div className="admin-stats-grid">
          <div className="admin-stat-card">
            <div className="admin-stat-label">Total Statuses</div>
            <div className="admin-stat-value">{statuses?.length || 0}</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">To Do</div>
            <div className="admin-stat-value">
              {statuses?.filter(s => s.statusCategory === 'TODO').length || 0}
            </div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">In Progress</div>
            <div className="admin-stat-value">
              {statuses?.filter(s => s.statusCategory === 'IN_PROGRESS').length || 0}
            </div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Done</div>
            <div className="admin-stat-value">
              {statuses?.filter(s => s.statusCategory === 'DONE').length || 0}
            </div>
          </div>
        </div>

        <div className="admin-toolbar">
          <div className="admin-toolbar-left">
            <input
              type="text"
              placeholder="Search statuses..."
              className="admin-search-input-toolbar"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="admin-toolbar-right">
            <button className="admin-btn-primary" onClick={openCreateModal}>Add Status</button>
          </div>
        </div>

        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Status</th>
                <th>Description</th>
                <th>Category</th>
                <th>Color</th>
                <th>Sequence</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <>
                  {[...Array(8)].map((_, i) => (
                    <tr key={i}>
                      <td style={{ padding: '12px 16px' }}><div style={{ display: 'flex', alignItems: 'center', gap: 8 }}><div className="ab-skeleton" style={{ height: 24, width: 24, borderRadius: '50%', flexShrink: 0 }} /><div className="ab-skeleton" style={{ height: 16, width: '50%', borderRadius: 'var(--sa-radius-sm)' }} /></div></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: '70%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 22, width: 80, borderRadius: 12 }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 60, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 30, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 100, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    </tr>
                  ))}
                </>
              ) : filteredStatuses.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '24px' }}>No statuses found</td>
                </tr>
              ) : (
                filteredStatuses.map((status) => (
                  <tr key={status.id}>
                    <td>
                      <div className="status-cell">
                        <span className="status-icon" style={{ backgroundColor: status.statusColor }}>
                          {status.name.charAt(0)}
                        </span>
                        <div>
                          <span className="status-name">{status.name}</span>
                          {status.isSystem && <span className="status-system-badge">System</span>}
                        </div>
                      </div>
                    </td>
                    <td className="description-cell">{status.description || 'No description'}</td>
                    <td>
                      <span className={`status-category category-${status.statusCategory.toLowerCase()}`}>
                        {status.statusCategory.replace('_', ' ')}
                      </span>
                    </td>
                    <td>
                      <div className="status-color-cell">
                        <span className="status-color-swatch" style={{ backgroundColor: status.statusColor }} />
                        <span className="status-color-hex">{status.statusColor}</span>
                      </div>
                    </td>
                    <td>{status.sequence}</td>
                    <td>
                      <div className="action-buttons">
                        <button className="admin-btn-secondary" onClick={() => openEditModal(status)}>Edit</button>
                        {!status.isSystem && (
                          <button className="admin-btn-danger" onClick={() => setDeleteConfirm(status)}>Delete</button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Create/Edit Modal */}
        {showModal && (
          <div className="admin-modal-overlay" onClick={closeModal}>
            <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
              <div className="admin-modal-header">
                <h2 className="admin-modal-title">
                  {modalMode === 'create' ? 'Add Status' : 'Edit Status'}
                </h2>
                <button className="admin-modal-close" onClick={closeModal}>x</button>
              </div>
              <div className="admin-modal-body">
                <div className="admin-form-group">
                  <label className="admin-form-label admin-form-label-required">Name</label>
                  <input
                    type="text"
                    className="admin-form-input"
                    value={formData.name}
                    onChange={(e) => handleNameChange(e.target.value)}
                    placeholder="e.g., Open, In Review, Closed"
                  />
                </div>
                <div className="admin-form-group">
                  <label className="admin-form-label">Key</label>
                  <input
                    type="text"
                    className="admin-form-input"
                    value={formData.statusKey}
                    onChange={(e) => setFormData({ ...formData, statusKey: e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '_') })}
                    placeholder="e.g., OPEN, IN_REVIEW"
                  />
                  <span className="admin-form-hint">Auto-generated from name. Uppercase letters, digits, and underscores only.</span>
                </div>
                <div className="admin-form-group">
                  <label className="admin-form-label admin-form-label-required">Category</label>
                  <select
                    className="admin-form-input"
                    value={formData.statusCategory}
                    onChange={(e) => setFormData({ ...formData, statusCategory: e.target.value })}
                  >
                    <option value="TODO">TODO</option>
                    <option value="IN_PROGRESS">IN_PROGRESS</option>
                    <option value="DONE">DONE</option>
                  </select>
                </div>
                <div className="admin-form-group">
                  <label className="admin-form-label">Color</label>
                  <div className="status-color-picker-row">
                    <input
                      type="color"
                      className="status-color-picker-input"
                      value={formData.statusColor}
                      onChange={(e) => setFormData({ ...formData, statusColor: e.target.value })}
                    />
                    <input
                      type="text"
                      className="admin-form-input"
                      value={formData.statusColor}
                      onChange={(e) => setFormData({ ...formData, statusColor: e.target.value })}
                      placeholder="#4a9df8"
                    />
                  </div>
                </div>
                <div className="admin-form-group">
                  <label className="admin-form-label">Icon</label>
                  <input
                    type="text"
                    className="admin-form-input"
                    value={formData.iconUrl}
                    onChange={(e) => setFormData({ ...formData, iconUrl: e.target.value })}
                    placeholder="Icon name or URL"
                  />
                </div>
                <div className="admin-form-group">
                  <label className="admin-form-label">Description</label>
                  <textarea
                    className="admin-form-textarea"
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="Describe what this status represents..."
                    rows={3}
                  />
                </div>
                <div className="admin-form-group">
                  <label className="admin-form-label">Sort Order</label>
                  <input
                    type="number"
                    className="admin-form-input"
                    value={formData.sequence}
                    onChange={(e) => setFormData({ ...formData, sequence: parseInt(e.target.value, 10) || 0 })}
                    min={0}
                  />
                </div>
              </div>
              <div className="admin-modal-footer">
                <button className="admin-btn-secondary" onClick={closeModal}>Cancel</button>
                <button
                  className="admin-btn-primary"
                  onClick={modalMode === 'create' ? handleCreate : handleUpdate}
                  disabled={modalMode === 'create' ? createStatus.isPending : updateStatus.isPending}
                >
                  {modalMode === 'create'
                    ? (createStatus.isPending ? 'Creating...' : 'Create')
                    : (updateStatus.isPending ? 'Updating...' : 'Update')}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Delete Confirmation Modal */}
        {deleteConfirm && (
          <div className="admin-modal-overlay" onClick={() => setDeleteConfirm(null)}>
            <div className="admin-modal admin-modal-confirm" onClick={(e) => e.stopPropagation()}>
              <div className="admin-modal-header">
                <h2 className="admin-modal-title">Delete Status</h2>
                <button className="admin-modal-close" onClick={() => setDeleteConfirm(null)}>x</button>
              </div>
              <div className="admin-modal-body">
                <p>
                  Are you sure you want to delete status &lsquo;{deleteConfirm.name}&rsquo;? This cannot be undone.
                </p>
              </div>
              <div className="admin-modal-footer">
                <button className="admin-btn-secondary" onClick={() => setDeleteConfirm(null)}>Cancel</button>
                <button
                  className="admin-btn-danger"
                  onClick={handleDelete}
                  disabled={deleteStatus.isPending}
                >
                  {deleteStatus.isPending ? 'Deleting...' : 'Delete'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  );
}
