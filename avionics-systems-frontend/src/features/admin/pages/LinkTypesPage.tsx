import React, { useState } from 'react';
import {
  useLinkTypes,
  useCreateLinkType,
  useUpdateLinkType,
  useDeleteLinkType,
  LinkType,
} from '../hooks/useAdminApi';
import './AdminIssueConfig.css';

interface LinkTypeFormData {
  linkKey: string;
  outwardName: string;
  inwardName: string;
  description: string;
  sortOrder: number;
}

const emptyForm: LinkTypeFormData = {
  linkKey: '',
  outwardName: '',
  inwardName: '',
  description: '',
  sortOrder: 0,
};

function toKey(name: string): string {
  return name
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_|_$/g, '');
}

export default function LinkTypesPage() {
  const [search, setSearch] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  const [selectedLinkType, setSelectedLinkType] = useState<LinkType | null>(null);
  const [formData, setFormData] = useState<LinkTypeFormData>(emptyForm);
  const [deleteConfirm, setDeleteConfirm] = useState<LinkType | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const { data: linkTypes, isLoading, isError, refetch } = useLinkTypes();
  const createLinkType = useCreateLinkType();
  const updateLinkType = useUpdateLinkType();
  const deleteLinkType = useDeleteLinkType();

  const filtered =
    linkTypes?.filter(
      (lt) =>
        lt.outwardName.toLowerCase().includes(search.toLowerCase()) ||
        lt.inwardName.toLowerCase().includes(search.toLowerCase()) ||
        lt.linkKey.toLowerCase().includes(search.toLowerCase())
    ) ?? [];

  const totalCount = linkTypes?.length || 0;
  const activeCount = linkTypes?.filter((lt) => lt.isActive).length || 0;
  const systemCount = linkTypes?.filter((lt) => lt.isSystem).length || 0;

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
    setSelectedLinkType(null);
    const nextSortOrder =
      linkTypes && linkTypes.length > 0
        ? Math.max(...linkTypes.map((lt) => lt.sortOrder)) + 1
        : 1;
    setFormData({ ...emptyForm, sortOrder: nextSortOrder });
    setShowModal(true);
  };

  const openEditModal = (linkType: LinkType) => {
    setModalMode('edit');
    setSelectedLinkType(linkType);
    setFormData({
      linkKey: linkType.linkKey,
      outwardName: linkType.outwardName,
      inwardName: linkType.inwardName,
      description: linkType.description || '',
      sortOrder: linkType.sortOrder,
    });
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setSelectedLinkType(null);
    setFormData(emptyForm);
    setError(null);
  };

  const handleOutwardNameChange = (value: string) => {
    const updates: Partial<LinkTypeFormData> = { outwardName: value };
    if (modalMode === 'create') {
      updates.linkKey = toKey(value);
    }
    setFormData((prev) => ({ ...prev, ...updates }));
  };

  const handleCreate = async () => {
    if (!formData.outwardName.trim() || !formData.inwardName.trim() || !formData.linkKey.trim()) {
      setError('Link Key, Outward Name, and Inward Name are required');
      return;
    }
    try {
      await createLinkType.mutateAsync({
        linkKey: formData.linkKey.trim(),
        outwardName: formData.outwardName.trim(),
        inwardName: formData.inwardName.trim(),
        description: formData.description.trim() || undefined,
        sortOrder: formData.sortOrder,
      });
      showMessage(`Link type "${formData.outwardName}" created successfully`);
      closeModal();
    } catch (err: any) {
      showMessage(
        err?.response?.data?.message || err?.message || 'Failed to create link type',
        true
      );
    }
  };

  const handleUpdate = async () => {
    if (!formData.outwardName.trim() || !formData.inwardName.trim() || !formData.linkKey.trim()) {
      setError('Link Key, Outward Name, and Inward Name are required');
      return;
    }
    try {
      await updateLinkType.mutateAsync({
        id: selectedLinkType!.id,
        data: {
          linkKey: formData.linkKey.trim(),
          outwardName: formData.outwardName.trim(),
          inwardName: formData.inwardName.trim(),
          description: formData.description.trim() || undefined,
          sortOrder: formData.sortOrder,
        },
      });
      showMessage(`Link type "${formData.outwardName}" updated successfully`);
      closeModal();
    } catch (err: any) {
      showMessage(
        err?.response?.data?.message || err?.message || 'Failed to update link type',
        true
      );
    }
  };

  const handleDelete = async (linkType: LinkType) => {
    try {
      await deleteLinkType.mutateAsync(linkType.id);
      showMessage(`Link type "${linkType.outwardName}" deleted successfully`);
    } catch (err: any) {
      showMessage(
        err?.response?.data?.message || err?.message || 'Failed to delete link type',
        true
      );
    }
    setDeleteConfirm(null);
  };

  const openDeleteConfirm = (linkType: LinkType) => {
    if (linkType.isSystem) {
      showMessage('System link types cannot be deleted.', true);
      return;
    }
    setDeleteConfirm(linkType);
  };

  return (
    <div className="dc-page ab-issue-config-page">
      <header className="dc-page-header">
        <h1 className="dc-page-title">Issue Link Types</h1>
        <p className="dc-page-subtitle">
          Configure how issues can be linked to each other (blocks, clones, duplicates, relates to,
          etc.) — Systems Data Center issue settings.
        </p>
      </header>

      {error && <div className="admin-alert admin-alert-error">{error}</div>}
      {success && <div className="admin-alert admin-alert-success">{success}</div>}

      <div className="admin-stats-grid">
        <div className="admin-stat-card">
          <div className="admin-stat-value">{totalCount}</div>
          <div className="admin-stat-label">Total Link Types</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-value">{activeCount}</div>
          <div className="admin-stat-label">Active</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-value">{systemCount}</div>
          <div className="admin-stat-label">System</div>
        </div>
      </div>

      <div className="admin-toolbar">
        <div className="admin-toolbar-left">
          <input
            type="search"
            className="admin-search-input-toolbar"
            placeholder="Search link types..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="admin-toolbar-right">
          <button type="button" className="admin-btn-primary" onClick={openCreateModal}>
            Add Link Type
          </button>
        </div>
      </div>

      {isLoading && <p className="ab-issue-config-muted">Loading...</p>}
      {isError && (
        <div className="ab-issue-config-error">
          <p>Failed to load link types.</p>
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
                <th>Outward Name</th>
                <th>Inward Name</th>
                <th>Key</th>
                <th>System</th>
                <th>Active</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan={6} className="ab-issue-config-empty-cell">
                    {search ? 'No link types match your search.' : 'No link types found. Click "Add Link Type" to create one.'}
                  </td>
                </tr>
              ) : (
                filtered.map((lt: LinkType) => (
                  <tr key={lt.id}>
                    <td>
                      <span className="role-name">{lt.outwardName}</span>
                    </td>
                    <td>{lt.inwardName}</td>
                    <td>
                      <code style={{ fontSize: 12, color: 'var(--sa-n600)' }}>{lt.linkKey}</code>
                    </td>
                    <td>
                      {lt.isSystem ? (
                        <span className="role-type-badge role-type-default">System</span>
                      ) : (
                        <span className="role-type-badge role-type-custom">Custom</span>
                      )}
                    </td>
                    <td>
                      {lt.isActive ? (
                        <span className="role-default-badge" style={{ background: '#e3fcef', color: '#006644' }}>Active</span>
                      ) : (
                        <span className="role-default-badge" style={{ background: '#ffebe6', color: '#bf2600' }}>Inactive</span>
                      )}
                    </td>
                    <td>
                      <div className="action-buttons">
                        <button
                          className="admin-btn-secondary admin-btn-sm"
                          onClick={() => openEditModal(lt)}
                        >
                          Edit
                        </button>
                        {!lt.isSystem && (
                          <button
                            className="admin-btn-danger admin-btn-sm"
                            onClick={() => openDeleteConfirm(lt)}
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

      {/* Delete Confirmation Modal */}
      {deleteConfirm && (
        <div className="admin-modal-overlay" onClick={() => setDeleteConfirm(null)}>
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h2 className="admin-modal-title">Delete Link Type</h2>
              <button className="admin-modal-close" onClick={() => setDeleteConfirm(null)}>
                &times;
              </button>
            </div>
            <div className="admin-modal-body">
              <p>
                Are you sure you want to delete the link type{' '}
                <strong>{deleteConfirm.outwardName}</strong> /{' '}
                <strong>{deleteConfirm.inwardName}</strong>?
              </p>
              <p style={{ color: 'var(--sa-n500)', fontSize: 13, marginTop: 8 }}>
                This will deactivate the link type. Existing issue links of this type will be
                preserved but no new links of this type can be created.
              </p>
            </div>
            <div className="admin-modal-footer">
              <button className="admin-btn-secondary" onClick={() => setDeleteConfirm(null)}>
                Cancel
              </button>
              <button
                className="admin-btn-danger"
                onClick={() => handleDelete(deleteConfirm)}
                disabled={deleteLinkType.isPending}
              >
                {deleteLinkType.isPending ? 'Deleting...' : 'Delete'}
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
              <h2 className="admin-modal-title">
                {modalMode === 'create' ? 'Add Link Type' : 'Edit Link Type'}
              </h2>
              <button className="admin-modal-close" onClick={closeModal}>
                &times;
              </button>
            </div>
            <div className="admin-modal-body">
              <div className="admin-form-group">
                <label className="admin-form-label admin-form-label-required">Outward Name</label>
                <input
                  type="text"
                  className="admin-form-input"
                  value={formData.outwardName}
                  onChange={(e) => handleOutwardNameChange(e.target.value)}
                  placeholder='e.g., blocks, clones, duplicates'
                />
                <span style={{ fontSize: 12, color: 'var(--sa-n500)', marginTop: 4, display: 'block' }}>
                  The name shown on the source issue (e.g. "blocks")
                </span>
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label admin-form-label-required">Inward Name</label>
                <input
                  type="text"
                  className="admin-form-input"
                  value={formData.inwardName}
                  onChange={(e) => setFormData({ ...formData, inwardName: e.target.value })}
                  placeholder='e.g., is blocked by, is cloned by, is duplicated by'
                />
                <span style={{ fontSize: 12, color: 'var(--sa-n500)', marginTop: 4, display: 'block' }}>
                  The name shown on the target issue (e.g. "is blocked by")
                </span>
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label admin-form-label-required">Link Key</label>
                <input
                  type="text"
                  className="admin-form-input"
                  value={formData.linkKey}
                  onChange={(e) =>
                    setFormData({ ...formData, linkKey: e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '') })
                  }
                  placeholder="Auto-generated from outward name"
                  readOnly={modalMode === 'edit'}
                  style={modalMode === 'edit' ? { background: 'var(--sa-n20, #f4f5f7)', cursor: 'default' } : undefined}
                />
                {modalMode === 'edit' && (
                  <span style={{ fontSize: 12, color: 'var(--sa-n500)', marginTop: 4, display: 'block' }}>
                    Key cannot be changed after creation
                  </span>
                )}
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label">Description</label>
                <textarea
                  className="admin-form-textarea"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Describe this link type..."
                  rows={3}
                />
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label">Sort Order</label>
                <input
                  type="number"
                  className="admin-form-input"
                  value={formData.sortOrder}
                  onChange={(e) =>
                    setFormData({ ...formData, sortOrder: parseInt(e.target.value, 10) || 0 })
                  }
                  min={0}
                />
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
                    ? createLinkType.isPending
                    : updateLinkType.isPending
                }
              >
                {modalMode === 'create'
                  ? createLinkType.isPending
                    ? 'Creating...'
                    : 'Create'
                  : updateLinkType.isPending
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
