import React, { useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import { useUsers, useCreateUser, useUpdateUser, useDeleteUser, User } from '../hooks/useAdminApi';
import './UserManagementPage.css';

export default function UserManagementPage() {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [page, setPage] = useState(0);
  const pageSize = 20;

  const { data, isLoading, refetch } = useUsers({ search, status: statusFilter, page, size: pageSize });
  const createUser = useCreateUser();
  const updateUser = useUpdateUser();
  const deleteUser = useDeleteUser();

  const [formData, setFormData] = useState({
    username: '',
    email: '',
    displayName: '',
    role: 'USER',
  });

  const handleCreate = async () => {
    try {
      await createUser.mutateAsync(formData);
      setShowCreateModal(false);
      setFormData({ username: '', email: '', displayName: '', role: 'USER' });
      refetch();
    } catch (error) {
      console.error('Failed to create user:', error);
    }
  };

  const handleEdit = async () => {
    if (!selectedUser) return;
    try {
      await updateUser.mutateAsync({ userId: selectedUser.id, data: selectedUser });
      setShowEditModal(false);
      setSelectedUser(null);
      refetch();
    } catch (error) {
      console.error('Failed to update user:', error);
    }
  };

  const handleDelete = async (userId: string) => {
    if (!confirm('Are you sure you want to delete this user?')) return;
    try {
      await deleteUser.mutateAsync(userId);
      refetch();
    } catch (error) {
      console.error('Failed to delete user:', error);
    }
  };

  const openEditModal = (user: User) => {
    setSelectedUser({ ...user });
    setShowEditModal(true);
  };

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">User Management</h1>
          <p className="admin-page-description">
            Manage users, groups, and global permissions for the system.
          </p>
        </div>

        <div className="admin-toolbar">
          <div className="admin-toolbar-left">
            <input
              type="text"
              placeholder="Search users..."
              className="admin-search-input-toolbar"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <select
              className="admin-form-select"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              style={{ width: '150px' }}
            >
              <option value="">All Statuses</option>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
              <option value="SUSPENDED">Suspended</option>
              <option value="PENDING">Pending</option>
            </select>
          </div>
          <div className="admin-toolbar-right">
            <button className="admin-btn-primary" onClick={() => setShowCreateModal(true)}>
              Create User
            </button>
          </div>
        </div>

        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>User</th>
                <th>Email</th>
                <th>Status</th>
                <th>Role</th>
                <th>Last Login</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '24px' }}>
                    Loading...
                  </td>
                </tr>
              ) : data?.content?.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '24px' }}>
                    No users found
                  </td>
                </tr>
              ) : (
                data?.content?.map((user) => (
                  <tr key={user.id}>
                    <td>
                      <div className="user-cell">
                        <div className="user-avatar">
                          {user.displayName.charAt(0).toUpperCase()}
                        </div>
                        <div className="user-info">
                          <div className="user-name">{user.displayName}</div>
                          <div className="user-username">@{user.username}</div>
                        </div>
                      </div>
                    </td>
                    <td>{user.email}</td>
                    <td>
                      <span className={`admin-status admin-status-${user.status.toLowerCase()}`}>
                        {user.status}
                      </span>
                    </td>
                    <td>{user.role}</td>
                    <td>{user.lastLogin ? new Date(user.lastLogin).toLocaleDateString() : 'Never'}</td>
                    <td>{new Date(user.createdAt).toLocaleDateString()}</td>
                    <td>
                      <div className="action-buttons">
                        <button className="admin-btn-secondary" onClick={() => openEditModal(user)}>
                          Edit
                        </button>
                        <button className="admin-btn-danger" onClick={() => handleDelete(user.id)}>
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>

          {data && data.totalElements > pageSize && (
            <div className="admin-pagination">
              <div className="admin-pagination-info">
                Showing {page * pageSize + 1} to {Math.min((page + 1) * pageSize, data.totalElements)} of {data.totalElements} users
              </div>
              <div className="admin-pagination-controls">
                <button
                  className="admin-pagination-btn"
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                >
                  ←
                </button>
                <button className="admin-pagination-btn" disabled>
                  {page + 1}
                </button>
                <button
                  className="admin-pagination-btn"
                  onClick={() => setPage(p => Math.ceil(data.totalElements / pageSize) - 1 > p ? p + 1 : p)}
                  disabled={Math.ceil(data.totalElements / pageSize) - 1 <= page}
                >
                  →
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Create User Modal */}
      {showCreateModal && (
        <div className="admin-modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h2 className="admin-modal-title">Create User</h2>
              <button className="admin-modal-close" onClick={() => setShowCreateModal(false)}>×</button>
            </div>
            <div className="admin-modal-body">
              <div className="admin-form-group">
                <label className="admin-form-label admin-form-label-required">Username</label>
                <input
                  type="text"
                  className="admin-form-input"
                  value={formData.username}
                  onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                />
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label admin-form-label-required">Email</label>
                <input
                  type="email"
                  className="admin-form-input"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                />
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label admin-form-label-required">Display Name</label>
                <input
                  type="text"
                  className="admin-form-input"
                  value={formData.displayName}
                  onChange={(e) => setFormData({ ...formData, displayName: e.target.value })}
                />
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label">Role</label>
                <select
                  className="admin-form-select"
                  value={formData.role}
                  onChange={(e) => setFormData({ ...formData, role: e.target.value })}
                >
                  <option value="USER">User</option>
                  <option value="ADMIN">Admin</option>
                </select>
              </div>
            </div>
            <div className="admin-modal-footer">
              <button className="admin-btn-secondary" onClick={() => setShowCreateModal(false)}>
                Cancel
              </button>
              <button className="admin-btn-primary" onClick={handleCreate} disabled={createUser.isPending}>
                {createUser.isPending ? 'Creating...' : 'Create'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit User Modal */}
      {showEditModal && selectedUser && (
        <div className="admin-modal-overlay" onClick={() => setShowEditModal(false)}>
          <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
            <div className="admin-modal-header">
              <h2 className="admin-modal-title">Edit User</h2>
              <button className="admin-modal-close" onClick={() => setShowEditModal(false)}>×</button>
            </div>
            <div className="admin-modal-body">
              <div className="admin-form-group">
                <label className="admin-form-label">Display Name</label>
                <input
                  type="text"
                  className="admin-form-input"
                  value={selectedUser.displayName}
                  onChange={(e) => setSelectedUser({ ...selectedUser, displayName: e.target.value })}
                />
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label">Email</label>
                <input
                  type="email"
                  className="admin-form-input"
                  value={selectedUser.email}
                  onChange={(e) => setSelectedUser({ ...selectedUser, email: e.target.value })}
                />
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label">Role</label>
                <select
                  className="admin-form-select"
                  value={selectedUser.role}
                  onChange={(e) => setSelectedUser({ ...selectedUser, role: e.target.value })}
                >
                  <option value="USER">User</option>
                  <option value="ADMIN">Admin</option>
                </select>
              </div>
              <div className="admin-form-group">
                <label className="admin-form-label">Status</label>
                <select
                  className="admin-form-select"
                  value={selectedUser.status}
                  onChange={(e) => setSelectedUser({ ...selectedUser, status: e.target.value as any })}
                >
                  <option value="ACTIVE">Active</option>
                  <option value="INACTIVE">Inactive</option>
                  <option value="SUSPENDED">Suspended</option>
                </select>
              </div>
            </div>
            <div className="admin-modal-footer">
              <button className="admin-btn-secondary" onClick={() => setShowEditModal(false)}>
                Cancel
              </button>
              <button className="admin-btn-primary" onClick={handleEdit} disabled={updateUser.isPending}>
                {updateUser.isPending ? 'Saving...' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}