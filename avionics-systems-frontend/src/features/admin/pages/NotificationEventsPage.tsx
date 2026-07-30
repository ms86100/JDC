import React, { useState, useMemo } from 'react';
import {
  useNotificationEvents,
  useCreateNotificationEvent,
  useUpdateNotificationEvent,
  useDeleteNotificationEvent,
  NotificationEvent,
} from '../hooks/useAdminApi';
import AdminLayout from '../components/AdminLayout';
import './AdminIssueConfig.css';

interface NotificationEventFormData {
  eventKey: string;
  displayName: string;
  description: string;
  category: string;
  isActive: boolean;
}

const EMPTY_FORM: NotificationEventFormData = {
  eventKey: '',
  displayName: '',
  description: '',
  category: 'Issue',
  isActive: true,
};

const CATEGORIES = ['Issue', 'Comment', 'Status', 'Sprint', 'Project'];

function toEventKey(name: string): string {
  return name
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]+/g, '_')
    .replace(/^_|_$/g, '');
}

const CATEGORY_COLORS: Record<string, { bg: string; color: string }> = {
  Issue:   { bg: '#deebff', color: '#0747a6' },
  Comment: { bg: '#e3fcef', color: '#006644' },
  Status:  { bg: '#fff0b3', color: '#172b4d' },
  Sprint:  { bg: '#eae6ff', color: '#403294' },
  Project: { bg: '#ffebe6', color: '#bf2600' },
};

export default function NotificationEventsPage() {
  const [search, setSearch] = useState('');
  const [activeCategory, setActiveCategory] = useState<string>('All');
  const [showModal, setShowModal] = useState(false);
  const [editingEvent, setEditingEvent] = useState<NotificationEvent | null>(null);
  const [formData, setFormData] = useState<NotificationEventFormData>(EMPTY_FORM);
  const [deleteConfirm, setDeleteConfirm] = useState<NotificationEvent | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const { data: events, isLoading, isError, refetch } = useNotificationEvents();
  const createMutation = useCreateNotificationEvent();
  const updateMutation = useUpdateNotificationEvent();
  const deleteMutation = useDeleteNotificationEvent();

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
    }, 4000);
  };

  const filteredEvents = useMemo(() => {
    const list = events || [];
    return list.filter((evt) => {
      const matchesCategory =
        activeCategory === 'All' || evt.category === activeCategory;
      const matchesSearch =
        !search ||
        evt.eventKey.toLowerCase().includes(search.toLowerCase()) ||
        evt.displayName.toLowerCase().includes(search.toLowerCase()) ||
        evt.description?.toLowerCase().includes(search.toLowerCase());
      return matchesCategory && matchesSearch;
    });
  }, [events, activeCategory, search]);

  const totalCount = events?.length ?? 0;
  const activeCount = events?.filter((e) => e.isActive).length ?? 0;
  const systemCount = events?.filter((e) => e.isSystem).length ?? 0;
  const categoryCounts = useMemo(() => {
    const counts: Record<string, number> = {};
    (events || []).forEach((e) => {
      counts[e.category] = (counts[e.category] || 0) + 1;
    });
    return counts;
  }, [events]);

  // --- Modal handlers ---

  const openCreateModal = () => {
    setEditingEvent(null);
    setFormData(EMPTY_FORM);
    setError(null);
    setShowModal(true);
  };

  const openEditModal = (evt: NotificationEvent) => {
    setEditingEvent(evt);
    setFormData({
      eventKey: evt.eventKey,
      displayName: evt.displayName,
      description: evt.description || '',
      category: evt.category,
      isActive: evt.isActive,
    });
    setError(null);
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setEditingEvent(null);
    setFormData(EMPTY_FORM);
    setError(null);
  };

  const handleDisplayNameChange = (value: string) => {
    const updates: Partial<NotificationEventFormData> = { displayName: value };
    if (!editingEvent) {
      updates.eventKey = toEventKey(value);
    }
    setFormData((prev) => ({ ...prev, ...updates }));
  };

  const handleSave = async () => {
    if (!formData.eventKey.trim() || !formData.displayName.trim()) {
      setError('Event Key and Display Name are required');
      return;
    }

    const payload: Partial<NotificationEvent> = {
      eventKey: formData.eventKey.trim(),
      displayName: formData.displayName.trim(),
      description: formData.description.trim(),
      category: formData.category,
      isActive: formData.isActive,
    };

    if (editingEvent) {
      try {
        await updateMutation.mutateAsync({
          id: editingEvent.id,
          data: payload,
        });
        showMessage(`Notification event "${formData.displayName}" updated successfully.`);
        closeModal();
      } catch (err: unknown) {
        const msg =
          (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
          (err instanceof Error ? err.message : 'Failed to update notification event');
        setError(msg);
      }
    } else {
      try {
        await createMutation.mutateAsync(payload);
        showMessage(`Notification event "${formData.displayName}" created successfully.`);
        closeModal();
      } catch (err: unknown) {
        const msg =
          (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
          (err instanceof Error ? err.message : 'Failed to create notification event');
        setError(msg);
      }
    }
  };

  const handleToggleActive = async (evt: NotificationEvent) => {
    try {
      await updateMutation.mutateAsync({
        id: evt.id,
        data: { isActive: !evt.isActive },
      });
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (err instanceof Error ? err.message : 'Failed to update notification event');
      showMessage(msg, true);
    }
  };

  const openDeleteConfirm = (evt: NotificationEvent) => {
    if (evt.isSystem) {
      showMessage('System notification events cannot be deleted.', true);
      return;
    }
    setDeleteConfirm(evt);
  };

  const handleDelete = async (evt: NotificationEvent) => {
    try {
      await deleteMutation.mutateAsync(evt.id);
      showMessage(`Notification event "${evt.displayName}" deleted successfully.`);
    } catch (err: unknown) {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        (err instanceof Error ? err.message : 'Failed to delete notification event');
      showMessage(msg, true);
    }
    setDeleteConfirm(null);
  };

  const isSaving = createMutation.isPending || updateMutation.isPending;

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">Notification Events</h1>
          <p className="admin-page-description">
            Configure notification events that trigger alerts and emails across the system.
          </p>
        </div>

        {/* Stats */}
        <div className="admin-stats-grid">
          <div className="admin-stat-card">
            <div className="admin-stat-value">{totalCount}</div>
            <div className="admin-stat-label">Total Events</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-value">{activeCount}</div>
            <div className="admin-stat-label">Active</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-value">{systemCount}</div>
            <div className="admin-stat-label">System</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-value">{Object.keys(categoryCounts).length}</div>
            <div className="admin-stat-label">Categories</div>
          </div>
        </div>

        {/* Alerts */}
        {success && (
          <div className="admin-alert admin-alert-success">
            {success}
            <button type="button" className="admin-alert-dismiss" onClick={() => setSuccess(null)}>&times;</button>
          </div>
        )}
        {error && !showModal && (
          <div className="admin-alert admin-alert-error">
            {error}
            <button type="button" className="admin-alert-dismiss" onClick={() => setError(null)}>&times;</button>
          </div>
        )}

        {/* Category Filter Tabs */}
        <div className="admin-toolbar" style={{ flexDirection: 'column', gap: 12, alignItems: 'stretch' }}>
          <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
            {['All', ...CATEGORIES].map((cat) => {
              const count = cat === 'All' ? totalCount : (categoryCounts[cat] || 0);
              return (
                <button
                  key={cat}
                  type="button"
                  className={activeCategory === cat ? 'admin-btn-primary admin-btn-sm' : 'admin-btn-secondary admin-btn-sm'}
                  onClick={() => setActiveCategory(cat)}
                  style={{ minWidth: 70 }}
                >
                  {cat} ({count})
                </button>
              );
            })}
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div className="admin-toolbar-left">
              <input
                type="text"
                placeholder="Search events..."
                className="admin-search-input"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            <div className="admin-toolbar-right">
              <button className="admin-btn-primary" onClick={openCreateModal}>
                Add Event
              </button>
            </div>
          </div>
        </div>

        {/* Loading / Error */}
        {isLoading && <p style={{ textAlign: 'center', padding: 24, color: 'var(--sa-n500)' }}>Loading notification events...</p>}
        {isError && (
          <div style={{ textAlign: 'center', padding: 24 }}>
            <p style={{ color: 'var(--sa-n600)' }}>Failed to load notification events.</p>
            <button type="button" className="admin-btn-secondary" onClick={() => refetch()} style={{ marginTop: 8 }}>
              Retry
            </button>
          </div>
        )}

        {/* Table */}
        {!isLoading && !isError && (
          <div className="admin-table-container">
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Event Key</th>
                  <th>Display Name</th>
                  <th>Category</th>
                  <th>System</th>
                  <th>Active</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredEvents.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ textAlign: 'center', padding: 24, color: 'var(--sa-n600)' }}>
                      {search || activeCategory !== 'All'
                        ? 'No notification events match your filters.'
                        : 'No notification events configured yet. Click "Add Event" to create one.'}
                    </td>
                  </tr>
                ) : (
                  filteredEvents.map((evt) => {
                    const catStyle = CATEGORY_COLORS[evt.category] || { bg: '#f4f5f7', color: '#172b4d' };
                    return (
                      <tr key={evt.id}>
                        <td>
                          <code style={{ fontSize: 12, color: 'var(--sa-n600)' }}>{evt.eventKey}</code>
                        </td>
                        <td>
                          <span style={{ fontWeight: 500 }}>{evt.displayName}</span>
                          {evt.description && (
                            <div style={{ fontSize: 12, color: 'var(--sa-n500)', marginTop: 2 }}>
                              {evt.description}
                            </div>
                          )}
                        </td>
                        <td>
                          <span
                            className="role-type-badge"
                            style={{ background: catStyle.bg, color: catStyle.color }}
                          >
                            {evt.category}
                          </span>
                        </td>
                        <td>
                          {evt.isSystem ? (
                            <span className="role-type-badge role-type-default">System</span>
                          ) : (
                            <span className="role-type-badge role-type-custom">Custom</span>
                          )}
                        </td>
                        <td>
                          <button
                            type="button"
                            onClick={() => handleToggleActive(evt)}
                            disabled={updateMutation.isPending}
                            style={{
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: 6,
                              padding: '4px 10px',
                              borderRadius: 12,
                              border: 'none',
                              fontSize: 12,
                              fontWeight: 500,
                              cursor: 'pointer',
                              background: evt.isActive ? '#e3fcef' : '#ffebe6',
                              color: evt.isActive ? '#006644' : '#bf2600',
                            }}
                            title={evt.isActive ? 'Click to deactivate' : 'Click to activate'}
                          >
                            <span
                              style={{
                                width: 8,
                                height: 8,
                                borderRadius: '50%',
                                background: evt.isActive ? '#006644' : '#bf2600',
                              }}
                            />
                            {evt.isActive ? 'Active' : 'Inactive'}
                          </button>
                        </td>
                        <td>
                          <div className="action-buttons">
                            <button
                              className="admin-btn-secondary admin-btn-sm"
                              onClick={() => openEditModal(evt)}
                            >
                              Edit
                            </button>
                            {!evt.isSystem && (
                              <button
                                className="admin-btn-danger admin-btn-sm"
                                onClick={() => openDeleteConfirm(evt)}
                              >
                                Delete
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })
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
                <h3>Delete Notification Event</h3>
                <button onClick={() => setDeleteConfirm(null)}>&times;</button>
              </div>
              <div className="admin-modal-body">
                <p>
                  Are you sure you want to delete the notification event{' '}
                  <strong>{deleteConfirm.displayName}</strong>?
                </p>
                <p style={{ color: 'var(--sa-n500)', fontSize: 13, marginTop: 8 }}>
                  This action cannot be undone. Any notification schemes referencing this event
                  will need to be updated.
                </p>
              </div>
              <div className="admin-modal-footer">
                <button className="admin-btn-secondary" onClick={() => setDeleteConfirm(null)}>
                  Cancel
                </button>
                <button
                  className="admin-btn-danger"
                  onClick={() => handleDelete(deleteConfirm)}
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
                <h3>{editingEvent ? 'Edit Notification Event' : 'Add Notification Event'}</h3>
                <button onClick={closeModal}>&times;</button>
              </div>
              <div className="admin-modal-body">
                {error && (
                  <div className="admin-alert admin-alert-error" style={{ marginBottom: 16 }}>
                    {error}
                  </div>
                )}

                <div className="admin-form-group">
                  <label className="admin-form-label">
                    Display Name <span style={{ color: '#d73a49' }}>*</span>
                  </label>
                  <input
                    className="admin-form-input"
                    type="text"
                    value={formData.displayName}
                    onChange={(e) => handleDisplayNameChange(e.target.value)}
                    placeholder="e.g., Issue Created, Comment Added"
                  />
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">
                    Event Key <span style={{ color: '#d73a49' }}>*</span>
                  </label>
                  <input
                    className="admin-form-input"
                    type="text"
                    value={formData.eventKey}
                    onChange={(e) =>
                      setFormData((prev) => ({
                        ...prev,
                        eventKey: e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, ''),
                      }))
                    }
                    placeholder="e.g., ISSUE_CREATED"
                    readOnly={!!editingEvent}
                    style={editingEvent ? { background: 'var(--sa-n20, #f4f5f7)', cursor: 'default' } : undefined}
                  />
                  {editingEvent ? (
                    <span style={{ fontSize: 12, color: 'var(--sa-n500)', marginTop: 4, display: 'block' }}>
                      Event key cannot be changed after creation
                    </span>
                  ) : (
                    <span style={{ fontSize: 12, color: 'var(--sa-n500)', marginTop: 4, display: 'block' }}>
                      Uppercase letters, numbers, and underscores only. Auto-generated from display name.
                    </span>
                  )}
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">Description</label>
                  <textarea
                    className="admin-form-input"
                    value={formData.description}
                    onChange={(e) => setFormData((prev) => ({ ...prev, description: e.target.value }))}
                    placeholder="Describe when this notification event fires"
                    rows={3}
                    style={{ resize: 'vertical' }}
                  />
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">
                    Category <span style={{ color: '#d73a49' }}>*</span>
                  </label>
                  <select
                    className="admin-form-input"
                    value={formData.category}
                    onChange={(e) => setFormData((prev) => ({ ...prev, category: e.target.value }))}
                  >
                    {CATEGORIES.map((cat) => (
                      <option key={cat} value={cat}>
                        {cat}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label-checkbox">
                    <input
                      type="checkbox"
                      checked={formData.isActive}
                      onChange={(e) => setFormData((prev) => ({ ...prev, isActive: e.target.checked }))}
                    />
                    <span>Active</span>
                  </label>
                  <span style={{ fontSize: 12, color: 'var(--sa-n500)' }}>
                    Inactive events will not trigger notifications
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
                  disabled={!formData.eventKey.trim() || !formData.displayName.trim() || isSaving}
                >
                  {isSaving
                    ? editingEvent ? 'Saving...' : 'Adding...'
                    : editingEvent ? 'Save Changes' : 'Add Event'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  );
}
