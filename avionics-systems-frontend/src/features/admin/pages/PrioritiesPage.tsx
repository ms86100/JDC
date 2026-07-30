import React, { useState, useMemo } from 'react';
import { usePriorities, useCreatePriority, useUpdatePriority, useDeletePriority, Priority } from '../hooks/useAdminApi';
import AdminLayout from '../components/AdminLayout';
import './PrioritiesPage.css';

interface PriorityFormData {
  name: string;
  key: string;
  description: string;
  statusColor: string;
  iconUrl: string;
  sequence: number;
  isDefault: boolean;
}

const EMPTY_FORM: PriorityFormData = {
  name: '',
  key: '',
  description: '',
  statusColor: '#0065FF',
  iconUrl: '',
  sequence: 0,
  isDefault: false,
};

function generateKey(name: string): string {
  return name
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
}

export default function PrioritiesPage() {
  const [search, setSearch] = useState('');
  const { data: priorities, isLoading, isError } = usePriorities();
  const createMutation = useCreatePriority();
  const updateMutation = useUpdatePriority();
  const deleteMutation = useDeletePriority();

  const [showModal, setShowModal] = useState(false);
  const [editingPriority, setEditingPriority] = useState<Priority | null>(null);
  const [formData, setFormData] = useState<PriorityFormData>(EMPTY_FORM);
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const filteredPriorities = useMemo(() => {
    const list = priorities || [];
    const filtered = list.filter(p =>
      p.name.toLowerCase().includes(search.toLowerCase())
    );
    return [...filtered].sort((a, b) => (a.sequence ?? 0) - (b.sequence ?? 0));
  }, [priorities, search]);

  const totalCount = priorities?.length ?? 0;
  const defaultPriority = priorities?.find(p => p.isDefault);

  // --- Modal handlers ---

  const openCreateModal = () => {
    setEditingPriority(null);
    setFormData({
      ...EMPTY_FORM,
      sequence: totalCount + 1,
    });
    setActionError(null);
    setShowModal(true);
  };

  const openEditModal = (priority: Priority) => {
    setEditingPriority(priority);
    setFormData({
      name: priority.name,
      key: priority.name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, ''),
      description: priority.description || '',
      statusColor: priority.statusColor || '#0065FF',
      iconUrl: priority.iconUrl || '',
      sequence: priority.sequence ?? 0,
      isDefault: priority.isDefault ?? false,
    });
    setActionError(null);
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setEditingPriority(null);
    setFormData(EMPTY_FORM);
    setActionError(null);
  };

  const handleNameChange = (name: string) => {
    setFormData(prev => ({
      ...prev,
      name,
      key: editingPriority ? prev.key : generateKey(name),
    }));
  };

  const handleSave = () => {
    if (!formData.name.trim()) return;

    const payload: Partial<Priority> = {
      name: formData.name.trim(),
      description: formData.description.trim(),
      statusColor: formData.statusColor,
      iconUrl: formData.iconUrl.trim(),
      sequence: formData.sequence,
      isDefault: formData.isDefault,
    };

    if (editingPriority) {
      updateMutation.mutate(
        { id: editingPriority.id, data: payload },
        {
          onSuccess: () => {
            setActionSuccess(`Priority "${formData.name}" updated successfully.`);
            closeModal();
            setTimeout(() => setActionSuccess(null), 4000);
          },
          onError: (err: unknown) => {
            const msg =
              (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
              (err instanceof Error ? err.message : 'Failed to update priority');
            setActionError(msg);
          },
        }
      );
    } else {
      createMutation.mutate(payload, {
        onSuccess: () => {
          setActionSuccess(`Priority "${formData.name}" created successfully.`);
          closeModal();
          setTimeout(() => setActionSuccess(null), 4000);
        },
        onError: (err: unknown) => {
          const msg =
            (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
            (err instanceof Error ? err.message : 'Failed to create priority');
          setActionError(msg);
        },
      });
    }
  };

  const handleDelete = (priority: Priority) => {
    setActionError(null);
    deleteMutation.mutate(priority.id, {
      onSuccess: () => {
        setDeleteConfirm(null);
        setActionSuccess(`Priority "${priority.name}" deleted successfully.`);
        setTimeout(() => setActionSuccess(null), 4000);
      },
      onError: (err: unknown) => {
        const msg =
          (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
          (err instanceof Error ? err.message : 'Failed to delete priority');
        setDeleteConfirm(null);
        setActionError(msg);
      },
    });
  };

  const isSaving = createMutation.isPending || updateMutation.isPending;

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">Priorities</h1>
          <p className="admin-page-description">
            Configure issue priorities and their display order.
          </p>
        </div>

        {/* Stats */}
        <div className="admin-stats-grid">
          <div className="admin-stat-card">
            <div className="admin-stat-value">{totalCount}</div>
            <div className="admin-stat-label">Total Priorities</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-value">{defaultPriority?.name || 'None'}</div>
            <div className="admin-stat-label">Default Priority</div>
          </div>
        </div>

        {/* Alerts */}
        {actionSuccess && (
          <div className="admin-alert admin-alert-success">
            {actionSuccess}
            <button type="button" className="admin-alert-dismiss" onClick={() => setActionSuccess(null)}>&times;</button>
          </div>
        )}
        {actionError && !showModal && (
          <div className="admin-alert admin-alert-error">
            {actionError}
            <button type="button" className="admin-alert-dismiss" onClick={() => setActionError(null)}>&times;</button>
          </div>
        )}

        {/* Toolbar */}
        <div className="admin-toolbar">
          <div className="admin-toolbar-left">
            <input
              type="text"
              placeholder="Search priorities..."
              className="admin-search-input"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="admin-toolbar-right">
            <button className="admin-btn-primary" onClick={openCreateModal}>Add Priority</button>
          </div>
        </div>

        {/* Table */}
        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Priority</th>
                <th>Description</th>
                <th>Color</th>
                <th>Sort Order</th>
                <th>Default</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <>
                  {[...Array(5)].map((_, i) => (
                    <tr key={i}>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: '60%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: '80%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 60, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 40, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 50, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 80, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    </tr>
                  ))}
                </>
              ) : isError ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '24px', color: 'var(--sa-n600)' }}>
                    Failed to load priorities. Please check if the server is running.
                  </td>
                </tr>
              ) : filteredPriorities.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '24px', color: 'var(--sa-n600)' }}>
                    {search ? 'No priorities match your search.' : 'No priorities configured yet.'}
                  </td>
                </tr>
              ) : (
                filteredPriorities.map((priority) => (
                  <tr key={priority.id}>
                    <td>
                      <div className="priority-name-cell">
                        <span
                          className="priority-color-dot"
                          style={{ backgroundColor: priority.statusColor || '#999' }}
                        />
                        <span className="priority-name">{priority.name}</span>
                      </div>
                    </td>
                    <td className="description-cell">{priority.description || 'No description'}</td>
                    <td>
                      <span className="priority-color-swatch" style={{ backgroundColor: priority.statusColor }}>
                        {priority.statusColor}
                      </span>
                    </td>
                    <td>{priority.sequence ?? '-'}</td>
                    <td>
                      {priority.isDefault ? (
                        <span className="admin-status admin-status-active">Default</span>
                      ) : (
                        <span style={{ color: 'var(--sa-n500)', fontSize: 13 }}>-</span>
                      )}
                    </td>
                    <td>
                      <div className="action-buttons">
                        <button className="admin-btn-secondary admin-btn-sm" onClick={() => openEditModal(priority)}>
                          Edit
                        </button>
                        {!priority.isDefault && (
                          <button className="admin-btn-danger admin-btn-sm" onClick={() => setDeleteConfirm(priority.id)}>
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

        {/* Delete Confirmation Modal */}
        {deleteConfirm && (
          <div className="admin-modal-overlay" onClick={() => setDeleteConfirm(null)}>
            <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
              <div className="admin-modal-header">
                <h3>Delete Priority</h3>
                <button onClick={() => setDeleteConfirm(null)}>&times;</button>
              </div>
              <div className="admin-modal-body">
                <p>
                  Are you sure you want to delete the priority{' '}
                  <strong>{priorities?.find(p => p.id === deleteConfirm)?.name}</strong>?
                  This action cannot be undone.
                </p>
              </div>
              <div className="admin-modal-footer">
                <button className="admin-btn-secondary" onClick={() => setDeleteConfirm(null)}>
                  Cancel
                </button>
                <button
                  className="admin-btn-danger"
                  onClick={() => {
                    const target = priorities?.find(p => p.id === deleteConfirm);
                    if (target) handleDelete(target);
                  }}
                  disabled={deleteMutation.isPending}
                >
                  {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Create / Edit Modal */}
        {showModal && (
          <div className="admin-modal-overlay" onClick={closeModal}>
            <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
              <div className="admin-modal-header">
                <h3>{editingPriority ? 'Edit Priority' : 'Add Priority'}</h3>
                <button onClick={closeModal}>&times;</button>
              </div>
              <div className="admin-modal-body">
                {actionError && (
                  <div className="admin-alert admin-alert-error" style={{ marginBottom: 16 }}>
                    {actionError}
                  </div>
                )}

                <div className="admin-form-group">
                  <label className="admin-form-label">Name <span style={{ color: '#d73a49' }}>*</span></label>
                  <input
                    className="admin-form-input"
                    type="text"
                    value={formData.name}
                    onChange={(e) => handleNameChange(e.target.value)}
                    placeholder="e.g., Highest, High, Medium, Low"
                  />
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">Key</label>
                  <input
                    className="admin-form-input"
                    type="text"
                    value={formData.key}
                    readOnly
                    style={{ backgroundColor: 'var(--sa-n100, #f4f5f7)', cursor: 'default' }}
                  />
                  <span style={{ fontSize: 12, color: 'var(--sa-n500)' }}>Auto-generated from name</span>
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">Description</label>
                  <textarea
                    className="admin-form-input"
                    value={formData.description}
                    onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
                    placeholder="Describe when this priority should be used"
                    rows={3}
                    style={{ resize: 'vertical' }}
                  />
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">Color</label>
                  <div className="priority-color-input-row">
                    <input
                      type="color"
                      value={formData.statusColor}
                      onChange={(e) => setFormData(prev => ({ ...prev, statusColor: e.target.value }))}
                      className="priority-color-picker"
                    />
                    <input
                      className="admin-form-input"
                      type="text"
                      value={formData.statusColor}
                      onChange={(e) => setFormData(prev => ({ ...prev, statusColor: e.target.value }))}
                      placeholder="#FF5630"
                      style={{ flex: 1 }}
                    />
                    <span
                      className="priority-color-preview"
                      style={{ backgroundColor: formData.statusColor }}
                    />
                  </div>
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">Icon URL</label>
                  <input
                    className="admin-form-input"
                    type="text"
                    value={formData.iconUrl}
                    onChange={(e) => setFormData(prev => ({ ...prev, iconUrl: e.target.value }))}
                    placeholder="https://example.com/icon.svg"
                  />
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">Sort Order</label>
                  <input
                    className="admin-form-input"
                    type="number"
                    min={0}
                    value={formData.sequence}
                    onChange={(e) => setFormData(prev => ({ ...prev, sequence: parseInt(e.target.value, 10) || 0 }))}
                  />
                  <span style={{ fontSize: 12, color: 'var(--sa-n500)' }}>Lower numbers appear first (higher priority)</span>
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label-checkbox">
                    <input
                      type="checkbox"
                      checked={formData.isDefault}
                      onChange={(e) => setFormData(prev => ({ ...prev, isDefault: e.target.checked }))}
                    />
                    <span>Set as default priority</span>
                  </label>
                  <span style={{ fontSize: 12, color: 'var(--sa-n500)' }}>Only one priority can be the default. Setting this will unset the current default.</span>
                </div>
              </div>
              <div className="admin-modal-footer">
                <button className="admin-btn-secondary" onClick={closeModal}>
                  Cancel
                </button>
                <button
                  className="admin-btn-primary"
                  onClick={handleSave}
                  disabled={!formData.name.trim() || isSaving}
                >
                  {isSaving
                    ? (editingPriority ? 'Saving...' : 'Adding...')
                    : (editingPriority ? 'Save Changes' : 'Add Priority')
                  }
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  );
}
