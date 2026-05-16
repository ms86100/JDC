import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';

export default function WorkflowPage() {
  const [activeTab, setActiveTab] = useState<'workflows' | 'statuses'>('workflows');

  const { data: workflows, isLoading } = useQuery({
    queryKey: ['workflows'],
    queryFn: async () => {
      const response = await apiClient.get('/api/workflows');
      return response.data;
    },
  });

  return (
    <div className="ab-workflow-page">
      <div className="ab-page-header">
        <div>
          <h1 className="ab-page-title">Workflows</h1>
          <p className="ab-page-subtitle">Manage workflows and status transitions</p>
        </div>
        <button className="ab-btn ab-btn-primary">
          <span>+</span> Create Workflow
        </button>
      </div>

      <div className="ab-tabs">
        <button
          className={`ab-tab ${activeTab === 'workflows' ? 'active' : ''}`}
          onClick={() => setActiveTab('workflows')}
        >
          Workflows
        </button>
        <button
          className={`ab-tab ${activeTab === 'statuses' ? 'active' : ''}`}
          onClick={() => setActiveTab('statuses')}
        >
          Issue Statuses
        </button>
      </div>

      {isLoading ? (
        <div className="ab-loading">
          <div className="ab-spinner"></div>
        </div>
      ) : workflows && workflows.length > 0 ? (
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
              {workflows.map((workflow: any) => (
                <tr key={workflow.id}>
                  <td>{workflow.name}</td>
                  <td>{workflow.projectId || '-'}</td>
                  <td>{workflow.isDefault ? 'Yes' : 'No'}</td>
                  <td>{new Date(workflow.createdAt).toLocaleDateString()}</td>
                  <td>
                    <button className="ab-btn ab-btn-ghost ab-btn-sm">View</button>
                    <button className="ab-btn ab-btn-ghost ab-btn-sm">Edit</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="ab-card">
          <div className="ab-empty-state">
            <div className="ab-empty-state-icon">⚙️</div>
            <h3 className="ab-empty-state-title">No workflows found</h3>
            <p className="ab-empty-state-description">
              Create your first workflow to manage issue status transitions.
            </p>
            <button className="ab-btn ab-btn-primary">
              Create Workflow
            </button>
          </div>
        </div>
      )}
    </div>
  );
}