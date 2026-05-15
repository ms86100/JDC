import React, { useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import { useScreens } from '../hooks/useAdminApi';
import './ScreensPage.css';

export default function ScreensPage() {
  const [search, setSearch] = useState('');
  const [activeTab, setActiveTab] = useState<'screens' | 'schemes'>('screens');
  const { data: screens, isLoading } = useScreens();

  const filteredScreens = screens?.filter(s =>
    s.name.toLowerCase().includes(search.toLowerCase())
  ) || [];

  const renderScreensTab = () => (
    <>
      <div className="admin-toolbar">
        <div className="admin-toolbar-left">
          <input
            type="text"
            placeholder="Search screens..."
            className="admin-search-input-toolbar"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="admin-toolbar-right">
          <button className="admin-btn-primary">Add Screen</button>
        </div>
      </div>

      <div className="admin-table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Screen Name</th>
              <th>Description</th>
              <th>Tabs</th>
              <th>Fields</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={5} style={{ textAlign: 'center', padding: '24px' }}>Loading...</td>
              </tr>
            ) : filteredScreens.length === 0 ? (
              <tr>
                <td colSpan={5} style={{ textAlign: 'center', padding: '24px' }}>No screens found</td>
              </tr>
            ) : (
              filteredScreens.map((screen) => (
                <tr key={screen.id}>
                  <td>
                    <div className="screen-cell">
                      <span className="screen-icon">S</span>
                      <span className="screen-name">{screen.name}</span>
                    </div>
                  </td>
                  <td className="description-cell">{screen.description || 'No description'}</td>
                  <td>
                    <span className="tab-count">{screen.tabs?.length || 0} tabs</span>
                  </td>
                  <td>
                    <span className="field-count">
                      {screen.tabs?.reduce((sum, tab) => sum + (tab.fieldIds?.length || 0), 0) || 0} fields
                    </span>
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button className="admin-btn-secondary">Edit</button>
                      <button className="admin-btn-secondary">Configure Tabs</button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </>
  );

  const renderSchemesTab = () => (
    <>
      <div className="admin-toolbar">
        <div className="admin-toolbar-left">
          <input
            type="text"
            placeholder="Search screen schemes..."
            className="admin-search-input-toolbar"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="admin-toolbar-right">
          <button className="admin-btn-primary">Add Screen Scheme</button>
        </div>
      </div>

      <div className="admin-table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Scheme Name</th>
              <th>Description</th>
              <th>Associated Issue Types</th>
              <th>Screens</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>
                <div className="screen-cell">
                  <span className="screen-icon scheme">SC</span>
                  <span className="screen-name">Default Screen Scheme</span>
                </div>
              </td>
              <td className="description-cell">Used by default for all issues</td>
              <td>8 issue types</td>
              <td>Create, Edit, View</td>
              <td>
                <div className="action-buttons">
                  <button className="admin-btn-secondary">Edit</button>
                  <button className="admin-btn-secondary">Copy</button>
                </div>
              </td>
            </tr>
            <tr>
              <td>
                <div className="screen-cell">
                  <span className="screen-icon scheme">SC</span>
                  <span className="screen-name">Bug Screen Scheme</span>
                </div>
              </td>
              <td className="description-cell">Screens for bug reporting workflow</td>
              <td>Bug</td>
              <td>Create, Edit, View, Resolve</td>
              <td>
                <div className="action-buttons">
                  <button className="admin-btn-secondary">Edit</button>
                  <button className="admin-btn-secondary">Copy</button>
                </div>
              </td>
            </tr>
            <tr>
              <td>
                <div className="screen-cell">
                  <span className="screen-icon scheme">SC</span>
                  <span className="screen-name">Task Screen Scheme</span>
                </div>
              </td>
              <td className="description-cell">Screens for task management</td>
              <td>Task, Subtask</td>
              <td>Create, Edit, View</td>
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
          <h1 className="admin-page-title">Screens</h1>
          <p className="admin-page-description">
            Manage screens and screen schemes for issue operations.
          </p>
        </div>

        <div className="screens-tabs">
          <button
            className={`screens-tab ${activeTab === 'screens' ? 'active' : ''}`}
            onClick={() => setActiveTab('screens')}
          >
            Screens
          </button>
          <button
            className={`screens-tab ${activeTab === 'schemes' ? 'active' : ''}`}
            onClick={() => setActiveTab('schemes')}
          >
            Screen Schemes
          </button>
        </div>

        <div className="screens-content">
          {activeTab === 'screens' && renderScreensTab()}
          {activeTab === 'schemes' && renderSchemesTab()}
        </div>
      </div>
    </AdminLayout>
  );
}