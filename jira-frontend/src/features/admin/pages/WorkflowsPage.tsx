import React, { useState } from 'react';
import AdminLayout from '../components/AdminLayout';
import './WorkflowsPage.css';

interface WorkflowScheme {
  id: string;
  name: string;
}

interface Workflow {
  id: string;
  name: string;
  description: string;
  isDraft: boolean;
  isActive: boolean;
  isSystem: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
  updatedBy: string;
  updatedByName: string;
  stepCount: number;
  schemes: WorkflowScheme[];
}

// Mock data matching DC screenshot
const MOCK_WORKFLOWS: Workflow[] = [
  {
    id: '1',
    name: 'jira',
    description: 'Jira Workflow',
    isDraft: false,
    isActive: true,
    isSystem: true,
    version: 1,
    createdAt: '11/Dec/25',
    updatedAt: '11/Dec/25',
    updatedBy: 'uuid-1',
    updatedByName: 'Sagar Sharma',
    stepCount: 5,
    schemes: [
      { id: '1', name: 'Jira Service Management Workflow Scheme' },
    ],
  },
  {
    id: '2',
    name: 'Bug Workflow',
    description: 'Software Simplified',
    isDraft: false,
    isActive: true,
    isSystem: false,
    version: 1,
    createdAt: '11/Dec/25',
    updatedAt: '11/Dec/25',
    updatedBy: 'uuid-1',
    updatedByName: 'Sagar Sharma',
    stepCount: 5,
    schemes: [
      { id: '2', name: 'Bug Workflow Scheme' },
    ],
  },
  {
    id: '3',
    name: 'Kanban Workflow',
    description: 'Software Simplified',
    isDraft: false,
    isActive: true,
    isSystem: false,
    version: 1,
    createdAt: '11/Dec/25',
    updatedAt: '11/Dec/25',
    updatedBy: 'uuid-1',
    updatedByName: 'Sagar Sharma',
    stepCount: 4,
    schemes: [
      { id: '3', name: 'Kanban Workflow Scheme' },
    ],
  },
  {
    id: '4',
    name: 'Test Workflow',
    description: 'Software Simplified',
    isDraft: false,
    isActive: true,
    isSystem: false,
    version: 1,
    createdAt: '11/Dec/25',
    updatedAt: '11/Dec/25',
    updatedBy: 'uuid-1',
    updatedByName: 'Sagar Sharma',
    stepCount: 3,
    schemes: [],
  },
  {
    id: '5',
    name: 'Scrum Workflow',
    description: 'Scrum Simplified',
    isDraft: true,
    isActive: true,
    isSystem: false,
    version: 2,
    createdAt: '11/Dec/25',
    updatedAt: '11/Dec/25',
    updatedBy: 'uuid-1',
    updatedByName: 'Sagar Sharma',
    stepCount: 5,
    schemes: [],
  },
];

export default function WorkflowsPage() {
  const [showImport, setShowImport] = useState(false);
  const [showAddWorkflow, setShowAddWorkflow] = useState(false);
  const [search, setSearch] = useState('');

  const activeWorkflows = MOCK_WORKFLOWS.filter(w => w.isActive && !w.isDraft);
  const inactiveWorkflows = MOCK_WORKFLOWS.filter(w => !w.isActive || w.isDraft);

  const filtered = (list: Workflow[]) =>
    list.filter(w =>
      w.name.toLowerCase().includes(search.toLowerCase()) ||
      w.description.toLowerCase().includes(search.toLowerCase())
    );

  return (
    <AdminLayout>
      <div className="wf-page">
        {/* Page Header */}
        <div className="wf-page-header">
          <div className="wf-page-header-left">
            <h1 className="wf-page-title">Workflows</h1>
          </div>
          <div className="wf-page-header-right">
            <div className="wf-import-wrapper">
              <button
                className="wf-btn wf-btn-secondary"
                onClick={() => setShowImport(!showImport)}
              >
                Import <span className="wf-caret">▾</span>
              </button>
              {showImport && (
                <div className="wf-dropdown-menu">
                  <button className="wf-dropdown-item">Import Workflow Definition</button>
                  <button className="wf-dropdown-item">Import from XML</button>
                </div>
              )}
            </div>
            <button
              className="wf-btn wf-btn-primary"
              onClick={() => setShowAddWorkflow(true)}
            >
              Add workflow
            </button>
          </div>
        </div>

        {/* Toolbar */}
        <div className="wf-toolbar">
          <div className="wf-toolbar-left">
            <div className="wf-search-box">
              <span className="wf-search-icon">🔍</span>
              <input
                type="text"
                placeholder="Search workflows..."
                className="wf-search-input"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
              {search && (
                <button className="wf-search-clear" onClick={() => setSearch('')}>×</button>
              )}
            </div>
          </div>
        </div>

        {/* Active Workflows */}
        <div className="wf-section">
          <h2 className="wf-section-header">
            Active ({filtered(activeWorkflows).length})
          </h2>
          <div className="wf-card">
            <table className="wf-table">
              <thead>
                <tr>
                  <th className="wf-col-name">Name</th>
                  <th className="wf-col-modified">Last modified</th>
                  <th className="wf-col-schemes">Assigned schemes</th>
                  <th className="wf-col-steps">Steps</th>
                  <th className="wf-col-actions">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered(activeWorkflows).length === 0 ? (
                  <tr>
                    <td colSpan={5} className="wf-empty">No active workflows match your search.</td>
                  </tr>
                ) : (
                  filtered(activeWorkflows).map((wf) => (
                    <tr key={wf.id} className="wf-row">
                      <td className="wf-col-name">
                        <a href="#" className="wf-workflow-link">{wf.name}</a>
                        <span className="wf-workflow-desc">{wf.description}</span>
                      </td>
                      <td className="wf-col-modified">
                        <a href="#" className="wf-user-link">{wf.updatedByName}</a>
                        <span className="wf-date">{wf.updatedAt}</span>
                      </td>
                      <td className="wf-col-schemes">
                        {wf.schemes.length === 0 ? (
                          <span className="wf-no-scheme">-</span>
                        ) : (
                          <ul className="wf-scheme-list">
                            {wf.schemes.map((s) => (
                              <li key={s.id} className="wf-scheme-item">{s.name}</li>
                            ))}
                          </ul>
                        )}
                      </td>
                      <td className="wf-col-steps">{wf.stepCount}</td>
                      <td className="wf-col-actions">
                        <div className="wf-action-group">
                          <button className="wf-action-btn">Edit</button>
                          <button className="wf-action-btn">Diagram</button>
                          <button className="wf-action-btn wf-action-copy">Copy</button>
                          {!wf.isSystem && (
                            <button className="wf-action-btn wf-action-delete">Delete</button>
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

        {/* Inactive Workflows */}
        {filtered(inactiveWorkflows).length > 0 && (
          <div className="wf-section">
            <h2 className="wf-section-header">
              Inactive ({filtered(inactiveWorkflows).length})
            </h2>
            <div className="wf-card">
              <table className="wf-table">
                <thead>
                  <tr>
                    <th className="wf-col-name">Name</th>
                    <th className="wf-col-modified">Last modified</th>
                    <th className="wf-col-schemes">Assigned schemes</th>
                    <th className="wf-col-steps">Steps</th>
                    <th className="wf-col-actions">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered(inactiveWorkflows).map((wf) => (
                    <tr key={wf.id} className="wf-row wf-row-inactive">
                      <td className="wf-col-name">
                        <a href="#" className="wf-workflow-link">{wf.name}</a>
                        {wf.isDraft && <span className="wf-draft-badge">DRAFT</span>}
                        <span className="wf-workflow-desc">{wf.description}</span>
                      </td>
                      <td className="wf-col-modified">
                        <a href="#" className="wf-user-link">{wf.updatedByName}</a>
                        <span className="wf-date">{wf.updatedAt}</span>
                      </td>
                      <td className="wf-col-schemes">
                        {wf.schemes.length === 0 ? (
                          <span className="wf-no-scheme">-</span>
                        ) : (
                          <ul className="wf-scheme-list">
                            {wf.schemes.map((s) => (
                              <li key={s.id} className="wf-scheme-item">{s.name}</li>
                            ))}
                          </ul>
                        )}
                      </td>
                      <td className="wf-col-steps">{wf.stepCount}</td>
                      <td className="wf-col-actions">
                        <div className="wf-action-group">
                          <button className="wf-action-btn">Edit</button>
                          <button className="wf-action-btn">Diagram</button>
                          <button className="wf-action-btn wf-action-copy">Copy</button>
                          {!wf.isSystem && (
                            <button className="wf-action-btn wf-action-delete">Delete</button>
                          )}
                          {wf.isDraft && (
                            <button className="wf-action-btn wf-action-publish">Publish</button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  );
}