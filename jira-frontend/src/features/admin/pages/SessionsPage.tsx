import React, { useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import {
  useUserSessions,
  useRevokeSession,
  useRevokeAllSessions,
  useSessionPolicy,
  useUpdateSessionPolicy,
  UserSession,
  SessionPolicy,
} from '../hooks/useAdminApi';
import './SessionsPage.css';

export default function SessionsPage() {
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [showPolicyModal, setShowPolicyModal] = useState(false);
  const [policyForm, setPolicyForm] = useState<SessionPolicy>({
    sessionTimeout: 60,
    maxSessions: 0,
    allowMultipleSessions: true,
  });

  const { data: sessions, isLoading } = useUserSessions();
  const { data: policy } = useSessionPolicy();
  const revokeSession = useRevokeSession();
  const revokeAllSessions = useRevokeAllSessions();
  const updatePolicy = useUpdateSessionPolicy();

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

  const handleRevokeSession = async (session: UserSession) => {
    if (session.isCurrent) {
      showMessage('Cannot revoke your current session', true);
      return;
    }
    if (!confirm(`Are you sure you want to revoke the session for "${session.displayName}"?\n\nIP: ${session.ipAddress}\nLast active: ${formatRelativeTime(session.lastActive)}`)) return;
    try {
      await revokeSession.mutateAsync(session.id);
      showMessage('Session revoked successfully');
    } catch (err: any) {
      showMessage(err?.message || 'Failed to revoke session', true);
    }
  };

  const handleRevokeAll = async () => {
    if (!confirm('Are you sure you want to revoke ALL sessions?\n\nThis will log out all users except you.')) return;
    try {
      await revokeAllSessions.mutateAsync();
      showMessage('All sessions revoked successfully');
    } catch (err: any) {
      showMessage(err?.message || 'Failed to revoke sessions', true);
    }
  };

  const handleRevokeUserSessions = async (userId: string, userName: string) => {
    if (!confirm(`Are you sure you want to revoke all sessions for "${userName}"?`)) return;
    try {
      await revokeAllSessions.mutateAsync(userId);
      showMessage(`All sessions for "${userName}" revoked successfully`);
    } catch (err: any) {
      showMessage(err?.message || 'Failed to revoke sessions', true);
    }
  };

  const openPolicyModal = () => {
    if (policy) {
      setPolicyForm({ ...policy });
    }
    setShowPolicyModal(true);
  };

  const closePolicyModal = () => {
    setShowPolicyModal(false);
    setError(null);
  };

  const handleUpdatePolicy = async () => {
    try {
      await updatePolicy.mutateAsync(policyForm);
      showMessage('Session policy updated successfully');
      closePolicyModal();
    } catch (err: any) {
      showMessage(err?.message || 'Failed to update policy', true);
    }
  };

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">Sessions</h1>
          <p className="admin-page-description">
            View and manage active user sessions. Revoke sessions to force logout.
          </p>
        </div>

        {error && <div className="admin-alert admin-alert-error">{error}</div>}
        {success && <div className="admin-alert admin-alert-success">{success}</div>}

        <div className="admin-stats-grid">
          <div className="admin-stat-card">
            <div className="admin-stat-label">Active Sessions</div>
            <div className="admin-stat-value">{sessions?.length || 0}</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Unique Users</div>
            <div className="admin-stat-value">{new Set(sessions?.map(s => s.userId)).size || 0}</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Session Timeout</div>
            <div className="admin-stat-value">{policy?.sessionTimeout || 60} min</div>
          </div>
        </div>

        <div className="admin-toolbar">
          <div className="admin-toolbar-left">
            <input
              type="text"
              placeholder="Search sessions..."
              className="admin-search-input-toolbar"
            />
          </div>
          <div className="admin-toolbar-right">
            <button className="admin-btn-secondary" onClick={openPolicyModal}>Session Policy</button>
            <button className="admin-btn-danger" onClick={handleRevokeAll}>Revoke All Sessions</button>
          </div>
        </div>

        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>User</th>
                <th>IP Address</th>
                <th>Login Time</th>
                <th>Last Active</th>
                <th>Expires</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr><td colSpan={7} className="loading-cell">Loading...</td></tr>
              ) : sessions?.length === 0 ? (
                <tr><td colSpan={7} className="empty-cell">No active sessions found</td></tr>
              ) : (
                sessions?.map((session) => (
                  <tr key={session.id}>
                    <td>
                      <div className="user-cell">
                        <div className="user-avatar">{session.displayName.charAt(0).toUpperCase()}</div>
                        <div className="user-info">
                          <div className="user-name">{session.displayName}</div>
                          <div className="user-username">@{session.userName}</div>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span className="ip-address">{session.ipAddress}</span>
                      <span className="user-agent" title={session.userAgent}>{session.userAgent.substring(0, 40)}...</span>
                    </td>
                    <td>{formatDateTime(session.loginTime)}</td>
                    <td>{formatRelativeTime(session.lastActive)}</td>
                    <td>{formatRelativeTime(session.expiresAt)}</td>
                    <td>
                      {session.isCurrent ? (
                        <span className="session-status session-status-current">Current</span>
                      ) : (
                        <span className="session-status session-status-active">Active</span>
                      )}
                    </td>
                    <td>
                      <div className="action-buttons">
                        <button
                          className="admin-btn-danger"
                          onClick={() => handleRevokeSession(session)}
                          disabled={session.isCurrent}
                          title={session.isCurrent ? 'Cannot revoke current session' : 'Revoke this session'}
                        >
                          Revoke
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Session Policy Modal */}
        {showPolicyModal && (
          <div className="admin-modal-overlay" onClick={closePolicyModal}>
            <div className="admin-modal" onClick={(e) => e.stopPropagation()}>
              <div className="admin-modal-header">
                <h2 className="admin-modal-title">Session Policy</h2>
                <button className="admin-modal-close" onClick={closePolicyModal}>×</button>
              </div>
              <div className="admin-modal-body">
                <div className="admin-form-group">
                  <label className="admin-form-label">Session Timeout (minutes)</label>
                  <input
                    type="number"
                    className="admin-form-input admin-form-input-narrow"
                    value={policyForm.sessionTimeout}
                    onChange={(e) => setPolicyForm({ ...policyForm, sessionTimeout: parseInt(e.target.value) || 30 })}
                    min={5}
                    max={480}
                  />
                  <span className="form-help">How long a session can be idle before requiring re-authentication</span>
                </div>

                <div className="admin-form-group">
                  <label className="admin-form-label">Maximum Concurrent Sessions</label>
                  <input
                    type="number"
                    className="admin-form-input admin-form-input-narrow"
                    value={policyForm.maxSessions}
                    onChange={(e) => setPolicyForm({ ...policyForm, maxSessions: parseInt(e.target.value) || 0 })}
                    min={0}
                    max={100}
                  />
                  <span className="form-help">Set to 0 for unlimited concurrent sessions</span>
                </div>

                <div className="form-checkbox-group">
                  <label className="form-checkbox-label">
                    <input
                      type="checkbox"
                      checked={policyForm.allowMultipleSessions}
                      onChange={(e) => setPolicyForm({ ...policyForm, allowMultipleSessions: e.target.checked })}
                    />
                    Allow multiple sessions per user
                  </label>
                </div>
              </div>
              <div className="admin-modal-footer">
                <button className="admin-btn-secondary" onClick={closePolicyModal}>Cancel</button>
                <button
                  className="admin-btn-primary"
                  onClick={handleUpdatePolicy}
                  disabled={updatePolicy.isPending}
                >
                  Save Policy
                </button>
              </div>
            </div>
          </div>
        )}
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

function formatRelativeTime(dateString: string): string {
  try {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} min ago`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays < 30) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  } catch {
    return dateString;
  }
}