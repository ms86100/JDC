import React, { useState } from 'react';
import { useAuditLogs, useAuditStatistics, AuditLog } from '../hooks/useAdminApi';
import AdminLayout from '../components/AdminLayout';
import './AuditLogsPage.css';

export default function AuditLogsPage() {
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [page, setPage] = useState(0);
  const pageSize = 50;

  const { data, isLoading, refetch } = useAuditLogs({
    category: categoryFilter,
    page,
    size: pageSize
  });
  const { data: statistics } = useAuditStatistics();

  const getSeverityClass = (severity: string) => {
    switch (severity) {
      case 'ERROR': return 'admin-status-inactive';
      case 'WARNING': return 'admin-status-pending';
      default: return 'admin-status-active';
    }
  };

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">Audit Logs</h1>
          <p className="admin-page-description">
            View and search audit logs for compliance and security monitoring.
          </p>
        </div>

        <div className="admin-stats-grid">
          <div className="admin-stat-card">
            <div className="admin-stat-label">Total Events (30d)</div>
            <div className="admin-stat-value">{statistics?.totalEvents || data?.totalElements || 0}</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Users Affected</div>
            <div className="admin-stat-value">{statistics?.uniqueUsers || 0}</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Categories</div>
            <div className="admin-stat-value">{statistics?.categories || 0}</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Failed Actions</div>
            <div className="admin-stat-value error">
              {statistics?.failedActions || 0}
            </div>
          </div>
        </div>

        <div className="admin-toolbar">
          <div className="admin-toolbar-left">
            <input
              type="text"
              placeholder="Search by username, action, or entity..."
              className="admin-search-input-toolbar"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <select
              className="admin-form-select"
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              style={{ width: '180px' }}
            >
              <option value="">All Categories</option>
              <option value="USER_MANAGEMENT">User Management</option>
              <option value="PROJECT">Project</option>
              <option value="WORKFLOW">Workflow</option>
              <option value="SCHEME">Scheme</option>
              <option value="SYSTEM">System</option>
              <option value="AUTHENTICATION">Authentication</option>
            </select>
            <button className="admin-btn-secondary">Date Range</button>
          </div>
          <div className="admin-toolbar-right">
            <button className="admin-btn-secondary">Export CSV</button>
            <button className="admin-btn-secondary" onClick={() => refetch()}>Refresh</button>
          </div>
        </div>

        <div className="admin-table-container">
          <table className="admin-table audit-table">
            <thead>
              <tr>
                <th>Timestamp</th>
                <th>User</th>
                <th>Action</th>
                <th>Category</th>
                <th>Entity</th>
                <th>Details</th>
                <th>Result</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '24px' }}>Loading...</td>
                </tr>
              ) : data?.content.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', padding: '24px' }}>No audit logs found</td>
                </tr>
              ) : (
                data?.content.map((log) => (
                  <tr key={log.id}>
                    <td>
                      <div className="timestamp-cell">
                        <span className="timestamp-date">
                          {new Date(log.timestamp).toLocaleDateString()}
                        </span>
                        <span className="timestamp-time">
                          {new Date(log.timestamp).toLocaleTimeString()}
                        </span>
                      </div>
                    </td>
                    <td>
                      <div className="user-cell">
                        <div className="user-avatar-small">
                          {log.userName.charAt(0).toUpperCase()}
                        </div>
                        <div className="user-info">
                          <div className="user-name">{log.userName}</div>
                          <div className="user-ip">{log.userIp}</div>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span className="action-badge">{log.action}</span>
                    </td>
                    <td>
                      <span className="category-badge">{log.category}</span>
                    </td>
                    <td>
                      <div className="entity-cell">
                        <span className="entity-type">{log.entityType}</span>
                        <span className="entity-id">{log.entityId}</span>
                        <span className="entity-name">{log.entityName}</span>
                      </div>
                    </td>
                    <td>
                      <div className="details-cell" title={log.details}>
                        {log.details.length > 60 ? log.details.substring(0, 60) + '...' : log.details}
                      </div>
                    </td>
                    <td>
                      <span className={`admin-status ${getSeverityClass(log.severity)}`}>
                        {log.result}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>

          {data && data.totalElements > pageSize && (
            <div className="admin-pagination">
              <div className="admin-pagination-info">
                Showing {page * pageSize + 1} to {Math.min((page + 1) * pageSize, data.totalElements)} of {data.totalElements} entries
              </div>
              <div className="admin-pagination-controls">
                <button
                  className="admin-pagination-btn"
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                >
                  ←
                </button>
                <span style={{ padding: '0 12px', display: 'flex', alignItems: 'center' }}>
                  Page {page + 1} of {Math.ceil(data.totalElements / pageSize)}
                </span>
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
    </AdminLayout>
  );
}