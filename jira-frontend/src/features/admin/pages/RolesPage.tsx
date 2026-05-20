import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import AdminLayout from '../components/AdminLayout';
import {
  useProjectRoles,
  useCreateProjectRole,
  useUpdateProjectRole,
  useDeleteProjectRole,
  ProjectRole,
} from '../hooks/useAdminApi';
import './RolesPage.css';

interface RoleFormData {
  name: string;
  description: string;
}

export default function RolesPage() {
  const [showModal, setShowModal] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  const [selectedRole, setSelectedRole] = useState<ProjectRole | null>(null);
  const [formData, setFormData] = useState<RoleFormData>({ name: '', description: '' });
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const { data: roles, isLoading, refetch } = useProjectRoles();
  const createRole = useCreateProjectRole();
  const updateRole = useUpdateProjectRole();
  const deleteRole = useDeleteProjectRole();

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
    setSelectedRole(null);
    setFormData({ name: '', description: '' });
    setShowModal(true);
  };

  const openEditModal = (role: ProjectRole) => {
    setModalMode('edit');
    setSelectedRole(role);
    setFormData({ name: role.name, description: role.description });
    setShowModal(true);
  };

  const closeModal = () => {
    setShowModal(false);
    setSelectedRole(null);
    setFormData({ name: '', description: '' });
    setError(null);
  };

  const handleCreate = async () => {
    if (!formData.name.trim()) {
      setError('Role name is required');
      return;
    }
    try {
      await createRole.mutateAsync(formData);
      showMessage(`Role "${formData.name}" created successfully`);
      closeModal();
    } catch (err: any) {
      showMessage(err?.message || 'Failed to create role', true);
    }
  };

  const handleUpdate = async () => {
    if (!formData.name.trim()) {
      setError('Role name is required');
      return;
    }
    try {
      await updateRole.mutateAsync({ id: selectedRole!.id, data: formData });
      showMessage(`Role "${formData.name}" updated successfully`);
      closeModal();
    } catch (err: any) {
      showMessage(err?.message || 'Failed to update role', true);
    }
  };

  const handleDelete = async (role: ProjectRole) => {
    if (!confirm(`Are you sure you want to delete the role "${role.name}"?\n\nThis will remove the role from all projects and users.`)) return;
    try {
      await deleteRole.mutateAsync(role.id);
      showMessage(`Role "${role.name}" deleted successfully`);
    } catch (err: any) {
      showMessage(err?.message || 'Failed to delete role', true);
    }
  };

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">Project Roles</h1>
          <p className="admin-page-description">
            Create and manage project roles to control access to projects and issues.
          </p>
        </div>

        {error && <div className="admin-alert admin-alert-error">{error}</div>}
        {success && <div className="admin-alert admin-alert-success">{success}</div>}

        <div className="admin-stats-grid">
          <div className="admin-stat-card">
            <div className="admin-stat-label">Total Roles</div>
            <div className="admin-stat-value">{roles?.length || 0}</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Default Roles</div>
            <div className="admin-stat-value">{roles?.filter(r => r.isDefault).length || 0}</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Total Members</div>
            <div className="admin-stat-value">{roles?.reduce((sum, r) => sum + r.memberCount, 0) || 0}</div>
          </div>
        </div>

        <div className="admin-toolbar">
          <div className="admin-toolbar-left">
            <input
              type="text"
              placeholder="Search roles..."
              className="admin-search-input-toolbar"
            />
          </div>
          <div className="admin-toolbar-right">
            <button className="admin-btn-primary" onClick={openCreateModal}>Add Role</button>
          </div>
        </div>

        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Role Name</th>
                <th>Description</th>
                <th>Members</th>
                <th>Type</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr><td colSpan={5} className="loading-cell">Loading...</td></tr>
              ) : roles?.length === 0 ? (
                <tr><td colSpan={5} className="empty-cell">No roles found. Click "Add Role" to create one.</td></tr>
              ) : (
                roles?.map((role) => (
                  <tr key={role.id}>
                    <td>
                      <div className="role-cell">
                        <span className="role-name">{role.name}</span>
                        {role.isDefault && <span className="role-default-badge">Default</span>}
                      </div>
                    </td>
                    <td className="description-cell">{role.description || 'No description'}</td>
                    <td>{role.memberCount} members</td>
                    <td>
                      {role.isDefault ? (
                        <span className="role-type-badge role-type-default">System</span>
                      ) : (
                        <span className="role-type-badge role-type-custom">Custom</span>
                      )}
                    </td>
                    <td>
                      <div className="action-buttons">
                        <button className="admin-btn-secondary" onClick={() => openEditModal(role)}>Edit</button>
                        {!role.isDefault && (
                          <button className="admin-btn-danger" onClick={() => handleDelete(role)}>Delete</button>
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
                  {modalMode === 'create' ? 'Add Role' : 'Edit Role'}
                </h2>
                <button className="admin-modal-close" onClick={closeModal}>×</button>
              </div>
              <div className="admin-modal-body">
                <div className="admin-form-group">
                  <label className="admin-form-label admin-form-label-required">Role Name</label>
                  <input
                    type="text"
                    className="admin-form-input"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    placeholder="e.g., Developers, Testers, Viewers"
                  />
                </div>
                <div className="admin-form-group">
                  <label className="admin-form-label">Description</label>
                  <textarea
                    className="admin-form-textarea"
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    placeholder="Describe what this role is for..."
                    rows={3}
                  />
                </div>
              </div>
              <div className="admin-modal-footer">
                <button className="admin-btn-secondary" onClick={closeModal}>Cancel</button>
                <button
                  className="admin-btn-primary"
                  onClick={modalMode === 'create' ? handleCreate : handleUpdate}
                  disabled={modalMode === 'create' ? createRole.isPending : updateRole.isPending}
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