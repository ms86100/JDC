import React, { useState, useMemo } from 'react';
import {
  useQuickFilterPresets,
  useCreateQuickFilterPreset,
  useUpdateQuickFilterPreset,
  useDeleteQuickFilterPreset,
  QuickFilterPreset,
} from '../hooks/useAdminApi';
import AdminLayout from '../components/AdminLayout';
import { Search, Plus, Edit2, Trash2, X, Info } from 'lucide-react';
import '../styles/admin-shared.css';
import './AdminIssueConfig.css';

interface FilterFormData {
  filterName: string;
  jqlQuery: string;
  icon: string;
  sortOrder: number;
  isActive: boolean;
}

const EMPTY_FORM: FilterFormData = {
  filterName: '',
  jqlQuery: '',
  icon: '',
  sortOrder: 0,
  isActive: true,
};

export default function QuickFilterPresetsPage() {
  const [search, setSearch] = useState('');
  const { data: filters, isLoading, isError } = useQuickFilterPresets();
  const createMutation = useCreateQuickFilterPreset();
  const updateMutation = useUpdateQuickFilterPreset();
  const deleteMutation = useDeleteQuickFilterPreset();

  const [showModal, setShowModal] = useState(false);
  const [editingFilter, setEditingFilter] = useState<QuickFilterPreset | null>(null);
  const [formData, setFormData] = useState<FilterFormData>(EMPTY_FORM);
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  const filteredList = useMemo(() => {
    const list = filters || [];
    const filtered = list.filter(f =>
      f.filterName.toLowerCase().includes(search.toLowerCase()) ||
      f.jqlQuery.toLowerCase().includes(search.toLowerCase())
    );
    return [...filtered].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
  }, [filters, search]);

  const totalCount = filters?.length ?? 0;
  const activeCount = filters?.filter(f => f.isActive).length ?? 0;
  const systemCount = filters?.filter(f => f.isSystem).length ?? 0;

  // --- Modal handlers ---

  const openCreateModal = () => {
    setEditingFilter(null);
    setFormData({
      ...EMPTY_FORM,
      sortOrder: totalCount + 1,
    });
    setActionError(null);
    setShowModal(true);
  };

  const openEditModal = (filter: QuickFilterPreset) => {
    setEditingFilter(filter);
    setFormData({
      filterName: filter.filterName,
      jqlQuery: filter.jqlQuery,
      icon: filter.icon || '',
      sortOrder: filter.sortOrder ?? 0,
      isActive: filter.isActive ?? true,
    });
    setActionError(null);
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setEditingFilter(null);
    setFormData(EMPTY_FORM);
    setActionError(null);
  };

  const handleSave = () => {
    if (!formData.filterName.trim() || !formData.jqlQuery.trim()) return;

    const payload: Partial<QuickFilterPreset> = {
      filterName: formData.filterName.trim(),
      jqlQuery: formData.jqlQuery.trim(),
      icon: formData.icon.trim(),
      sortOrder: formData.sortOrder,
      isActive: formData.isActive,
    };

    if (editingFilter) {
      updateMutation.mutate(
        { id: editingFilter.id, data: payload },
        {
          onSuccess: () => {
            setActionSuccess(`Quick filter "${formData.filterName}" updated successfully.`);
            closeModal();
            setTimeout(() => setActionSuccess(null), 4000);
          },
          onError: (err: unknown) => {
            const msg =
              (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
              (err instanceof Error ? err.message : 'Failed to update quick filter');
            setActionError(msg);
          },
        }
      );
    } else {
      createMutation.mutate(payload, {
        onSuccess: () => {
          setActionSuccess(`Quick filter "${formData.filterName}" created successfully.`);
          closeModal();
          setTimeout(() => setActionSuccess(null), 4000);
        },
        onError: (err: unknown) => {
          const msg =
            (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
            (err instanceof Error ? err.message : 'Failed to create quick filter');
          setActionError(msg);
        },
      });
    }
  };

  const handleDelete = (filter: QuickFilterPreset) => {
    setActionError(null);
    deleteMutation.mutate(filter.id, {
      onSuccess: () => {
        setDeleteConfirm(null);
        setActionSuccess(`Quick filter "${filter.filterName}" deleted successfully.`);
        setTimeout(() => setActionSuccess(null), 4000);
      },
      onError: (err: unknown) => {
        const msg =
          (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
          (err instanceof Error ? err.message : 'Failed to delete quick filter');
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
          <h1 className="admin-page-title">Quick Filter Presets</h1>
          <p className="admin-page-description">
            Manage system-level saved filters for agile boards. Quick filters let users rapidly
            narrow board views using predefined JQL queries.
          </p>
        </div>

        {/* Stats */}
        <div className="admin-stats-grid">
          <div className="admin-stat-card">
            <div className="admin-stat-value">{totalCount}</div>
            <div className="admin-stat-label">Total Filters</div>
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
              placeholder="Search filters..."
              className="admin-search-input"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="admin-toolbar-right">
            <button className="admin-btn-primary" onClick={openCreateModal}>
              <Plus size={16} style={{ marginRight: 4, verticalAlign: 'middle' }} />
              Add Quick Filter
            </button>
          </div>
        </div>

        {/* Table */}
        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Icon</th>
                <th>Filter Name</th>
                <th>JQL Query</th>
                <th>Sort Order</th>
                <th>Type</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <>
                  {[...Array(5)].map((_, i) => (
                    <tr key={i}>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 30, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: '60%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: '80%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 40, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 22, width: 60, borderRadius: 12 }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 22, width: 60, borderRadius: 12 }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 80, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    </tr>
                  ))}
                </>
              ) : isError ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '24px', color: 'var(--sa-n600)' }}>
                    Failed to load quick filters. Please check if the server is running.
                  </td>
                </tr>
              ) : filteredList.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '24px', color: 'var(--sa-n600)' }}>
                    {search ? 'No quick filters match your search.' : 'No quick filters configured yet. Click "Add Quick Filter" to create one.'}
                  </td>
                </tr>
              ) : (
                filteredList.map((filter) => (
                  <tr key={filter.id}>
                    <td>
                      <span style={{ fontSize: 18 }}>{filter.icon || '-'}</span>
                    </td>
                    <td>
                      <strong>{filter.filterName}</strong>
                    </td>
                    <td>
                      <code style={{
                        fontSize: 12,
                        fontFamily: 'monospace',
                        backgroundColor: 'var(--sa-n100, #f4f5f7)',
                        padding: '2px 6px',
                        borderRadius: 4,
                        wordBreak: 'break-all',
                      }}>
                        {filter.jqlQuery}
                      </code>
                    </td>
                    <td>{filter.sortOrder ?? '-'}</td>
                    <td>
                      {filter.isSystem ? (
                        <span className="admin-status admin-status-active">System</span>
                      ) : (
                        <span className="admin-status admin-status-inactive">Custom</span>
                      )}
                    </td>
                    <td>
                      {filter.isActive ? (
                        <span className="admin-status admin-status-active">Active</span>
                      ) : (
                        <span className="admin-status admin-status-inactive">Inactive</span>
                      )}
                    </td>
                    <td>
                      <div className="action-buttons">
                        <button className="admin-btn-secondary admin-btn-sm" onClick={() => openEditModal(filter)}>
                          <Edit2 size={14} style={{ marginRight: 4, verticalAlign: 'middle' }} />
                          Edit
                        </button>
                        {filter.isSystem ? (
                          <button
                            className="admin-btn-secondary admin-btn-sm"
                            title="System filters cannot be deleted, only deactivated"
                            disabled
                            style={{ opacity: 0.5, cursor: 'not-allowed' }}
                          >
                            <Trash2 size={14} style={{ marginRight: 4, verticalAlign: 'middle' }} />
                            Delete
                          </button>
                        ) : (
                          <button className="admin-btn-danger admin-btn-sm" onClick={() => setDeleteConfirm(filter.id)}>
                            <Trash2 size={14} style={{ marginRight: 4, verticalAlign: 'middle' }} />
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
                <h3>Delete Quick Filter</h3>
                <button onClick={() => setDeleteConfirm(null)}>&times;</button>
              </div>
              <div className="admin-modal-body">
                <p>
                  Are you sure you want to delete the quick filter{' '}
                  <strong>{filters?.find(f => f.id === deleteConfirm)?.filterName}</strong>?
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
                    const target = filters?.find(f => f.id === deleteConfirm);
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
                <h3>{editingFilter ? 'Edit Quick Filter' : 'Add Quick Filter'}</h3>
                <button onClick={closeModal}>&times;</button>
              </div>
              <div className="admin-modal-body">
                {actionError && (
                  <div className="admin-alert admin-alert-error" style={{ marginBottom: 16 }}>
                    {actionError}
                  </div>
                )}

                <div className="admin-form-group">
                  <label className="admin-form-label">Filter Name <span style={{ color: '#d73a49' }}>*</span></label>
                  <input
                    className="admin-form-input"
                    type="text"
                    value={formData.filterName}
                    onChange={(e) => setFormData(prev => ({ ...prev, filterName: e.target.value }))}
                    placeholder="e.g., Assigned to Me, High Priority, Blocked"
                  />
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">JQL Query <span style={{ color: '#d73a49' }}>*</span></label>
                  <textarea
                    className="admin-form-input"
                    value={formData.jqlQuery}
                    onChange={(e) => setFormData(prev => ({ ...prev, jqlQuery: e.target.value }))}
                    placeholder='e.g., assignee = currentUser() AND resolution = Unresolved'
                    rows={4}
                    style={{
                      resize: 'vertical',
                      fontFamily: 'monospace',
                      fontSize: 13,
                    }}
                  />
                  {/* JQL help box */}
                  <div style={{
                    marginTop: 8,
                    padding: '10px 12px',
                    background: 'var(--sa-n50, #fafbfc)',
                    border: '1px solid var(--sa-n200, #dfe1e6)',
                    borderRadius: 6,
                    fontSize: 12,
                    color: 'var(--sa-n600, #6b778c)',
                    display: 'flex',
                    gap: 8,
                    alignItems: 'flex-start',
                  }}>
                    <Info size={16} style={{ flexShrink: 0, marginTop: 1, color: 'var(--sa-brand-500, #0065FF)' }} />
                    <div>
                      <strong style={{ display: 'block', marginBottom: 4, color: 'var(--sa-n700, #42526e)' }}>JQL Syntax Reference</strong>
                      <code style={{ fontSize: 11 }}>assignee = currentUser()</code> -- issues assigned to logged-in user<br />
                      <code style={{ fontSize: 11 }}>priority in (Highest, High)</code> -- high priority items<br />
                      <code style={{ fontSize: 11 }}>status = Blocked</code> -- blocked issues<br />
                      <code style={{ fontSize: 11 }}>labels in (frontend, backend)</code> -- issues with specific labels<br />
                      <code style={{ fontSize: 11 }}>updated &gt;= -7d</code> -- recently updated
                    </div>
                  </div>
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">Icon</label>
                  <input
                    className="admin-form-input"
                    type="text"
                    value={formData.icon}
                    onChange={(e) => setFormData(prev => ({ ...prev, icon: e.target.value }))}
                    placeholder="Enter an emoji or icon name (e.g., filter, star)"
                  />
                  <span style={{ fontSize: 12, color: 'var(--sa-n500)' }}>
                    Paste an emoji directly or type a descriptive icon name
                  </span>
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">Sort Order</label>
                  <input
                    className="admin-form-input"
                    type="number"
                    min={0}
                    value={formData.sortOrder}
                    onChange={(e) => setFormData(prev => ({ ...prev, sortOrder: parseInt(e.target.value, 10) || 0 }))}
                  />
                  <span style={{ fontSize: 12, color: 'var(--sa-n500)' }}>Lower numbers appear first in the filter bar</span>
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label-checkbox">
                    <input
                      type="checkbox"
                      checked={formData.isActive}
                      onChange={(e) => setFormData(prev => ({ ...prev, isActive: e.target.checked }))}
                    />
                    <span>Active</span>
                  </label>
                  <span style={{ fontSize: 12, color: 'var(--sa-n500)' }}>
                    Inactive filters are hidden from board quick filter bars
                  </span>
                </div>
              </div>
              <div className="admin-modal-footer">
                <button className="admin-btn-secondary" onClick={closeModal}>
                  Cancel
                </button>
                <button
                  className="admin-btn-primary"
                  onClick={handleSave}
                  disabled={!formData.filterName.trim() || !formData.jqlQuery.trim() || isSaving}
                >
                  {isSaving
                    ? (editingFilter ? 'Saving...' : 'Adding...')
                    : (editingFilter ? 'Save Changes' : 'Add Quick Filter')
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
