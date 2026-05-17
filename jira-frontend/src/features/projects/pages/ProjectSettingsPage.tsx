import { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { projectApi, ProjectResponse } from '../../../api/projectApi';
import '../styles/ProjectSettingsPage.css';

type SettingsTab = 'details' | 'members' | 'workflows' | 'components' | 'versions' | 'permissions';

export default function ProjectSettingsPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<SettingsTab>('details');
  const [editMode, setEditMode] = useState(false);

  const { data: project, isLoading } = useQuery<ProjectResponse>({
    queryKey: ['project', projectId],
    queryFn: async () => {
      const response = await projectApi.getById(projectId!);
      return response.data;
    },
  });

  const [form, setForm] = useState({
    name: '',
    description: '',
    leadUserId: '',
  });

  const updateMutation = useMutation({
    mutationFn: (data: any) => projectApi.update(projectId!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', projectId] });
      setEditMode(false);
    },
  });

  const handleSave = () => {
    updateMutation.mutate(form);
  };

  const handleCancel = () => {
    if (project) {
      setForm({
        name: project.name,
        description: project.description || '',
        leadUserId: project.leadUserId || '',
      });
    }
    setEditMode(false);
  };

  if (isLoading) {
    return (
      <div className="ab-loading">
        <div className="ab-spinner"></div>
      </div>
    );
  }

  if (!project) {
    return (
      <div className="ab-empty-state">
        <h3>Project not found</h3>
        <Link to="/projects" className="ab-btn ab-btn-primary">Back to Projects</Link>
      </div>
    );
  }

  const tabs: { id: SettingsTab; label: string }[] = [
    { id: 'details', label: 'Details' },
    { id: 'members', label: 'Members' },
    { id: 'workflows', label: 'Workflows' },
    { id: 'components', label: 'Components' },
    { id: 'versions', label: 'Versions' },
    { id: 'permissions', label: 'Permissions' },
  ];

  return (
    <div className="ab-project-settings">
      <div className="ab-settings-header">
        <div className="ab-breadcrumb">
          <Link to="/projects" className="ab-link">Projects</Link>
          <span className="ab-text-muted"> / </span>
          <Link to={`/projects/${projectId}`} className="ab-link">{project.name}</Link>
          <span className="ab-text-muted"> / </span>
          <span>Settings</span>
        </div>
        <h1 className="ab-page-title">Project Settings</h1>
        <p className="ab-page-subtitle">Configure {project.name} project settings</p>
      </div>

      <div className="ab-settings-tabs">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            className={`ab-settings-tab ${activeTab === tab.id ? 'active' : ''}`}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="ab-settings-content">
        {activeTab === 'details' && (
          <div className="ab-settings-section">
            <div className="ab-card">
              <div className="ab-card-header">
                <h3 className="ab-card-title">Project Details</h3>
                {!editMode && (
                  <button className="ab-btn ab-btn-ghost ab-btn-sm" onClick={() => setEditMode(true)}>
                    Edit
                  </button>
                )}
              </div>
              <div className="ab-card-body">
                {editMode ? (
                  <div className="ab-form">
                    <div className="ab-form-group">
                      <label className="ab-label">Project Name</label>
                      <input
                        type="text"
                        className="ab-input"
                        value={form.name}
                        onChange={(e) => setForm({ ...form, name: e.target.value })}
                      />
                    </div>
                    <div className="ab-form-group">
                      <label className="ab-label">Description</label>
                      <textarea
                        className="ab-textarea"
                        rows={4}
                        value={form.description}
                        onChange={(e) => setForm({ ...form, description: e.target.value })}
                      />
                    </div>
                    <div className="ab-form-actions">
                      <button className="ab-btn ab-btn-primary" onClick={handleSave}>
                        Save
                      </button>
                      <button className="ab-btn ab-btn-secondary" onClick={handleCancel}>
                        Cancel
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="ab-details-grid">
                    <div className="ab-detail-item">
                      <span className="ab-detail-label">Project Name</span>
                      <span className="ab-detail-value">{project.name}</span>
                    </div>
                    <div className="ab-detail-item">
                      <span className="ab-detail-label">Project Key</span>
                      <span className="ab-detail-value">{project.projectKey}</span>
                    </div>
                    <div className="ab-detail-item">
                      <span className="ab-detail-label">Description</span>
                      <span className="ab-detail-value">{project.description || 'No description'}</span>
                    </div>
                    <div className="ab-detail-item">
                      <span className="ab-detail-label">Project Lead</span>
                      <span className="ab-detail-value">{project.leadName || 'Unassigned'}</span>
                    </div>
                    <div className="ab-detail-item">
                      <span className="ab-detail-label">Project Type</span>
                      <span className="ab-detail-value">{project.projectType}</span>
                    </div>
                    <div className="ab-detail-item">
                      <span className="ab-detail-label">Template</span>
                      <span className="ab-detail-value">{project.template}</span>
                    </div>
                    <div className="ab-detail-item">
                      <span className="ab-detail-label">Created</span>
                      <span className="ab-detail-value">
                        {project.createdAt ? new Date(project.createdAt).toLocaleDateString() : '-'}
                      </span>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {activeTab === 'members' && (
          <div className="ab-settings-section">
            <div className="ab-card">
              <div className="ab-card-header">
                <h3 className="ab-card-title">Project Members</h3>
                <button className="ab-btn ab-btn-primary btn-sm">Add Member</button>
              </div>
              <div className="ab-card-body">
                <div className="ab-members-list">
                  {project.leadName && (
                    <div className="ab-member-item">
                      <div className="ab-member-avatar">{project.leadName.charAt(0).toUpperCase()}</div>
                      <div className="ab-member-info">
                        <span className="ab-member-name">{project.leadName}</span>
                        <span className="ab-member-role">Project Lead</span>
                      </div>
                      <span className="ab-badge ab-badge-primary">Lead</span>
                    </div>
                  )}
                  <div className="ab-empty-state-sm">
                    <p>No additional members added yet.</p>
                    <button className="ab-btn ab-btn-secondary btn-sm">Add Members</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'workflows' && (
          <div className="ab-settings-section">
            <div className="ab-card">
              <div className="ab-card-header">
                <h3 className="ab-card-title">Project Workflows</h3>
                <button className="ab-btn ab-btn-primary btn-sm" onClick={() => navigate('/workflows')}>
                  Manage Workflows
                </button>
              </div>
              <div className="ab-card-body">
                <div className="ab-workflow-info">
                  <p>Configure workflows for this project to define how issues progress through statuses.</p>
                  <Link to="/workflows" className="ab-link">Go to Workflow Administration →</Link>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'components' && (
          <div className="ab-settings-section">
            <div className="ab-card">
              <div className="ab-card-header">
                <h3 className="ab-card-title">Project Components</h3>
                <button className="ab-btn ab-btn-primary btn-sm">Add Component</button>
              </div>
              <div className="ab-card-body">
                <div className="ab-empty-state-sm">
                  <p>No components defined for this project.</p>
                  <button className="ab-btn ab-btn-secondary btn-sm">Add Component</button>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'versions' && (
          <div className="ab-settings-section">
            <div className="ab-card">
              <div className="ab-card-header">
                <h3 className="ab-card-title">Project Versions</h3>
                <button className="ab-btn ab-btn-primary btn-sm">Add Version</button>
              </div>
              <div className="ab-card-body">
                <div className="ab-empty-state-sm">
                  <p>No versions defined for this project.</p>
                  <button className="ab-btn ab-btn-secondary btn-sm">Add Version</button>
                </div>
              </div>
            </div>
          </div>
        )}

        {activeTab === 'permissions' && (
          <div className="ab-settings-section">
            <div className="ab-card">
              <div className="ab-card-header">
                <h3 className="ab-card-title">Project Permissions</h3>
                <button className="ab-btn ab-btn-secondary btn-sm" onClick={() => navigate('/admin/permissions')}>
                  Manage Permissions
                </button>
              </div>
              <div className="ab-card-body">
                <div className="ab-permission-info">
                  <p>Configure who can access and modify issues in this project.</p>
                  <Link to="/admin/permissions" className="ab-link">Go to Permissions Administration →</Link>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}