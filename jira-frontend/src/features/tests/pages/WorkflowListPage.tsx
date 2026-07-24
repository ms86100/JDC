import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import workflowEngineApi, { WorkflowDefinitionResponse } from '../../../api/workflowEngineApi';
import { appNotify } from '../../../lib/appNotify';
import './WorkflowListPage.css';

interface WorkflowListPageProps {
  projectId?: string;
}

export const WorkflowListPage: React.FC<WorkflowListPageProps> = ({ projectId }) => {
  const params = useParams<{ projectId?: string }>();
  const effectiveProjectId = projectId || params.projectId || '';
  const queryClient = useQueryClient();

  const [filter, setFilter] = useState<{
    search?: string;
    workflowType?: string;
    showActive?: boolean;
  }>({
    search: '',
    workflowType: '',
    showActive: true,
  });

  // Load workflows
  const { data: workflows = [], isLoading } = useQuery({
    queryKey: ['workflow-definitions', effectiveProjectId, filter.workflowType],
    queryFn: async () => {
      if (filter.workflowType) {
        return workflowEngineApi.getDefinitionsByType(effectiveProjectId, filter.workflowType);
      }
      if (effectiveProjectId) {
        return workflowEngineApi.getDefinitionsByProject(effectiveProjectId);
      }
      return [];
    },
    enabled: true,
  });

  // Filter workflows client-side
  const filteredWorkflows = workflows.filter(w => {
    if (filter.search) {
      const search = filter.search.toLowerCase();
      if (!w.name.toLowerCase().includes(search) &&
          !w.description?.toLowerCase().includes(search)) {
        return false;
      }
    }
    if (filter.showActive !== undefined && w.isActive !== filter.showActive) {
      return false;
    }
    return true;
  });

  // Activate/Deactivate mutations
  const activateMutation = useMutation({
    mutationFn: (id: string) => workflowEngineApi.activateDefinition(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['workflow-definitions'] }),
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => workflowEngineApi.deactivateDefinition(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['workflow-definitions'] }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => workflowEngineApi.deleteDefinition(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['workflow-definitions'] }),
    onError: (error: Error) => {
      appNotify.error(`Failed to delete: ${error.message}`);
    },
  });

  const getWorkflowTypeLabel = (type: string) => {
    const labels: Record<string, string> = {
      TEST_EXECUTION: 'Test Execution',
      TEST_PLAN: 'Test Plan',
      TEST_SET: 'Test Set',
      DEFECT: 'Defect',
      REVIEW: 'Review',
    };
    return labels[type] || type;
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  return (
    <div className="workflow-list-page">
      {/* Header */}
      <header className="workflow-list-header">
        <div>
          <h1>Workflow Engine</h1>
          {effectiveProjectId && (
            <p className="subtitle">Project: {effectiveProjectId.slice(0, 8)}...</p>
          )}
        </div>
        <Link
          to={effectiveProjectId ? `/tests/workflows/builder?projectId=${effectiveProjectId}` : '/tests/workflows/builder'}
          className="btn btn-primary"
        >
          + Create Workflow
        </Link>
      </header>

      {/* Filter Bar */}
      <div className="workflow-filter-bar">
        <input
          type="text"
          placeholder="Search workflows..."
          value={filter.search || ''}
          onChange={(e) => setFilter({ ...filter, search: e.target.value })}
          className="search-input"
        />
        <select
          value={filter.workflowType || ''}
          onChange={(e) => setFilter({ ...filter, workflowType: e.target.value || undefined })}
          className="type-select"
        >
          <option value="">All Types</option>
          <option value="TEST_EXECUTION">Test Execution</option>
          <option value="TEST_PLAN">Test Plan</option>
          <option value="TEST_SET">Test Set</option>
          <option value="DEFECT">Defect</option>
          <option value="REVIEW">Review</option>
        </select>
        <div className="active-filter">
          <label>
            <input
              type="checkbox"
              checked={filter.showActive}
              onChange={(e) => setFilter({ ...filter, showActive: e.target.checked })}
            />
            Show Active Only
          </label>
        </div>
      </div>

      {/* Workflow List */}
      <div className="workflow-list">
        {isLoading ? (
          <div className="loading-state">Loading workflows...</div>
        ) : filteredWorkflows.length === 0 ? (
          <div className="empty-state">
            <h3>No workflows found</h3>
            <p>
              {filter.search || filter.workflowType
                ? 'Try adjusting your filters.'
                : 'Create your first workflow to get started.'}
            </p>
            {!filter.search && !filter.workflowType && (
              <Link
                to={effectiveProjectId ? `/tests/workflows/builder?projectId=${effectiveProjectId}` : '/tests/workflows/builder'}
                className="btn btn-primary"
              >
                Create Workflow
              </Link>
            )}
          </div>
        ) : (
          <table className="workflow-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Type</th>
                <th>Status</th>
                <th>Default</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredWorkflows.map(workflow => (
                <tr key={workflow.id}>
                  <td>
                    <div className="workflow-name-cell">
                      <Link
                        to={`/tests/workflows/builder/${workflow.id}`}
                        className="workflow-name"
                      >
                        {workflow.name}
                      </Link>
                      {workflow.description && (
                        <span className="workflow-description">
                          {workflow.description}
                        </span>
                      )}
                    </div>
                  </td>
                  <td>
                    <span className={`type-badge type-${workflow.workflowType.toLowerCase()}`}>
                      {getWorkflowTypeLabel(workflow.workflowType)}
                    </span>
                  </td>
                  <td>
                    <span className={`status-badge ${workflow.isActive ? 'active' : 'inactive'}`}>
                      {workflow.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td>
                    {workflow.isDefault ? (
                      <span className="default-badge">Default</span>
                    ) : (
                      <span className="no-default">-</span>
                    )}
                  </td>
                  <td className="date-cell">{formatDate(workflow.createdAt)}</td>
                  <td>
                    <div className="action-buttons">
                      <Link
                        to={`/tests/workflows/builder/${workflow.id}`}
                        className="btn btn-sm btn-secondary"
                        title="Edit"
                      >
                        Edit
                      </Link>
                      {workflow.isActive ? (
                        <button
                          type="button"
                          className="btn btn-sm btn-secondary"
                          onClick={() => deactivateMutation.mutate(workflow.id)}
                          disabled={deactivateMutation.isPending}
                          title="Deactivate"
                        >
                          Deactivate
                        </button>
                      ) : (
                        <button
                          type="button"
                          className="btn btn-sm btn-secondary"
                          onClick={() => activateMutation.mutate(workflow.id)}
                          disabled={activateMutation.isPending}
                          title="Activate"
                        >
                          Activate
                        </button>
                      )}
                      <button
                        type="button"
                        className="btn btn-sm btn-danger"
                        onClick={() => {
                          if (confirm('Are you sure you want to delete this workflow?')) {
                            deleteMutation.mutate(workflow.id);
                          }
                        }}
                        disabled={deleteMutation.isPending}
                        title="Delete"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Stats Summary */}
      {workflows.length > 0 && (
        <div className="workflow-stats">
          <span>Total: {workflows.length} workflows</span>
          <span>Active: {workflows.filter(w => w.isActive).length}</span>
          <span>Inactive: {workflows.filter(w => !w.isActive).length}</span>
        </div>
      )}
    </div>
  );
};

export default WorkflowListPage;