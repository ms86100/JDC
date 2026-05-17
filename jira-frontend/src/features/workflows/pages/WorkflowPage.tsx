import { useState, useEffect, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import { workflowApi, Workflow, WorkflowStatus, WorkflowTransition } from '../../../api/workflowApi';

const API_BASE = '/api/workflows';

export default function WorkflowPage() {
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showAddWorkflow, setShowAddWorkflow] = useState(false);
  const [search, setSearch] = useState('');
  const [selectedWorkflow, setSelectedWorkflow] = useState<Workflow | null>(null);
  const [view, setView] = useState<'list' | 'workflow-detail'>('list');
  const [activeTab, setActiveTab] = useState<'workflows' | 'statuses'>('workflows');
  const [workflowForm, setWorkflowForm] = useState({ name: '', description: '' });

  const fetchWorkflows = useCallback(async () => {
    try {
      setLoading(true);
      const res = await workflowApi.getAll();
      setWorkflows(Array.isArray(res.data) ? res.data : []);
    } catch (err: any) {
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchWorkflows();
  }, [fetchWorkflows]);

  const handleCreateWorkflow = async () => {
    if (!workflowForm.name.trim()) return;
    try {
      await workflowApi.create({
        ...workflowForm,
        projectId: '00000000-0000-0000-0000-000000000001'
      });
      await fetchWorkflows();
      setShowAddWorkflow(false);
      setWorkflowForm({ name: '', description: '' });
    } catch (err: any) {
      setError(err instanceof Error ? err.message : 'Failed to create workflow');
    }
  };

  const handleDeleteWorkflow = async (id: string) => {
    if (!confirm('Are you sure you want to delete this workflow?')) return;
    try {
      await workflowApi.delete(id);
      await fetchWorkflows();
    } catch (err: any) {
      setError(err instanceof Error ? err.message : 'Failed to delete workflow');
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
      <div className="ab-workflow-page">
        <div className="ab-loading">
          <div className="ab-spinner"></div>
          <p>Loading workflows...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="ab-workflow-page">
      {view === 'list' && (
        <>
          <div className="ab-page-header">
            <div>
              <h1 className="ab-page-title">Workflows</h1>
              <p className="ab-page-subtitle">Manage workflows and status transitions</p>
            </div>
            <button
              className="ab-btn ab-btn-primary"
              onClick={() => setShowAddWorkflow(true)}
            >
              <span>+</span> Create Workflow
            </button>
          </div>

          {showAddWorkflow && (
            <div className="ab-modal-overlay" onClick={() => setShowAddWorkflow(false)}>
              <div className="ab-modal" onClick={e => e.stopPropagation()}>
                <h2 className="ab-modal-title">Create Workflow</h2>
                <div className="ab-form-group">
                  <label>Name *</label>
                  <input
                    type="text"
                    className="ab-input"
                    value={workflowForm.name}
                    onChange={e => setWorkflowForm({ ...workflowForm, name: e.target.value })}
                    placeholder="Enter workflow name"
                  />
                </div>
                <div className="ab-form-group">
                  <label>Description</label>
                  <textarea
                    className="ab-textarea"
                    value={workflowForm.description}
                    onChange={e => setWorkflowForm({ ...workflowForm, description: e.target.value })}
                    placeholder="Enter description"
                  />
                </div>
                <div className="ab-modal-actions">
                  <button className="ab-btn ab-btn-secondary" onClick={() => setShowAddWorkflow(false)}>
                    Cancel
                  </button>
                  <button className="ab-btn ab-btn-primary" onClick={handleCreateWorkflow}>
                    Create
                  </button>
                </div>
              </div>
            </div>
          )}

          <div className="ab-tabs">
            <button
              className={`ab-tab ${activeTab === 'workflows' ? 'active' : ''}`}
              onClick={() => {}}
            >
              Workflows
            </button>
            <button
              className={`ab-tab ${activeTab === 'statuses' ? 'active' : ''}`}
              onClick={() => {}}
            >
              Issue Statuses
            </button>
          </div>

          {error && (
            <div className="ab-error-banner">
              <span>{error}</span>
              <button onClick={() => setError(null)}>×</button>
            </div>
          )}

          <div className="ab-toolbar">
            <div className="ab-search-box">
              <span className="ab-search-icon">🔍</span>
              <input
                type="text"
                placeholder="Search workflows..."
                className="ab-search-input"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
              {search && (
                <button className="ab-search-clear" onClick={() => setSearch('')}>×</button>
              )}
            </div>
          </div>

          {filtered(activeWorkflows).length > 0 && (
            <div className="ab-section">
              <h2 className="ab-section-header">Active ({filtered(activeWorkflows).length})</h2>
              <div className="ab-card">
                <table className="ab-table">
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Project</th>
                      <th>Default</th>
                      <th>Created</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filtered(activeWorkflows).map((wf) => (
                      <tr key={wf.id}>
                        <td>
                          <span className="ab-workflow-name">{wf.name}</span>
                          {wf.isSystem && <span className="ab-badge ab-badge-system">SYSTEM</span>}
                          {wf.isDraft && <span className="ab-badge ab-badge-draft">DRAFT</span>}
                          <span className="ab-workflow-desc">{wf.description}</span>
                        </td>
                        <td>{wf.projectId || '-'}</td>
                        <td>{wf.isDefault ? 'Yes' : 'No'}</td>
                        <td>{wf.createdAt ? new Date(wf.createdAt).toLocaleDateString() : '-'}</td>
                        <td>
                          <button
                            className="ab-btn ab-btn-ghost ab-btn-sm"
                            onClick={() => { setSelectedWorkflow(wf); setView('workflow-detail'); }}
                          >
                            Edit
                          </button>
                          {!wf.isSystem && (
                            <button
                              className="ab-btn ab-btn-ghost ab-btn-sm ab-btn-danger"
                              onClick={() => handleDeleteWorkflow(wf.id)}
                            >
                              Delete
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {filtered(inactiveWorkflows).length > 0 && (
            <div className="ab-section">
              <h2 className="ab-section-header">Inactive ({filtered(inactiveWorkflows).length})</h2>
              <div className="ab-card">
                <table className="ab-table">
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Project</th>
                      <th>Default</th>
                      <th>Created</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filtered(inactiveWorkflows).map((wf) => (
                      <tr key={wf.id}>
                        <td>
                          <span className="ab-workflow-name">{wf.name}</span>
                          {wf.isDraft && <span className="ab-badge ab-badge-draft">DRAFT</span>}
                          <span className="ab-workflow-desc">{wf.description}</span>
                        </td>
                        <td>{wf.projectId || '-'}</td>
                        <td>{wf.isDefault ? 'Yes' : 'No'}</td>
                        <td>{wf.createdAt ? new Date(wf.createdAt).toLocaleDateString() : '-'}</td>
                        <td>
                          <button
                            className="ab-btn ab-btn-ghost ab-btn-sm"
                            onClick={() => { setSelectedWorkflow(wf); setView('workflow-detail'); }}
                          >
                            Edit
                          </button>
                          {!wf.isSystem && (
                            <button
                              className="ab-btn ab-btn-ghost ab-btn-sm ab-btn-danger"
                              onClick={() => handleDeleteWorkflow(wf.id)}
                            >
                              Delete
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </>
      )}

      {view === 'workflow-detail' && selectedWorkflow && (
        <WorkflowDetailView
          workflow={selectedWorkflow}
          onBack={() => { setView('list'); setSelectedWorkflow(null); }}
          onRefresh={fetchWorkflows}
        />
      )}
    </div>
  );
}

interface WorkflowDetailViewProps {
  workflow: Workflow;
  onBack: () => void;
  onRefresh: () => void;
}

function WorkflowDetailView({ workflow, onBack, onRefresh }: WorkflowDetailViewProps) {
  const [statuses, setStatuses] = useState<WorkflowStatus[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchWorkflowDetails();
  }, [workflow.id]);

  const fetchWorkflowDetails = async () => {
    setLoading(true);
    try {
      const res = await workflowApi.getById(workflow.id);
      if (res.data) {
        setStatuses(res.data.statuses || []);
      }
    } catch (err) {
      console.error('Failed to fetch workflow details:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="ab-detail-page">
      <button className="ab-back-btn" onClick={onBack}>← Back to Workflows</button>
      <h1 className="ab-detail-title">
        {workflow.name}
        {workflow.isDraft && <span className="ab-badge ab-badge-draft">DRAFT</span>}
      </h1>
      <p className="ab-detail-description">{workflow.description}</p>

      <div className="ab-tabs">
        <button className="ab-tab active">Statuses ({statuses.length})</button>
        <button className="ab-tab">Transitions</button>
      </div>

      <div className="ab-card">
        {loading ? (
          <div className="ab-loading">Loading...</div>
        ) : statuses.length === 0 ? (
          <p className="ab-empty">No statuses defined.</p>
        ) : (
          <div className="ab-status-list">
            {statuses.map((status) => (
              <div key={status.id} className="ab-status-card">
                <div className="ab-status-color" style={{ backgroundColor: status.color || '#6C757D' }} />
                <div className="ab-status-info">
                  <strong>{status.name}</strong>
                  <span className="ab-status-category">{status.category}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}