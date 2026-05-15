import React, { useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import './ProjectAdministrationPage.css';

interface Project {
  id: string;
  key: string;
  name: string;
  lead: string;
  type: string;
  status: string;
  issues: number;
}

const mockProjects: Project[] = [
  { id: '1', key: 'PROJ', name: 'Product Development', lead: 'John Smith', type: 'Software', status: 'Active', issues: 245 },
  { id: '2', key: 'HR', name: 'Human Resources', lead: 'Jane Doe', type: 'Business', status: 'Active', issues: 32 },
  { id: '3', key: 'MKT', name: 'Marketing', lead: 'Mike Johnson', type: 'Business', status: 'Active', issues: 18 },
  { id: '4', key: 'SEC', name: 'Security', lead: 'Sarah Wilson', type: 'Software', status: 'Active', issues: 67 },
  { id: '5', key: 'INFRA', name: 'Infrastructure', lead: 'Tom Brown', type: 'Operations', status: 'Active', issues: 89 },
  { id: '6', key: 'MOBILE', name: 'Mobile App', lead: 'Lisa Chen', type: 'Software', status: 'Inactive', issues: 156 },
];

export default function ProjectAdministrationPage() {
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const filteredProjects = mockProjects.filter(project => {
    const matchesSearch = project.name.toLowerCase().includes(search.toLowerCase()) ||
      project.key.toLowerCase().includes(search.toLowerCase());
    const matchesType = !typeFilter || project.type === typeFilter;
    const matchesStatus = !statusFilter || project.status === statusFilter;
    return matchesSearch && matchesType && matchesStatus;
  });

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <h1 className="admin-page-title">Projects</h1>
          <p className="admin-page-description">
            Manage projects, project types, and categories.
          </p>
        </div>

        <div className="admin-stats-grid">
          <div className="admin-stat-card">
            <div className="admin-stat-label">Total Projects</div>
            <div className="admin-stat-value">{mockProjects.length}</div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Active Projects</div>
            <div className="admin-stat-value">
              {mockProjects.filter(p => p.status === 'Active').length}
            </div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Total Issues</div>
            <div className="admin-stat-value">
              {mockProjects.reduce((sum, p) => sum + p.issues, 0)}
            </div>
          </div>
          <div className="admin-stat-card">
            <div className="admin-stat-label">Categories</div>
            <div className="admin-stat-value">4</div>
          </div>
        </div>

        <div className="admin-toolbar">
          <div className="admin-toolbar-left">
            <input
              type="text"
              placeholder="Search projects..."
              className="admin-search-input-toolbar"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <select
              className="admin-form-select"
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              style={{ width: '140px' }}
            >
              <option value="">All Types</option>
              <option value="Software">Software</option>
              <option value="Business">Business</option>
              <option value="Operations">Operations</option>
            </select>
            <select
              className="admin-form-select"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              style={{ width: '140px' }}
            >
              <option value="">All Status</option>
              <option value="Active">Active</option>
              <option value="Inactive">Inactive</option>
              <option value="Archived">Archived</option>
            </select>
          </div>
          <div className="admin-toolbar-right">
            <button className="admin-btn-secondary">Project Categories</button>
            <button className="admin-btn-secondary">Project Types</button>
            <button className="admin-btn-primary">Create Project</button>
          </div>
        </div>

        <div className="admin-table-container">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Project</th>
                <th>Key</th>
                <th>Lead</th>
                <th>Type</th>
                <th>Status</th>
                <th>Issues</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredProjects.map((project) => (
                <tr key={project.id}>
                  <td>
                    <div className="project-cell">
                      <span className="project-icon">{project.key.charAt(0)}</span>
                      <span className="project-name">{project.name}</span>
                    </div>
                  </td>
                  <td>
                    <code className="project-key">{project.key}</code>
                  </td>
                  <td>{project.lead}</td>
                  <td>{project.type}</td>
                  <td>
                    <span className={`admin-status ${project.status === 'Active' ? 'admin-status-active' : 'admin-status-inactive'}`}>
                      {project.status}
                    </span>
                  </td>
                  <td>{project.issues.toLocaleString()}</td>
                  <td>
                    <div className="action-buttons">
                      <button className="admin-btn-secondary">Settings</button>
                      <button className="admin-btn-secondary">Details</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </AdminLayout>
  );
}