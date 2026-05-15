import React, { useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import './PermissionsPage.css';

type TabType = 'permission-schemes' | 'notification-schemes' | 'issue-security';

export default function PermissionsPage() {
  const [activeTab, setActiveTab] = useState<TabType>('permission-schemes');

  const renderPermissionSchemes = () => (
    <>
      <div className="admin-stats-grid">
        <div className="admin-stat-card">
          <div className="admin-stat-label">Total Schemes</div>
          <div className="admin-stat-value">4</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Default Scheme</div>
          <div className="admin-stat-value">Permission</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Projects Using</div>
          <div className="admin-stat-value">6</div>
        </div>
      </div>

      <div className="admin-toolbar">
        <div className="admin-toolbar-left">
          <input
            type="text"
            placeholder="Search schemes..."
            className="admin-search-input-toolbar"
          />
        </div>
        <div className="admin-toolbar-right">
          <button className="admin-btn-primary">Add Permission Scheme</button>
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
            <tr>
              <td>
                <div className="scheme-cell">
                  <span className="scheme-icon">P</span>
                  <span className="scheme-name">Default Permission Scheme</span>
                </div>
              </td>
              <td className="description-cell">Default permission scheme for all projects</td>
              <td>4 projects</td>
              <td>32 permissions</td>
              <td>
                <div className="action-buttons">
                  <button className="admin-btn-secondary">Edit</button>
                  <button className="admin-btn-secondary">Copy</button>
                </div>
              </td>
            </tr>
            <tr>
              <td>
                <div className="scheme-cell">
                  <span className="scheme-icon">P</span>
                  <span className="scheme-name">Admin Only Scheme</span>
                </div>
              </td>
              <td className="description-cell">Restrictive scheme for admin projects</td>
              <td>1 project</td>
              <td>12 permissions</td>
              <td>
                <div className="action-buttons">
                  <button className="admin-btn-secondary">Edit</button>
                  <button className="admin-btn-secondary">Copy</button>
                </div>
              </td>
            </tr>
            <tr>
              <td>
                <div className="scheme-cell">
                  <span className="scheme-icon">P</span>
                  <span className="scheme-name">Public Read-Only Scheme</span>
                </div>
              </td>
              <td className="description-cell">Allow public read access to projects</td>
              <td>1 project</td>
              <td>8 permissions</td>
              <td>
                <div className="action-buttons">
                  <button className="admin-btn-secondary">Edit</button>
                  <button className="admin-btn-secondary">Copy</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </>
  );

  const renderNotificationSchemes = () => (
    <>
      <div className="admin-stats-grid">
        <div className="admin-stat-card">
          <div className="admin-stat-label">Total Schemes</div>
          <div className="admin-stat-value">3</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Default Scheme</div>
          <div className="admin-stat-value">Notification</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Projects Using</div>
          <div className="admin-stat-value">6</div>
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
          <button className="admin-btn-primary">Add Notification Scheme</button>
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
            <tr>
              <td>
                <div className="scheme-cell">
                  <span className="scheme-icon notification">N</span>
                  <span className="scheme-name">Default Notification Scheme</span>
                </div>
              </td>
              <td className="description-cell">Standard notification scheme for all events</td>
              <td>4 projects</td>
              <td>12 events</td>
              <td>
                <div className="action-buttons">
                  <button className="admin-btn-secondary">Edit</button>
                  <button className="admin-btn-secondary">Copy</button>
                </div>
              </td>
            </tr>
            <tr>
              <td>
                <div className="scheme-cell">
                  <span className="scheme-icon notification">N</span>
                  <span className="scheme-name">Minimal Notifications</span>
                </div>
              </td>
              <td className="description-cell">Only notify assignees and reporters</td>
              <td>2 projects</td>
              <td>4 events</td>
              <td>
                <div className="action-buttons">
                  <button className="admin-btn-secondary">Edit</button>
                  <button className="admin-btn-secondary">Copy</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </>
  );

  const renderIssueSecurity = () => (
    <>
      <div className="admin-stats-grid">
        <div className="admin-stat-card">
          <div className="admin-stat-label">Total Schemes</div>
          <div className="admin-stat-value">2</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Projects Using</div>
          <div className="admin-stat-value">2</div>
        </div>
        <div className="admin-stat-card">
          <div className="admin-stat-label">Security Levels</div>
          <div className="admin-stat-value">5</div>
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
          <button className="admin-btn-primary">Add Security Scheme</button>
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
            <tr>
              <td>
                <div className="scheme-cell">
                  <span className="scheme-icon security">S</span>
                  <span className="scheme-name">Default Issue Security Scheme</span>
                </div>
              </td>
              <td className="description-cell">Security levels for confidential projects</td>
              <td>1 project</td>
              <td>3 levels</td>
              <td>
                <div className="action-buttons">
                  <button className="admin-btn-secondary">Edit</button>
                  <button className="admin-btn-secondary">Copy</button>
                </div>
              </td>
            </tr>
            <tr>
              <td>
                <div className="scheme-cell">
                  <span className="scheme-icon security">S</span>
                  <span className="scheme-name">Strict Security Scheme</span>
                </div>
              </td>
              <td className="description-cell">Maximum security for sensitive projects</td>
              <td>1 project</td>
              <td>2 levels</td>
              <td>
                <div className="action-buttons">
                  <button className="admin-btn-secondary">Edit</button>
                  <button className="admin-btn-secondary">Copy</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </>
  );

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
      </div>
    </AdminLayout>
  );
}