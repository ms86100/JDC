import React, { useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import { useStatuses, Status } from '../hooks/useAdminApi';
import './StatusesPage.css';

export default function StatusesPage() {
  const [search, setSearch] = useState('');
  const { data: statuses, isLoading } = useStatuses();

  const filteredStatuses = statuses?.filter(s =>
    s.name.toLowerCase().includes(search.toLowerCase())
  ) || [];

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">Statuses</h1>
          <p className="admin-page-description">
            Manage issue statuses and their categories.
          </p>
        </div>

        <div className="admin-stats-grid">
          <div className="admin-stat-card">
            <div className="admin-stat-label">Total Statuses</div>
            <div className="admin-stat-value">{statuses?.length || 0}</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">To Do</div>
            <div className="admin-stat-value">
              {statuses?.filter(s => s.statusCategory === 'TODO').length || 0}
            </div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">In Progress</div>
            <div className="admin-stat-value">
              {statuses?.filter(s => s.statusCategory === 'IN_PROGRESS').length || 0}
            </div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Done</div>
            <div className="admin-stat-value">
              {statuses?.filter(s => s.statusCategory === 'DONE').length || 0}
            </div>
          </div>
        </div>

        <div className="admin-toolbar">
          <div className="admin-toolbar-left">
            <input
              type="text"
              placeholder="Search statuses..."
              className="admin-search-input-toolbar"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="admin-toolbar-right">
            <button className="admin-btn-secondary">Import Statuses</button>
            <button className="admin-btn-primary">Add Status</button>
          </div>
        </div>

        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Status</th>
                <th>Description</th>
                <th>Category</th>
                <th>Color</th>
                <th>Sequence</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <>
                  {[...Array(8)].map((_, i) => (
                    <tr key={i}>
                      <td style={{ padding: '12px 16px' }}><div style={{ display: 'flex', alignItems: 'center', gap: 8 }}><div className="ab-skeleton" style={{ height: 24, width: 24, borderRadius: '50%', flexShrink: 0 }} /><div className="ab-skeleton" style={{ height: 16, width: '50%', borderRadius: 'var(--sa-radius-sm)' }} /></div></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: '70%', borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 22, width: 80, borderRadius: 12 }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 60, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 30, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                      <td style={{ padding: '12px 16px' }}><div className="ab-skeleton" style={{ height: 16, width: 100, borderRadius: 'var(--sa-radius-sm)' }} /></td>
                    </tr>
                  ))}
                </>
              ) : filteredStatuses.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '24px' }}>No statuses found</td>
                </tr>
              ) : (
                filteredStatuses.map((status) => (
                  <tr key={status.id}>
                    <td>
                      <div className="status-cell">
                        <span className="status-icon" style={{ backgroundColor: status.statusColor }}>
                          {status.name.charAt(0)}
                        </span>
                        <span className="status-name">{status.name}</span>
                      </div>
                    </td>
                    <td className="description-cell">{status.description || 'No description'}</td>
                    <td>
                      <span className={`status-category category-${status.statusCategory.toLowerCase()}`}>
                        {status.statusCategory.replace('_', ' ')}
                      </span>
                    </td>
                    <td>
                      <span className="status-color-badge" style={{ backgroundColor: status.statusColor }}>
                        {status.statusColor}
                      </span>
                    </td>
                    <td>{status.sequence}</td>
                    <td>
                      <div className="action-buttons">
                        <button className="admin-btn-secondary">Edit</button>
                        <button className="admin-btn-secondary">Configure</button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </AdminLayout>
  );
}