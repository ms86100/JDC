import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AdminLayout from '../components/AdminLayout';
import { workflowApi } from '../../../api/workflowApi';
import WorkflowVersionHistoryPanel from '../../workflows/components/WorkflowVersionHistoryPanel';
import WorkflowStatusMigrationModal from '../../workflows/components/WorkflowStatusMigrationModal';
import './WorkflowsPage.css';

const API_BASE = '/api/workflows';
const SCHEMES_API_BASE = '/api/workflow-schemes';

interface WorkflowScheme {
  id: string;
  name: string;
  description?: string;
  isDefault?: boolean;
  mappingCount?: number;
}

interface Workflow {
  id: string;
  name: string;
  description: string;
  isDraft: boolean;
  isActive: boolean;
  isSystem: boolean;
  isDefault?: boolean;
  projectId?: string;
  draftOfWorkflowId?: string;
  createdAt?: string;
  updatedAt?: string;
  statusCount?: number;
  transitionCount?: number;
  schemes?: WorkflowScheme[];
}

interface WorkflowVersion {
  id: string;
  versionNumber: number;
  changeDescription: string;
  changeType: string;
  createdAt: string;
  createdBy?: string;
}

interface WorkflowStatus {
  id: string;
  name: string;
  description?: string;
  category?: string;
  color?: string;
  sequence?: number;
}

interface WorkflowTransition {
  id: string;
  name: string;
  description?: string;
  fromStatusId: string;
  toStatusId: string;
  fromStatusName?: string;
  toStatusName?: string;
  fromStatusColor?: string;
  toStatusColor?: string;
  displayOrder?: number;
  type?: string;
  conditions?: unknown[];
  validators?: unknown[];
  postFunctions?: unknown[];
}

export default function WorkflowsPage() {
  const navigate = useNavigate();
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [schemes, setSchemes] = useState<WorkflowScheme[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showImport, setShowImport] = useState(false);
  const [showAddWorkflow, setShowAddWorkflow] = useState(false);
  const [showAddScheme, setShowAddScheme] = useState(false);
  const [search, setSearch] = useState('');
  const [selectedWorkflow, setSelectedWorkflow] = useState<Workflow | null>(null);
  const [selectedScheme, setSelectedScheme] = useState<WorkflowScheme | null>(null);
  const [view, setView] = useState<'list' | 'workflow-detail' | 'scheme-detail'>('list');

  // Form states
  const [workflowForm, setWorkflowForm] = useState({ name: '', description: '' });
  const [schemeForm, setSchemeForm] = useState({ name: '', description: '' });

  const fetchWorkflows = useCallback(async () => {
    try {
      setLoading(true);
      const res = await workflowApi.getAll();
      // Ensure all required fields exist and description is a string
      const workflows: Workflow[] = Array.isArray(res.data)
        ? res.data.map(w => ({
            id: w.id,
            name: w.name,
            description: w.description || '',
            isDraft: w.isDraft,
            isActive: w.isActive,
            isSystem: w.isSystem,
            isDefault: w.isDefault,
            projectId: w.projectId,
            draftOfWorkflowId: w.draftOfWorkflowId,
            statusCount: w.statusCount,
            transitionCount: w.transitionCount,
            createdAt: w.createdAt,
            updatedAt: w.updatedAt,
          }))
        : [];
      setWorkflows(workflows);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch workflows');
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchSchemes = useCallback(async () => {
    try {
      const res = await workflowApi.getSchemes();
      setSchemes(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      console.error('Failed to fetch schemes:', err);
    }
  }, []);

  useEffect(() => {
    fetchWorkflows();
    fetchSchemes();
  }, [fetchWorkflows, fetchSchemes]);

  const handleCreateWorkflow = async () => {
    if (!workflowForm.name.trim()) return;
    try {
      const res = await fetch(`${API_BASE}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(workflowForm),
      });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.error || 'Failed to create workflow');
      }
      await fetchWorkflows();
      setShowAddWorkflow(false);
      setWorkflowForm({ name: '', description: '' });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create workflow');
    }
  };

  const handleDeleteWorkflow = async (id: string) => {
    if (!confirm('Are you sure you want to delete this workflow?')) return;
    try {
      const res = await fetch(`${API_BASE}/${id}`, { method: 'DELETE' });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.error || 'Failed to delete workflow');
      }
      await fetchWorkflows();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete workflow');
    }
  };

  const handleCloneWorkflow = async (id: string, newName: string) => {
    try {
      const res = await fetch(`${API_BASE}/${id}/clone`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ newName }),
      });
      if (!res.ok) throw new Error('Failed to clone workflow');
      await fetchWorkflows();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to clone workflow');
    }
  };

  const handlePublishWorkflow = async (id: string) => {
    try {
      const res = await fetch(`${API_BASE}/${id}/publish`, { method: 'POST' });
      if (!res.ok) throw new Error('Failed to publish workflow');
      await fetchWorkflows();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to publish workflow');
    }
  };

  const handleCreateScheme = async () => {
    if (!schemeForm.name.trim()) return;
    try {
      const res = await fetch(SCHEMES_API_BASE, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(schemeForm),
      });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.error || 'Failed to create scheme');
      }
      await fetchSchemes();
      setShowAddScheme(false);
      setSchemeForm({ name: '', description: '' });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create scheme');
    }
  };

  const handleDeleteScheme = async (id: string) => {
    if (!confirm('Are you sure you want to delete this scheme?')) return;
    try {
      const res = await fetch(`${SCHEMES_API_BASE}/${id}`, { method: 'DELETE' });
      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.error || 'Failed to delete scheme');
      }
      await fetchSchemes();
      if (selectedScheme?.id === id) {
        setSelectedScheme(null);
        setView('list');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete scheme');
    }
  };

  const activeWorkflows = workflows.filter(w => w.isActive && !w.isDraft);
  const inactiveWorkflows = workflows.filter(w => !w.isActive || w.isDraft);

  const filtered = (list: Workflow[]) =>
    list.filter(w =>
      w.name.toLowerCase().includes(search.toLowerCase()) ||
      (w.description || '').toLowerCase().includes(search.toLowerCase())
    );

  if (loading) {
    return (
      <AdminLayout>
      <div className="wf-page">
        <div className="wf-loading">Loading workflows...</div>
      </div>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout>
    <div className="wf-page">
        {view === 'list' && (
          <>
            {/* Page Header */}
            <div className="wf-page-header">
              <div className="wf-page-header-left">
                <h1 className="wf-page-title">Workflows</h1>
              </div>
              <div className="wf-page-header-right">
                <Link to="/workflows/admin/tools" className="wf-btn wf-btn-secondary" style={{ marginRight: 8 }}>
                  Admin API tools
                </Link>
                <Link to="/workflows" className="wf-btn wf-btn-secondary" style={{ marginRight: 8 }}>
                  Workflow hub
                </Link>
                <div className="wf-import-wrapper">
                  <button
                    className="wf-btn wf-btn-secondary"
                    onClick={() => setShowImport(!showImport)}
                  >
                    Import <span className="wf-caret">▾</span>
                  </button>
                  {showImport && (
                    <div className="wf-dropdown-menu">
                      <button
                        type="button"
                        className="wf-dropdown-item"
                        onClick={() => {
                          setShowImport(false);
                          navigate('/migration?import=workflow-xml');
                        }}
                      >
                        Import Workflow Definition
                      </button>
                      <button
                        type="button"
                        className="wf-dropdown-item"
                        onClick={() => {
                          setShowImport(false);
                          navigate('/migration?import=workflow-xml');
                        }}
                      >
                        Import from XML
                      </button>
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

            {/* Add Workflow Modal */}
            {showAddWorkflow && (
              <div className="wf-modal-overlay" onClick={() => setShowAddWorkflow(false)}>
                <div className="wf-modal" onClick={e => e.stopPropagation()}>
                  <h2 className="wf-modal-title">Add Workflow</h2>
                  <div className="wf-form-group">
                    <label>Name *</label>
                    <input
                      type="text"
                      className="wf-input"
                      value={workflowForm.name}
                      onChange={e => setWorkflowForm({ ...workflowForm, name: e.target.value })}
                      placeholder="Enter workflow name"
                    />
                  </div>
                  <div className="wf-form-group">
                    <label>Description</label>
                    <textarea
                      className="wf-textarea"
                      value={workflowForm.description}
                      onChange={e => setWorkflowForm({ ...workflowForm, description: e.target.value })}
                      placeholder="Enter description"
                    />
                  </div>
                  <div className="wf-modal-actions">
                    <button className="wf-btn wf-btn-secondary" onClick={() => setShowAddWorkflow(false)}>
                      Cancel
                    </button>
                    <button className="wf-btn wf-btn-primary" onClick={handleCreateWorkflow}>
                      Add
                    </button>
                  </div>
                </div>
              </div>
            )}

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

            {/* Error Banner */}
            {error && (
              <div className="wf-error-banner">
                <span>{error}</span>
                <button onClick={() => setError(null)}>×</button>
              </div>
            )}

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
                            <a
                              href="#"
                              className="wf-workflow-link"
                              onClick={(e) => { e.preventDefault(); setSelectedWorkflow(wf); setView('workflow-detail'); }}
                            >
                              {wf.name}
                            </a>
                            {wf.isSystem && <span className="wf-system-badge">SYSTEM</span>}
                            <span className="wf-workflow-desc">{wf.description}</span>
                          </td>
                          <td className="wf-col-modified">
                            <span className="wf-date">{wf.updatedAt || 'N/A'}</span>
                          </td>
                          <td className="wf-col-schemes">
                            {wf.schemes && wf.schemes.length > 0 ? (
                              <ul className="wf-scheme-list">
                                {wf.schemes.map((s) => (
                                  <li key={s.id} className="wf-scheme-item">{s.name}</li>
                                ))}
                              </ul>
                            ) : (
                              <span className="wf-no-scheme">-</span>
                            )}
                          </td>
                          <td className="wf-col-steps">{wf.statusCount || 0}</td>
                          <td className="wf-col-actions">
                            <div className="wf-action-group">
                              <button
                                className="wf-action-btn"
                                onClick={() => { setSelectedWorkflow(wf); setView('workflow-detail'); }}
                              >
                                Edit
                              </button>
                              <button
                                className="wf-action-btn"
                                onClick={() => navigate(`/admin/workflows/${wf.id}/designer`)}
                              >
                                Diagram
                              </button>
                              <button
                                className="wf-action-btn wf-action-copy"
                                onClick={() => {
                                  const newName = prompt('Enter new workflow name:', wf.name + ' (Copy)');
                                  if (newName) handleCloneWorkflow(wf.id, newName);
                                }}
                              >
                                Copy
                              </button>
                              {!wf.isSystem && (
                                <button
                                  className="wf-action-btn wf-action-delete"
                                  onClick={() => handleDeleteWorkflow(wf.id)}
                                >
                                  Delete
                                </button>
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
                            <a
                              href="#"
                              className="wf-workflow-link"
                              onClick={(e) => { e.preventDefault(); setSelectedWorkflow(wf); setView('workflow-detail'); }}
                            >
                              {wf.name}
                            </a>
                            {wf.isDraft && <span className="wf-draft-badge">DRAFT</span>}
                            {wf.isSystem && <span className="wf-system-badge">SYSTEM</span>}
                            <span className="wf-workflow-desc">{wf.description}</span>
                          </td>
                          <td className="wf-col-modified">
                            <span className="wf-date">{wf.updatedAt || 'N/A'}</span>
                          </td>
                          <td className="wf-col-schemes">
                            {wf.schemes && wf.schemes.length > 0 ? (
                              <ul className="wf-scheme-list">
                                {wf.schemes.map((s) => (
                                  <li key={s.id} className="wf-scheme-item">{s.name}</li>
                                ))}
                              </ul>
                            ) : (
                              <span className="wf-no-scheme">-</span>
                            )}
                          </td>
                          <td className="wf-col-steps">{wf.statusCount || 0}</td>
                          <td className="wf-col-actions">
                            <div className="wf-action-group">
                              <button
                                className="wf-action-btn"
                                onClick={() => { setSelectedWorkflow(wf); setView('workflow-detail'); }}
                              >
                                Edit
                              </button>
                              <button
                                className="wf-action-btn"
                                onClick={() => navigate(`/admin/workflows/${wf.id}/designer`)}
                              >
                                Diagram
                              </button>
                              <button
                                className="wf-action-btn wf-action-copy"
                                onClick={() => {
                                  const newName = prompt('Enter new workflow name:', wf.name + ' (Copy)');
                                  if (newName) handleCloneWorkflow(wf.id, newName);
                                }}
                              >
                                Copy
                              </button>
                              {!wf.isSystem && (
                                <button
                                  className="wf-action-btn wf-action-delete"
                                  onClick={() => handleDeleteWorkflow(wf.id)}
                                >
                                  Delete
                                </button>
                              )}
                              {wf.isDraft && (
                                <button
                                  className="wf-action-btn wf-action-publish"
                                  onClick={() => handlePublishWorkflow(wf.id)}
                                >
                                  Publish
                                </button>
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

            {/* Workflow Schemes Section */}
            <div className="wf-section wf-section-schemes">
              <div className="wf-section-header-row">
                <h2 className="wf-section-header">Workflow Schemes</h2>
                <button className="wf-btn wf-btn-primary" onClick={() => setShowAddScheme(true)}>
                  Add scheme
                </button>
              </div>

              {/* Add Scheme Modal */}
              {showAddScheme && (
                <div className="wf-modal-overlay" onClick={() => setShowAddScheme(false)}>
                  <div className="wf-modal" onClick={e => e.stopPropagation()}>
                    <h2 className="wf-modal-title">Add Workflow Scheme</h2>
                    <div className="wf-form-group">
                      <label>Name *</label>
                      <input
                        type="text"
                        className="wf-input"
                        value={schemeForm.name}
                        onChange={e => setSchemeForm({ ...schemeForm, name: e.target.value })}
                        placeholder="Enter scheme name"
                      />
                    </div>
                    <div className="wf-form-group">
                      <label>Description</label>
                      <textarea
                        className="wf-textarea"
                        value={schemeForm.description}
                        onChange={e => setSchemeForm({ ...schemeForm, description: e.target.value })}
                        placeholder="Enter description"
                      />
                    </div>
                    <div className="wf-modal-actions">
                      <button className="wf-btn wf-btn-secondary" onClick={() => setShowAddScheme(false)}>
                        Cancel
                      </button>
                      <button className="wf-btn wf-btn-primary" onClick={handleCreateScheme}>
                        Add
                      </button>
                    </div>
                  </div>
                </div>
              )}

              <div className="wf-card">
                <table className="wf-table">
                  <thead>
                    <tr>
                      <th className="wf-col-name">Name</th>
                      <th className="wf-col-description">Description</th>
                      <th className="wf-col-modified">Modified</th>
                      <th className="wf-col-steps">Issue Types</th>
                      <th className="wf-col-actions">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {schemes.length === 0 ? (
                      <tr>
                        <td colSpan={5} className="wf-empty">No workflow schemes found.</td>
                      </tr>
                    ) : (
                      schemes.map((scheme) => (
                        <tr key={scheme.id} className="wf-row">
                          <td className="wf-col-name">
                            <a
                              href="#"
                              className="wf-workflow-link"
                              onClick={(e) => { e.preventDefault(); setSelectedScheme(scheme); setView('scheme-detail'); }}
                            >
                              {scheme.name}
                            </a>
                            {scheme.isDefault && <span className="wf-default-badge">DEFAULT</span>}
                          </td>
                          <td className="wf-col-description">{scheme.description || '-'}</td>
                          <td className="wf-col-modified">
                            <span className="wf-date">-</span>
                          </td>
                          <td className="wf-col-steps">{scheme.mappingCount || 0}</td>
                          <td className="wf-col-actions">
                            <div className="wf-action-group">
                              <button
                                className="wf-action-btn"
                                onClick={() => { setSelectedScheme(scheme); setView('scheme-detail'); }}
                              >
                                Edit
                              </button>
                              {!scheme.isDefault && (
                                <button
                                  className="wf-action-btn wf-action-delete"
                                  onClick={() => handleDeleteScheme(scheme.id)}
                                >
                                  Delete
                                </button>
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
          </>
        )}

        {view === 'workflow-detail' && selectedWorkflow && (
          <WorkflowDetailView
            workflow={selectedWorkflow}
            workflows={workflows}
            onBack={() => { setView('list'); setSelectedWorkflow(null); }}
            onRefresh={fetchWorkflows}
          />
        )}

        {view === 'scheme-detail' && selectedScheme && (
          <SchemeDetailView
            scheme={selectedScheme}
            workflows={workflows}
            onBack={() => { setView('list'); setSelectedScheme(null); }}
            onRefresh={fetchSchemes}
          />
        )}
      </div>
    </AdminLayout>
  );
}

// ==================== Workflow Detail View ====================

interface WorkflowDetailViewProps {
  workflow: Workflow;
  workflows: Workflow[];
  onBack: () => void;
  onRefresh: () => void;
}

function WorkflowDetailView({ workflow, workflows, onBack }: WorkflowDetailViewProps) {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<'statuses' | 'transitions' | 'versions'>('statuses');
  const [showStatusMigration, setShowStatusMigration] = useState(false);
  const [statuses, setStatuses] = useState<WorkflowStatus[]>([]);
  const [transitions, setTransitions] = useState<WorkflowTransition[]>([]);
  const [versions, setVersions] = useState<WorkflowVersion[]>([]);
  const [loading, setLoading] = useState(false);
  const [detailError, setDetailError] = useState<string | null>(null);

  useEffect(() => {
    fetchWorkflowDetails();
  }, [workflow.id]);

  const fetchWorkflowDetails = async () => {
    setLoading(true);
    setDetailError(null);
    try {
      const res = await workflowApi.getWorkflowDetail(workflow.id);
      const detail = res.data;

      setStatuses(
        (detail.statuses || []).map((s) => ({
          id: s.statusId || s.id,
          linkId: s.id,
          name: s.statusName || String(s.statusId),
          category: s.statusCategory,
          color: s.statusColor,
          sequence: s.sequence,
        }))
      );
      setTransitions(
        (detail.transitions || []).map((t) => ({
          id: t.id,
          name: t.name,
          description: t.description,
          fromStatusId: t.fromStatusId,
          toStatusId: t.toStatusId,
          fromStatusName: t.fromStatusName,
          toStatusName: t.toStatusName,
          fromStatusColor: t.fromStatusColor,
          toStatusColor: t.toStatusColor,
          displayOrder: t.displayOrder,
          type: t.type,
          conditions: t.conditions,
          validators: t.validators,
          postFunctions: t.postFunctions,
        }))
      );
      setVersions(Array.isArray(detail.versions) ? detail.versions : []);
    } catch (err) {
      console.error('Failed to fetch workflow details:', err);
      setDetailError('Could not load workflow details. Check that workflow-service and issue-service are running.');
    } finally {
      setLoading(false);
    }
  };

  const categoryLabel = (cat?: string) => {
    if (!cat) return 'To Do';
    if (cat === 'IN_PROGRESS') return 'In Progress';
    if (cat === 'DONE') return 'Done';
    return 'To Do';
  };

  return (
    <div className="wf-detail-page">
      <div className="wf-detail-header">
        <button className="wf-back-btn" onClick={onBack}>← Back to Workflows</button>
        <div className="wf-detail-header-row">
          <div>
            <h1 className="wf-detail-title">
              {workflow.name}
              {workflow.isSystem && <span className="wf-system-badge">SYSTEM</span>}
              {workflow.isDraft && <span className="wf-draft-badge">DRAFT</span>}
            </h1>
            <p className="wf-detail-description">{workflow.description}</p>
          </div>
          <div className="wf-detail-actions">
            <button type="button" className="wf-btn wf-btn-secondary" onClick={() => setShowStatusMigration(true)}>
              Status migration
            </button>
            <button
              type="button"
              className="wf-btn wf-btn-primary"
              onClick={() => navigate(`/workflows/${workflow.id}/designer`)}
            >
              Open diagram editor
            </button>
          </div>
        </div>
        <div className="wf-detail-summary">
          <span className="wf-summary-pill">{statuses.length} steps</span>
          <span className="wf-summary-pill">{transitions.length} transitions</span>
          <span className="wf-summary-pill">{versions.length} versions</span>
        </div>
        {detailError && <p className="wf-detail-error">{detailError}</p>}
      </div>

      <div className="wf-detail-tabs">
        <button
          className={`wf-tab ${activeTab === 'statuses' ? 'wf-tab-active' : ''}`}
          onClick={() => setActiveTab('statuses')}
        >
          Statuses ({statuses.length})
        </button>
        <button
          className={`wf-tab ${activeTab === 'transitions' ? 'wf-tab-active' : ''}`}
          onClick={() => setActiveTab('transitions')}
        >
          Transitions ({transitions.length})
        </button>
        <button
          className={`wf-tab ${activeTab === 'versions' ? 'wf-tab-active' : ''}`}
          onClick={() => setActiveTab('versions')}
        >
          Versions ({versions.length})
        </button>
      </div>

      <div className="wf-detail-content">
        {loading ? (
          <div className="wf-loading">Loading...</div>
        ) : (
          <>
            {activeTab === 'statuses' && (
              <div className="wf-statuses-list">
                {statuses.length === 0 ? (
                  <p className="wf-empty">No statuses defined.</p>
                ) : (
                  statuses.map((status, index) => (
                    <div key={status.id} className="wf-status-card">
                      <span className="wf-status-seq">{index + 1}</span>
                      <div className="wf-status-color" style={{ backgroundColor: status.color || '#6C757D' }} />
                      <div className="wf-status-info">
                        <strong>{status.name}</strong>
                        <span className={`wf-status-category wf-cat-${(status.category || 'TODO').toLowerCase()}`}>
                          {categoryLabel(status.category)}
                        </span>
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}

            {activeTab === 'transitions' && (
              <div className="wf-transitions-list">
                {transitions.length === 0 ? (
                  <p className="wf-empty">No transitions defined.</p>
                ) : (
                  transitions.map((transition) => (
                    <div key={transition.id} className="wf-transition-card">
                      <strong className="wf-transition-name">{transition.name}</strong>
                      <div className="wf-transition-flow">
                        <span
                          className="wf-flow-status"
                          style={{ borderColor: transition.fromStatusColor || '#6C757D' }}
                        >
                          {transition.fromStatusName || transition.fromStatusId}
                        </span>
                        <span className="wf-flow-arrow" aria-hidden="true">→</span>
                        <span
                          className="wf-flow-status"
                          style={{ borderColor: transition.toStatusColor || '#6C757D' }}
                        >
                          {transition.toStatusName || transition.toStatusId}
                        </span>
                      </div>
                      {(transition.conditions?.length || transition.validators?.length || transition.postFunctions?.length) ? (
                        <div className="wf-transition-meta">
                          {transition.conditions?.length ? <span>{transition.conditions.length} conditions</span> : null}
                          {transition.validators?.length ? <span>{transition.validators.length} validators</span> : null}
                          {transition.postFunctions?.length ? <span>{transition.postFunctions.length} post-functions</span> : null}
                        </div>
                      ) : null}
                    </div>
                  ))
                )}
              </div>
            )}

            {activeTab === 'versions' && (
              <WorkflowVersionHistoryPanel
                workflowId={workflow.id}
                versions={versions.map((v) => ({
                  id: v.id,
                  versionNumber: v.versionNumber,
                  changeDescription: v.changeDescription,
                  changeType: v.changeType,
                  createdAt: v.createdAt,
                  createdBy: v.createdBy,
                }))}
              />
            )}
          </>
        )}
      </div>

      {showStatusMigration && (
        <WorkflowStatusMigrationModal
          workflowId={workflow.id}
          statuses={statuses.map((s) => ({
            id: s.id,
            workflowId: workflow.id,
            statusId: s.id,
            statusName: s.name,
            statusCategory: s.category,
            statusColor: s.color,
            sequence: s.sequence ?? 0,
          }))}
          onClose={() => setShowStatusMigration(false)}
        />
      )}
    </div>
  );
}

// ==================== Scheme Detail View ====================

interface SchemeDetailViewProps {
  scheme: WorkflowScheme;
  workflows: Workflow[];
  onBack: () => void;
  onRefresh: () => void;
}

function SchemeDetailView({ scheme, workflows, onBack, onRefresh }: SchemeDetailViewProps) {
  const [mappings, setMappings] = useState<{ issueTypeId: string; workflowId: string }[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchSchemeMappings();
  }, [scheme.id]);

  const fetchSchemeMappings = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${SCHEMES_API_BASE}/${scheme.id}`);
      if (res.ok) {
        const data = await res.json();
        const raw = data.mappings || [];
        setMappings(
          raw.map((m: { issueTypeId: string; workflowId: string }) => ({
            issueTypeId: String(m.issueTypeId),
            workflowId: String(m.workflowId),
          }))
        );
      }
    } catch (err) {
      console.error('Failed to fetch scheme mappings:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleAssignWorkflow = async (issueTypeId: string, workflowId: string) => {
    try {
      const res = await fetch(`${SCHEMES_API_BASE}/${scheme.id}/mappings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ issueTypeId, workflowId }),
      });
      if (res.ok) {
        await fetchSchemeMappings();
        onRefresh();
      }
    } catch (err) {
      console.error('Failed to assign workflow:', err);
    }
  };

  return (
    <div className="wf-detail-page">
      <div className="wf-detail-header">
        <button className="wf-back-btn" onClick={onBack}>← Back to Workflows</button>
        <h1 className="wf-detail-title">
          {scheme.name}
          {scheme.isDefault && <span className="wf-default-badge">DEFAULT</span>}
        </h1>
        <p className="wf-detail-description">{scheme.description}</p>
      </div>

      <div className="wf-mappings-section">
        <h2 className="wf-section-header">Issue Type Mappings</h2>
        <div className="wf-card">
          {loading ? (
            <div className="wf-loading">Loading...</div>
          ) : mappings.length === 0 ? (
            <p className="wf-empty">No issue type mappings configured.</p>
          ) : (
            <table className="wf-table">
              <thead>
                <tr>
                  <th>Issue Type</th>
                  <th>Workflow</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {mappings.map((mapping, idx) => {
                  const workflow = workflows.find(w => w.id === mapping.workflowId);
                  return (
                    <tr key={idx}>
                      <td>{mapping.issueTypeId}</td>
                      <td>{workflow?.name || 'Unknown'}</td>
                      <td>
                        <button className="wf-btn wf-btn-secondary wf-btn-sm">Change</button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}