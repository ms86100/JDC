import React, { useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import { usePriorities, Priority } from '../hooks/useAdminApi';
import './PrioritiesPage.css';

export default function PrioritiesPage() {
  const [search, setSearch] = useState('');
  const { data: priorities, isLoading } = usePriorities();

  const filteredPriorities = priorities?.filter(p =>
    p.name.toLowerCase().includes(search.toLowerCase())
  ) || [];

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">Priorities</h1>
          <p className="admin-page-description">
            Configure issue priorities and their display order.
          </p>
        </div>

        <div className="admin-toolbar">
          <div className="admin-toolbar-left">
            <input
              type="text"
              placeholder="Search priorities..."
              className="admin-search-input-toolbar"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <div className="admin-toolbar-right">
            <button className="admin-btn-primary">Add Priority</button>
          </div>
        </div>

        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Status Icon</th>
                <th>Priority Name</th>
                <th>Description</th>
                <th>Color</th>
                <th>Default</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '24px' }}>Loading...</td>
                </tr>
              ) : filteredPriorities.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', padding: '24px' }}>No priorities found</td>
                </tr>
              ) : (
                filteredPriorities.map((priority) => (
                  <tr key={priority.id}>
                    <td>
                      <div className="priority-icon" style={{ backgroundColor: priority.statusColor }}>
                        {priority.name.charAt(0)}
                      </div>
                    </td>
                    <td>
                      <span className="priority-name">{priority.name}</span>
                    </td>
                    <td className="description-cell">{priority.description || 'No description'}</td>
                    <td>
                      <span className="priority-color" style={{ backgroundColor: priority.statusColor }}>
                        {priority.statusColor}
                      </span>
                    </td>
                    <td>
                      {priority.isDefault && (
                        <span className="admin-status admin-status-active">Default</span>
                      )}
                    </td>
                    <td>
                      <div className="action-buttons">
                        <button className="admin-btn-secondary">Edit</button>
                        {!priority.isDefault && (
                          <button className="admin-btn-danger">Delete</button>
                        )}
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