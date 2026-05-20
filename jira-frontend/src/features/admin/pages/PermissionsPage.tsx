import React, { useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import {
  usePermissionSchemes,
  useNotificationSchemes,
  useSecuritySchemes,
  useCreatePermissionScheme,
  useUpdatePermissionScheme,
  useDeletePermissionScheme,
  useCopyPermissionScheme,
  useCreateNotificationScheme,
  useUpdateNotificationScheme,
  useDeleteNotificationScheme,
  useCopyNotificationScheme,
  useCreateSecurityScheme,
  useUpdateSecurityScheme,
  useDeleteSecurityScheme,
  useCopySecurityScheme,
  PermissionScheme,
  NotificationScheme,
  SecurityScheme,
} from '../hooks/useAdminApi';
import './PermissionsPage.css';

type TabType = 'permission-schemes' | 'notification-schemes' | 'issue-security';

interface SchemeFormData {
  name: string;
  description: string;
}

export default function PermissionsPage() {
  const [activeTab, setActiveTab] = useState<TabType>('permission-schemes');
  const [showModal, setShowModal] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  const [selectedScheme, setSelectedScheme] = useState<any>(null);
  const [formData, setFormData] = useState<SchemeFormData>({ name: '', description: '' });
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Permission Schemes
  const { data: permissionSchemes, isLoading: loadingPermissions } = usePermissionSchemes();
  const createPermissionScheme = useCreatePermissionScheme();
  const updatePermissionScheme = useUpdatePermissionScheme();
  const deletePermissionScheme = useDeletePermissionScheme();
  const copyPermissionScheme = useCopyPermissionScheme();

  // Notification Schemes
  const { data: notificationSchemes, isLoading: loadingNotifications } = useNotificationSchemes();
  const createNotificationScheme = useCreateNotificationScheme();
  const updateNotificationScheme = useUpdateNotificationScheme();
  const deleteNotificationScheme = useDeleteNotificationScheme();
  const copyNotificationScheme = useCopyNotificationScheme();

  // Security Schemes
  const { data: securitySchemes, isLoading: loadingSecurity } = useSecuritySchemes();
  const createSecurityScheme = useCreateSecurityScheme();
  const updateSecurityScheme = useUpdateSecurityScheme();
  const deleteSecurityScheme = useDeleteSecurityScheme();
  const copySecurityScheme = useCopySecurityScheme();

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
    setSelectedScheme(null);
    setFormData({ name: '', description: '' });
    setShowModal(true);
  };

  const openEditModal = (scheme: any) => {
    setModalMode('edit');
    setSelectedScheme(scheme);
    setFormData({ name: scheme.name, description: scheme.description });
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setSelectedScheme(null);
    setFormData({ name: '', description: '' });
    setError(null);
  };

  const handleCreate = async () => {
    if (!formData.name.trim()) {
      setError('Name is required');
      return;
    }
    try {
      switch (activeTab) {
        case 'permission-schemes':
          await createPermissionScheme.mutateAsync(formData);
          showMessage('Permission scheme created successfully');
          break;
        case 'notification-schemes':
          await createNotificationScheme.mutateAsync(formData);
          showMessage('Notification scheme created successfully');
          break;
        case 'issue-security':
          await createSecurityScheme.mutateAsync(formData);
          showMessage('Security scheme created successfully');
          break;
      }
      closeModal();
    } catch (err: any) {
      showMessage(err?.message || 'Failed to create scheme', true);
    }
  };

  const handleUpdate = async () => {
    if (!formData.name.trim()) {
      setError('Name is required');
      return;
    }
    try {
      switch (activeTab) {
        case 'permission-schemes':
          await updatePermissionScheme.mutateAsync({ id: selectedScheme.id, data: formData });
          showMessage('Permission scheme updated successfully');
          break;
        case 'notification-schemes':
          await updateNotificationScheme.mutateAsync({ id: selectedScheme.id, data: formData });
          showMessage('Notification scheme updated successfully');
          break;
        case 'issue-security':
          await updateSecurityScheme.mutateAsync({ id: selectedScheme.id, data: formData });
          showMessage('Security scheme updated successfully');
          break;
      }
      closeModal();
    } catch (err: any) {
      showMessage(err?.message || 'Failed to update scheme', true);
    }
  };

  const handleCopy = async (scheme: any) => {
    try {
      switch (activeTab) {
        case 'permission-schemes':
          await copyPermissionScheme.mutateAsync(scheme.id);
          showMessage('Permission scheme copied successfully');
          break;
        case 'notification-schemes':
          await copyNotificationScheme.mutateAsync(scheme.id);
          showMessage('Notification scheme copied successfully');
          break;
        case 'issue-security':
          await copySecurityScheme.mutateAsync(scheme.id);
          showMessage('Security scheme copied successfully');
          break;
      }
    } catch (err: any) {
      showMessage(err?.message || 'Failed to copy scheme', true);
    }
  };

  const handleDelete = async (scheme: any) => {
    if (!confirm(`Are you sure you want to delete "${scheme.name}"?`)) return;
    try {
      switch (activeTab) {
        case 'permission-schemes':
          await deletePermissionScheme.mutateAsync(scheme.id);
          showMessage('Permission scheme deleted successfully');
          break;
        case 'notification-schemes':
          await deleteNotificationScheme.mutateAsync(scheme.id);
          showMessage('Notification scheme deleted successfully');
          break;
        case 'issue-security':
          await deleteSecurityScheme.mutateAsync(scheme.id);
          showMessage('Security scheme deleted successfully');
          break;
      }
    } catch (err: any) {
      showMessage(err?.message || 'Failed to delete scheme', true);
    }
  };

  const renderPermissionSchemes = () => (
    <>
      {error && <div className="admin-alert admin-alert-error">{error}</div>}
      {success && <div className="admin-alert admin-alert-success">{success}</div>}

      <div className="admin-stats-grid">
        <div className="admin-stat-card">
          <div className="admin-stat-label">Total Schemes</div>
          <div className="admin-stat-value">{permissionSchemes?.length || 0}</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Default Scheme</div>
          <div className="admin-stat-value">{permissionSchemes?.find(s => s.isDefault)?.name || 'None'}</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Projects Using</div>
          <div className="admin-stat-value">{permissionSchemes?.reduce((sum, s) => sum + s.projectCount, 0) || 0}</div>
        </div>
      </div>

      <div className="admin-toolbar">
        <div className="admin-toolbar-left">
          <input
            type="text"
            placeholder="Search permission schemes..."
            className="admin-search-input-toolbar"
          />
        </div>
        <div className="admin-toolbar-right">
          <button className="admin-btn-primary" onClick={openCreateModal}>Add Permission Scheme</button>
        </div>
      </div>

      <div className="admin-table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Scheme Name</th>
              <th>Description</th>
              <th>Projects</th>
              <th>Permissions</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loadingPermissions ? (
              <tr><td colSpan={5} className="loading-cell">Loading...</td></tr>
            ) : permissionSchemes?.length === 0 ? (
              <tr><td colSpan={5} className="empty-cell">No permission schemes found</td></tr>
            ) : (
              permissionSchemes?.map((scheme) => (
                <tr key={scheme.id}>
                  <td>
                    <div className="scheme-cell">
                      <span className="scheme-icon">P</span>
                      <div>
                        <span className="scheme-name">{scheme.name}</span>
                        {scheme.isDefault && <span className="scheme-default-badge">Default</span>}
                      </div>
                    </div>
                  </td>
                  <td className="description-cell">{scheme.description}</td>
                  <td>{scheme.projectCount} projects</td>
                  <td>{scheme.permissionCount} permissions</td>
                  <td>
                    <div className="action-buttons">
                      <button className="admin-btn-secondary" onClick={() => openEditModal(scheme)}>Edit</button>
                      <button className="admin-btn-secondary" onClick={() => handleCopy(scheme)}>Copy</button>
                      <button className="admin-btn-danger" onClick={() => handleDelete(scheme)}>Delete</button>
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

  const renderNotificationSchemes = () => (
    <>
      {error && <div className="admin-alert admin-alert-error">{error}</div>}
      {success && <div className="admin-alert admin-alert-success">{success}</div>}

      <div className="admin-stats-grid">
        <div className="admin-stat-card">
          <div className="admin-stat-label">Total Schemes</div>
          <div className="admin-stat-value">{notificationSchemes?.length || 0}</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Default Scheme</div>
          <div className="admin-stat-value">{notificationSchemes?.find(s => s.isDefault)?.name || 'None'}</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Projects Using</div>
          <div className="admin-stat-value">{notificationSchemes?.reduce((sum, s) => sum + s.projectCount, 0) || 0}</div>
        </div>
      </div>

      <div className="admin-toolbar">
        <div className="admin-toolbar-left">
          <input
            type="text"
            placeholder="Search notification schemes..."
            className="admin-search-input-toolbar"
          />
        </div>
        <div className="admin-toolbar-right">
          <button className="admin-btn-primary" onClick={openCreateModal}>Add Notification Scheme</button>
        </div>
      </div>

      <div className="admin-table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Scheme Name</th>
              <th>Description</th>
              <th>Projects</th>
              <th>Events</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loadingNotifications ? (
              <tr><td colSpan={5} className="loading-cell">Loading...</td></tr>
            ) : notificationSchemes?.length === 0 ? (
              <tr><td colSpan={5} className="empty-cell">No notification schemes found</td></tr>
            ) : (
              notificationSchemes?.map((scheme) => (
                <tr key={scheme.id}>
                  <td>
                    <div className="scheme-cell">
                      <span className="scheme-icon notification">N</span>
                      <div>
                        <span className="scheme-name">{scheme.name}</span>
                        {scheme.isDefault && <span className="scheme-default-badge">Default</span>}
                      </div>
                    </div>
                  </td>
                  <td className="description-cell">{scheme.description}</td>
                  <td>{scheme.projectCount} projects</td>
                  <td>{scheme.eventCount} events</td>
                  <td>
                    <div className="action-buttons">
                      <button className="admin-btn-secondary" onClick={() => openEditModal(scheme)}>Edit</button>
                      <button className="admin-btn-secondary" onClick={() => handleCopy(scheme)}>Copy</button>
                      <button className="admin-btn-danger" onClick={() => handleDelete(scheme)}>Delete</button>
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

  const renderIssueSecurity = () => (
    <>
      {error && <div className="admin-alert admin-alert-error">{error}</div>}
      {success && <div className="admin-alert admin-alert-success">{success}</div>}

      <div className="admin-stats-grid">
        <div className="admin-stat-card">
          <div className="admin-stat-label">Total Schemes</div>
          <div className="admin-stat-value">{securitySchemes?.length || 0}</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Projects Using</div>
          <div className="admin-stat-value">{securitySchemes?.reduce((sum, s) => sum + s.projectCount, 0) || 0}</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Security Levels</div>
          <div className="admin-stat-value">{securitySchemes?.reduce((sum, s) => sum + s.securityLevelCount, 0) || 0}</div>
        </div>
      </div>

      <div className="admin-toolbar">
        <div className="admin-toolbar-left">
          <input
            type="text"
            placeholder="Search security schemes..."
            className="admin-search-input-toolbar"
          />
        </div>
        <div className="admin-toolbar-right">
          <button className="admin-btn-primary" onClick={openCreateModal}>Add Security Scheme</button>
        </div>
      </div>

      <div className="admin-table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Scheme Name</th>
              <th>Description</th>
              <th>Projects</th>
              <th>Security Levels</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loadingSecurity ? (
              <tr><td colSpan={5} className="loading-cell">Loading...</td></tr>
            ) : securitySchemes?.length === 0 ? (
              <tr><td colSpan={5} className="empty-cell">No security schemes found</td></tr>
            ) : (
              securitySchemes?.map((scheme) => (
                <tr key={scheme.id}>
                  <td>
                    <div className="scheme-cell">
                      <span className="scheme-icon security">S</span>
                      <div>
                        <span className="scheme-name">{scheme.name}</span>
                        {scheme.isDefault && <span className="scheme-default-badge">Default</span>}
                      </div>
                    </div>
                  </td>
                  <td className="description-cell">{scheme.description}</td>
                  <td>{scheme.projectCount} projects</td>
                  <td>{scheme.securityLevelCount} levels</td>
                  <td>
                    <div className="action-buttons">
                      <button className="admin-btn-secondary" onClick={() => openEditModal(scheme)}>Edit</button>
                      <button className="admin-btn-secondary" onClick={() => handleCopy(scheme)}>Copy</button>
                      <button className="admin-btn-danger" onClick={() => handleDelete(scheme)}>Delete</button>
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

  const getTabTitle = () => {
    switch (activeTab) {
      case 'permission-schemes': return 'Permission Schemes';
      case 'notification-schemes': return 'Notification Schemes';
      case 'issue-security': return 'Issue Security';
    }
  };

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">Permissions</h1>
          <p className="admin-page-description">
            Manage permission schemes, notification schemes, and issue security.
          </p>
        </div>

        <div className="permissions-tabs">
          <button
            className={`permissions-tab ${activeTab === 'permission-schemes' ? 'active' : ''}`}
            onClick={() => setActiveTab('permission-schemes')}
          >
            Permission Schemes
          </button>
          <button
            className={`permissions-tab ${activeTab === 'notification-schemes' ? 'active' : ''}`}
            onClick={() => setActiveTab('notification-schemes')}
          >
            Notification Schemes
          </button>
          <button
            className={`permissions-tab ${activeTab === 'issue-security' ? 'active' : ''}`}
            onClick={() => setActiveTab('issue-security')}
          >
            Issue Security
          </button>
        </div>

        <div className="permissions-content">
          {activeTab === 'permission-schemes' && renderPermissionSchemes()}
          {activeTab === 'notification-schemes' && renderNotificationSchemes()}
          {activeTab === 'issue-security' && renderIssueSecurity()}
        </div>

        {/* Create/Edit Modal */}
        {showModal && (
          <div className="admin-modal-overlay" onClick={closeModal}>
            <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
              <div className="admin-modal-header">
                <h2 className="admin-modal-title">
                  {modalMode === 'create' ? `Add ${getTabTitle().replace('s', '')}` : `Edit ${getTabTitle().replace('s', '')}`}
                </h2>
                <button className="admin-modal-close" onClick={closeModal}>×</button>
              </div>
              <div className="admin-modal-body">
                <div className="admin-form-group">
                  <label className="admin-form-label admin-form-label-required">Name</label>
                  <input
                    type="text"
                    className="admin-form-input"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    placeholder="Enter scheme name"
                  />
                  {error && formData.name.trim() === '' && (
                    <span className="form-error">Name is required</span>
                  )}
                </div>
                <div className="admin-form-group">
                  <label className="admin-form-label">Description</label>
                  <textarea
                    className="admin-form-textarea"
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="Enter scheme description"
                    rows={3}
                  />
                </div>
              </div>
              <div className="admin-modal-footer">
                <button className="admin-btn-secondary" onClick={closeModal}>Cancel</button>
                <button
                  className="admin-btn-primary"
                  onClick={modalMode === 'create' ? handleCreate : handleUpdate}
                  disabled={
                    modalMode === 'create'
                      ? createPermissionScheme.isPending || createNotificationScheme.isPending || createSecurityScheme.isPending
                      : updatePermissionScheme.isPending || updateNotificationScheme.isPending || updateSecurityScheme.isPending
                  }
                >
                  {modalMode === 'create' ? 'Create' : 'Update'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  );
}