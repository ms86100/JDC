import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useJiraUsers, useJiraGroups } from '../hooks/useAdminApi';
import type { JiraUser } from '../hooks/useAdminApi';
import { appNotify } from '../../../lib/appNotify';
import './JiraUserBrowser.css';

export default function JiraUserBrowser() {
  const [search, setSearch] = useState('');
  const [groupFilter, setGroupFilter] = useState<string>('');
  const [applicationFilter, setApplicationFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState(0);
  const pageSize = 20;

  const { data, isLoading, refetch } = useJiraUsers({
    search: search || undefined,
    status: statusFilter !== 'ALL' ? statusFilter : undefined,
    page,
    size: pageSize,
  });

  const groups = useJiraGroups({ size: 100 });

  const handleFilter = () => {
    setPage(0);
    refetch();
  };

  const handleReset = () => {
    setSearch('');
    setGroupFilter('');
    setApplicationFilter('ALL');
    setStatusFilter('ALL');
    setPage(0);
  };

  return (
      <div className="user-browser">
        <div className="user-browser-header">
          <h1 className="user-browser-title">Users</h1>
          <div className="user-browser-actions">
            <button className="btn-secondary" onClick={() => appNotify.info('Invite users feature coming soon')}>Invite users</button>
            <Link to="/admin/users/create" className="btn-primary">Create user</Link>
          </div>
        </div>

        <div className="user-browser-filters">
          <div className="filter-row">
            <div className="filter-field">
              <label className="filter-label">Filter users</label>
              <input
                type="text"
                className="filter-input"
                placeholder="Name, username or email contains"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleFilter()}
              />
            </div>
            <div className="filter-field">
              <label className="filter-label">In group</label>
              <select
                className="filter-select"
                value={groupFilter}
                onChange={(e) => setGroupFilter(e.target.value)}
              >
                <option value="">Any</option>
                {groups.data?.content?.map((g) => (
                  <option key={g.id} value={g.name}>{g.name}</option>
                ))}
              </select>
            </div>
            <div className="filter-field">
              <label className="filter-label">Application access</label>
              <select
                className="filter-select"
                value={applicationFilter}
                onChange={(e) => setApplicationFilter(e.target.value)}
              >
                <option value="ALL">All Users</option>
                <option value="NONE">None</option>
                <option value="jira-software">Systems and Avionics</option>
              </select>
            </div>
            <div className="filter-field">
              <label className="filter-label">Status</label>
              <select
                className="filter-select"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
              >
                <option value="ALL">All Users</option>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </div>
            <div className="filter-field">
              <label className="filter-label">Users per page</label>
              <select
                className="filter-select"
                defaultValue={pageSize}
              >
                <option value="10">10</option>
                <option value="20">20</option>
                <option value="50">50</option>
                <option value="100">100</option>
              </select>
            </div>
          </div>
          <div className="filter-actions">
            <button className="btn-primary" onClick={handleFilter}>Filter</button>
            <button className="btn-link" onClick={handleReset}>Reset filter</button>
          </div>
        </div>

        <div className="user-browser-pagination-info">
          Displaying users {data?.content?.length ? page * pageSize + 1 : 0} to{' '}
          {Math.min((page + 1) * pageSize, data?.totalElements || 0)} of {data?.totalElements || 0}
        </div>

        <table className="user-table">
          <thead>
            <tr>
              <th>Full name</th>
              <th>Username</th>
              <th>Login details</th>
              <th>Group name</th>
              <th>Applications</th>
              <th>Directory</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <>
                {[...Array(8)].map((_, i) => (
                  <tr key={i}>
                    <td style={{ padding: '12px 16px' }}><div style={{ display: 'flex', alignItems: 'center', gap: 8 }}><div className="ab-skeleton" style={{ height: 32, width: 32, borderRadius: '50%', flexShrink: 0 }} /><div className="ab-skeleton" style={{ height: 16, width: '60%', borderRadius: 'var(--sa-radius-sm)' }} /></div></td>
                    <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: '70%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: '50%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: '40%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: '30%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: '50%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 40, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                  </tr>
                ))}
              </>
            ) : data?.content?.length === 0 ? (
              <tr>
                <td colSpan={7} className="empty-cell">No users found</td>
              </tr>
            ) : (
              data?.content?.map((user) => (
                <UserRow key={user.id} user={user} />
              ))
            )}
          </tbody>
        </table>
      </div>
  );
}

function UserRow({ user }: { user: JiraUser }) {
  return (
    <tr className="user-row">
      <td>
        <div className="user-cell">
          <div className="user-avatar-sm">
            {user.displayName?.charAt(0).toUpperCase() || '?'}
          </div>
          <div className="user-info">
            <span className="user-fullname">{user.displayName}</span>
          </div>
        </div>
      </td>
      <td>
        <span className="user-username">{user.userName}</span>
        <span className="user-email">{user.emailAddress}</span>
      </td>
      <td>
        <div className="login-details">
          <span>Count: {user.loginInfo?.loginCount || 0}</span>
          <span>Last: {user.loginInfo?.lastLogin ? formatRelativeTime(user.loginInfo.lastLogin) : 'Never'}</span>
        </div>
      </td>
      <td>
        <div className="user-groups">
          {user.groups?.map((g) => (
            <Link key={g.id} to={`/admin/groups/view?name=${g.name}`} className="group-link">
              {g.name}
            </Link>
          ))}
        </div>
      </td>
      <td>
        <span className="application-badge">{user.applications?.join(', ') || '-'}</span>
      </td>
      <td>
        <span className="directory-name">{user.directoryName || 'Systems and Avionics Internal Directory'}</span>
      </td>
      <td>
        <div className="action-links">
          <Link to={`/admin/users/edit/${user.id}`} className="action-link">Edit</Link>
          <button className="action-link action-link-danger" onClick={() => {
            if (confirm(`Delete user "${user.displayName}"?`)) {
              // Placeholder for delete functionality
              console.log('Delete user:', user.id);
            }
          }}>...</button>
        </div>
      </td>
    </tr>
  );
}

function formatRelativeTime(dateString: string): string {
  try {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} minutes ago`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours} hours ago`;
    const diffDays = Math.floor(diffHours / 24);
    if (diffDays < 30) return `${diffDays} days ago`;
    return date.toLocaleDateString();
  } catch {
    return dateString;
  }
}