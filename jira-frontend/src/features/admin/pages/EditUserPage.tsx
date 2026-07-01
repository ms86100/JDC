import React, { useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import AdminLayout from '../components/AdminLayout';
import {
  useJiraUser,
  useDeleteJiraUser,
} from '../hooks/useAdminApi';
import './EditUserPage.css';

export default function EditUserPage() {
  const { userId } = useParams();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const { data: user, isLoading, error: fetchError } = useJiraUser(userId || '');
  const deleteUser = useDeleteJiraUser();

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

  const handleDelete = async () => {
    if (!user) return;
    if (!confirm(`Are you sure you want to delete the user "${user.displayName}"?\n\nThis action cannot be undone.`)) return;
    try {
      await deleteUser.mutateAsync(user.id);
      showMessage('User deleted successfully');
      setTimeout(() => navigate('/admin/users'), 1500);
    } catch (err: any) {
      showMessage(err?.message || 'Failed to delete user', true);
    }
  };

  if (!userId) {
    return (
      <AdminLayout>
        <div className="admin-page">
          <div className="admin-page-header">
            <h1 className="admin-page-title">Edit User</h1>
          </div>
          <div className="edit-user-error">
            <p>No user specified. Please select a user from the{' '}
              <Link to="/admin/users">Users list</Link>.
            </p>
          </div>
        </div>
      </AdminLayout>
    );
  }

  if (isLoading) {
    return (
      <AdminLayout>
        <div className="admin-page">
          <div className="admin-page-header">
            <h1 className="admin-page-title">Edit User</h1>
          </div>
          <div className="loading-state">Loading user details...</div>
        </div>
      </AdminLayout>
    );
  }

  if (fetchError || !user) {
    return (
      <AdminLayout>
        <div className="admin-page">
          <div className="admin-page-header">
            <h1 className="admin-page-title">Edit User</h1>
          </div>
          <div className="edit-user-error">
            <p>User not found.</p>
            <Link to="/admin/users">← Back to Users</Link>
          </div>
        </div>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">Edit User</h1>
        </div>

        {error && <div className="admin-alert admin-alert-error">{error}</div>}
        {success && <div className="admin-alert admin-alert-success">{success}</div>}

        <div className="admin-stats-grid">
          <div className="admin-stat-card">
            <div className="admin-stat-label">User</div>
            <div className="admin-stat-value">{user.displayName}</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Status</div>
            <div className="admin-stat-value">{user.active ? 'Active' : 'Inactive'}</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Directory</div>
            <div className="admin-stat-value">{user.directoryName}</div>
          </div>
        </div>

        <div className="edit-user-section">
          <h2 className="section-title">User Information</h2>
          <div className="info-table">
            <div className="info-row">
              <div className="info-label">Display Name</div>
              <div className="info-value">{user.displayName}</div>
            </div>
            <div className="info-row">
              <div className="info-label">Username</div>
              <div className="info-value">{user.userName}</div>
            </div>
            <div className="info-row">
              <div className="info-label">Email</div>
              <div className="info-value">{user.emailAddress}</div>
            </div>
            <div className="info-row">
              <div className="info-label">First Name</div>
              <div className="info-value">{user.firstName || '-'}</div>
            </div>
            <div className="info-row">
              <div className="info-label">Last Name</div>
              <div className="info-value">{user.lastName || '-'}</div>
            </div>
          </div>
        </div>

        <div className="edit-user-section">
          <h2 className="section-title">Groups</h2>
          <div className="groups-list">
            {user.groups?.length > 0 ? (
              user.groups.map(group => (
                <Link key={group.id} to={`/admin/groups/view?name=${encodeURIComponent(group.name)}`} className="group-tag">
                  {group.name}
                </Link>
              ))
            ) : (
              <span className="no-groups">Not a member of any groups</span>
            )}
          </div>
        </div>

        <div className="edit-user-section">
          <h2 className="section-title">Applications</h2>
          <div className="applications-list">
            {user.applications?.length > 0 ? (
              user.applications.map(app => (
                <span key={app} className="application-tag">{app}</span>
              ))
            ) : (
              <span className="no-applications">No application access</span>
            )}
          </div>
        </div>

        <div className="edit-user-section">
          <h2 className="section-title">Login Information</h2>
          <div className="info-table">
            <div className="info-row">
              <div className="info-label">Login Count</div>
              <div className="info-value">{user.loginInfo?.loginCount || 0}</div>
            </div>
            <div className="info-row">
              <div className="info-label">Last Login</div>
              <div className="info-value">
                {user.loginInfo?.lastLogin ? formatDateTime(user.loginInfo.lastLogin) : 'Never'}
              </div>
            </div>
            <div className="info-row">
              <div className="info-label">Created</div>
              <div className="info-value">{formatDateTime(user.createdDate)}</div>
            </div>
            <div className="info-row">
              <div className="info-label">Updated</div>
              <div className="info-value">{formatDateTime(user.updatedDate)}</div>
            </div>
          </div>
        </div>

        <div className="edit-user-actions">
          <Link to="/admin/users" className="admin-btn-secondary">Back to Users</Link>
          <button className="admin-btn-danger" onClick={handleDelete} disabled={deleteUser.isPending}>
            Delete User
          </button>
        </div>
      </div>
    </AdminLayout>
  );
}

function formatDateTime(dateString: string): string {
  try {
    return new Date(dateString).toLocaleString();
  } catch {
    return dateString;
  }
}