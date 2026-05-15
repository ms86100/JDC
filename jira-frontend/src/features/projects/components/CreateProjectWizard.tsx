import { useState, useEffect } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { projectApi, ProjectTemplate, CreateProjectWizardRequest, ProjectType } from '../../../api/projectApi';
import '../styles/create-project-wizard.css';

interface WizardState {
  step: number;
  projectType: 'COMPANY_MANAGED' | 'TEAM_MANAGED' | null;
  template: ProjectTemplate | null;
  name: string;
  projectKey: string;
  description: string;
  leadUserId: string;
  defaultAssigneeType: string;
}

const INITIAL_STATE: WizardState = {
  step: 1,
  projectType: null,
  template: null,
  name: '',
  projectKey: '',
  description: '',
  leadUserId: '',
  defaultAssigneeType: 'PROJECT_LEAD',
};

export default function CreateProjectWizard() {
  const navigate = useNavigate();
  const [wizardState, setWizardState] = useState<WizardState>(INITIAL_STATE);
  const [keyValidation, setKeyValidation] = useState<{ valid: boolean; available: boolean }>({
    valid: true,
    available: true,
  });

  // Fetch project types
  const { data: projectTypes, isLoading: isLoadingTypes, error: typesError } = useQuery({
    queryKey: ['projectTypes'],
    queryFn: () => projectApi.getProjectTypes().then((res) => res.data),
    retry: 2,
  });

  // Fetch templates when project type is selected
  const { data: templates, isLoading: isLoadingTemplates } = useQuery({
    queryKey: ['templates', wizardState.projectType],
    queryFn: () => {
      const type = projectTypes?.find((t: ProjectType) => t.category === wizardState.projectType);
      if (!type) return Promise.resolve([]);
      return projectApi.getTemplatesForType(type.id).then((res) => res.data);
    },
    enabled: !!wizardState.projectType && !!projectTypes,
    retry: 2,
  });

  // Check project key availability
  useEffect(() => {
    if (wizardState.projectKey.length >= 2) {
      projectApi.checkProjectKey(wizardState.projectKey).then((res) => {
        setKeyValidation({ valid: res.data.valid, available: res.data.available });
      });
    } else {
      setKeyValidation({ valid: true, available: true });
    }
  }, [wizardState.projectKey]);

  // Auto-generate project key from name
  useEffect(() => {
    if (wizardState.name && !wizardState.projectKey) {
      const words = wizardState.name.trim().split(/\s+/);
      let key = '';
      for (const word of words) {
        if (key.length >= 10) break;
        const cleaned = word.replace(/[^a-zA-Z0-9]/g, '');
        if (cleaned) {
          key += cleaned.charAt(0).toUpperCase();
          if (key.length >= 10) break;
        }
      }
      if (!key) key = 'PRJ';
      while (key.length < 3) key += 'X';
      setWizardState((prev) => ({ ...prev, projectKey: key.substring(0, 10) }));
    }
  }, [wizardState.name]);

  const createMutation = useMutation({
    mutationFn: (data: CreateProjectWizardRequest) => projectApi.createViaWizard(data),
    onSuccess: (response) => {
      navigate(`/projects/${response.data.id}`);
    },
  });

  const handleTypeSelect = (category: 'COMPANY_MANAGED' | 'TEAM_MANAGED') => {
    setWizardState((prev) => ({
      ...prev,
      projectType: category,
      template: null,
    }));
  };

  const handleTemplateSelect = (template: ProjectTemplate) => {
    setWizardState((prev) => ({
      ...prev,
      template,
      defaultAssigneeType: template.defaultAssigneeType,
    }));
  };

  const handleNext = () => {
    setWizardState((prev) => ({ ...prev, step: prev.step + 1 }));
  };

  const handleBack = () => {
    setWizardState((prev) => ({ ...prev, step: prev.step - 1 }));
  };

  const handleSubmit = () => {
    const request: CreateProjectWizardRequest = {
      projectType: wizardState.projectType!,
      templateId: wizardState.template?.id,
      name: wizardState.name,
      projectKey: wizardState.projectKey,
      description: wizardState.description,
      leadUserId: wizardState.leadUserId || undefined,
      defaultAssigneeType: wizardState.defaultAssigneeType,
    };
    createMutation.mutate(request);
  };

  const canProceed = () => {
    switch (wizardState.step) {
      case 1:
        return wizardState.projectType !== null;
      case 2:
        return wizardState.template !== null || wizardState.projectType === 'TEAM_MANAGED';
      case 3:
        return wizardState.name.length >= 1 && wizardState.projectKey.length >= 2 && keyValidation.valid && keyValidation.available;
      case 4:
        return true;
      default:
        return false;
    }
  };

  return (
    <div className="ab-wizard-overlay">
      <div className="ab-wizard">
        <div className="ab-wizard-header">
          <h2 className="ab-wizard-title">Create Project</h2>
          <button className="ab-btn-icon ab-wizard-close" onClick={() => navigate('/projects')}>
            ✕
          </button>
        </div>

        {/* Progress Steps */}
        <div className="ab-wizard-steps">
          <div className={`ab-step ${wizardState.step >= 1 ? 'active' : ''} ${wizardState.step > 1 ? 'completed' : ''}`}>
            <div className="ab-step-number">{wizardState.step > 1 ? '✓' : '1'}</div>
            <div className="ab-step-label">Type</div>
          </div>
          <div className="ab-step-connector"></div>
          <div className={`ab-step ${wizardState.step >= 2 ? 'active' : ''} ${wizardState.step > 2 ? 'completed' : ''}`}>
            <div className="ab-step-number">{wizardState.step > 2 ? '✓' : '2'}</div>
            <div className="ab-step-label">Template</div>
          </div>
          <div className="ab-step-connector"></div>
          <div className={`ab-step ${wizardState.step >= 3 ? 'active' : ''} ${wizardState.step > 3 ? 'completed' : ''}`}>
            <div className="ab-step-number">{wizardState.step > 3 ? '✓' : '3'}</div>
            <div className="ab-step-label">Details</div>
          </div>
          <div className="ab-step-connector"></div>
          <div className={`ab-step ${wizardState.step >= 4 ? 'active' : ''}`}>
            <div className="ab-step-number">4</div>
            <div className="ab-step-label">Review</div>
          </div>
        </div>

        <div className="ab-wizard-body">
          {/* Step 1: Project Type Selection */}
          {wizardState.step === 1 && (
            <div className="ab-wizard-step">
              <h3 className="ab-step-title">Select your project type</h3>
              <p className="ab-step-description">
                Choose how you want to manage your project. You can change most settings later.
              </p>
              {isLoadingTypes ? (
                <div className="ab-loading">
                  <div className="ab-spinner"></div>
                </div>
              ) : typesError ? (
                <div className="ab-empty-state">
                  <div className="ab-empty-state-icon">⚠️</div>
                  <h4 className="ab-empty-state-title">Failed to load project types</h4>
                  <p className="ab-empty-state-description">Please check your connection and try again.</p>
                </div>
              ) : (
                <div className="ab-type-grid">
                  {projectTypes && projectTypes.length > 0 ? (
                    projectTypes.map((type: ProjectType) => (
                      <div
                        key={type.id}
                        className={`ab-type-card ${wizardState.projectType === type.category ? 'selected' : ''}`}
                        onClick={() => handleTypeSelect(type.category as 'COMPANY_MANAGED' | 'TEAM_MANAGED')}
                      >
                        <div className="ab-type-icon">{type.category === 'COMPANY_MANAGED' ? '💼' : '👥'}</div>
                        <h4 className="ab-type-name">{type.name}</h4>
                        <p className="ab-type-description">{type.description}</p>
                      </div>
                    ))
                  ) : (
                    <div className="ab-empty-state">
                      <div className="ab-empty-state-icon">📋</div>
                      <h4 className="ab-empty-state-title">No project types available</h4>
                      <p className="ab-empty-state-description">Project types haven't been configured yet.</p>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {/* Step 2: Template Selection */}
          {wizardState.step === 2 && (
            <div className="ab-wizard-step">
              <h3 className="ab-step-title">Select a template</h3>
              <p className="ab-step-description">
                Templates include preset configurations for workflows, issue types, and more.
              </p>
              {isLoadingTemplates ? (
                <div className="ab-loading">
                  <div className="ab-spinner"></div>
                </div>
              ) : (
                <div className="ab-template-grid">
                  {templates && templates.length > 0 ? (
                    templates.map((template: ProjectTemplate) => (
                      <div
                        key={template.id}
                        className={`ab-template-card ${wizardState.template?.id === template.id ? 'selected' : ''}`}
                        onClick={() => handleTemplateSelect(template)}
                        style={{ '--template-color': template.color } as React.CSSProperties}
                      >
                        <div className="ab-template-icon" style={{ backgroundColor: template.color }}>
                          {template.icon === 'scrum' && '🏃'}
                          {template.icon === 'kanban' && '📋'}
                          {template.icon === 'bug' && '🐛'}
                          {template.icon === 'task' && '✓'}
                          {template.icon === 'portfolio' && '📊'}
                          {template.icon === 'team' && '👥'}
                        </div>
                        <h4 className="ab-template-name">{template.name}</h4>
                        <p className="ab-template-description">{template.description}</p>
                        {template.defaultAssigneeType === 'PROJECT_LEAD' && (
                          <span className="ab-template-badge">Project Lead</span>
                        )}
                      </div>
                    ))
                  ) : (
                    <div className="ab-empty-state">
                      <div className="ab-empty-state-icon">📋</div>
                      <h4 className="ab-empty-state-title">No templates available</h4>
                      <p className="ab-empty-state-description">No templates found for this project type.</p>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {/* Step 3: Project Details */}
          {wizardState.step === 3 && (
            <div className="ab-wizard-step">
              <h3 className="ab-step-title">Configure your project</h3>
              <p className="ab-step-description">
                Enter the basic information for your project.
              </p>
              <div className="ab-form">
                <div className="ab-form-group">
                  <label className="ab-label">Project Name *</label>
                  <input
                    type="text"
                    className="ab-input"
                    value={wizardState.name}
                    onChange={(e) => setWizardState((prev) => ({ ...prev, name: e.target.value }))}
                    placeholder="e.g., My Awesome Project"
                    maxLength={200}
                  />
                </div>

                <div className="ab-form-group">
                  <label className="ab-label">Project Key *</label>
                  <input
                    type="text"
                    className={`ab-input ${!keyValidation.valid || !keyValidation.available ? 'ab-input-error' : ''}`}
                    value={wizardState.projectKey}
                    onChange={(e) =>
                      setWizardState((prev) => ({
                        ...prev,
                        projectKey: e.target.value.toUpperCase().replace(/[^A-Z0-9]/g, ''),
                      }))
                    }
                    placeholder="e.g., MAP"
                    maxLength={10}
                  />
                  {!keyValidation.valid && (
                    <span className="ab-field-error">Key must be 2-10 uppercase letters/numbers</span>
                  )}
                  {!keyValidation.available && keyValidation.valid && (
                    <span className="ab-field-error">This key is already in use</span>
                  )}
                  <span className="ab-field-hint">
                    The key is used as a prefix for issues (e.g., MAP-123). You cannot change this later.
                  </span>
                </div>

                <div className="ab-form-group">
                  <label className="ab-label">Description</label>
                  <textarea
                    className="ab-textarea"
                    value={wizardState.description}
                    onChange={(e) => setWizardState((prev) => ({ ...prev, description: e.target.value }))}
                    placeholder="Describe what this project is about..."
                    rows={4}
                  />
                </div>

                <div className="ab-form-group">
                  <label className="ab-label">Default Assignee</label>
                  <select
                    className="ab-select"
                    value={wizardState.defaultAssigneeType}
                    onChange={(e) => setWizardState((prev) => ({ ...prev, defaultAssigneeType: e.target.value }))}
                  >
                    <option value="PROJECT_LEAD">Project Lead</option>
                    <option value="UNASSIGNED">Unassigned</option>
                  </select>
                  <span className="ab-field-hint">
                    The default assignee for issues created in this project.
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* Step 4: Review */}
          {wizardState.step === 4 && (
            <div className="ab-wizard-step">
              <h3 className="ab-step-title">Review and confirm</h3>
              <p className="ab-step-description">
                Please review your project configuration before creating.
              </p>
              <div className="ab-review-section">
                <div className="ab-review-card">
                  <h4 className="ab-review-section-title">Project Type</h4>
                  <div className="ab-review-item">
                    <span className="ab-review-label">Type:</span>
                    <span className="ab-review-value">
                      {wizardState.projectType === 'COMPANY_MANAGED' ? 'Company-managed' : 'Team-managed'}
                    </span>
                  </div>
                  {wizardState.template && (
                    <div className="ab-review-item">
                      <span className="ab-review-label">Template:</span>
                      <span className="ab-review-value">{wizardState.template.name}</span>
                    </div>
                  )}
                </div>

                <div className="ab-review-card">
                  <h4 className="ab-review-section-title">Project Details</h4>
                  <div className="ab-review-item">
                    <span className="ab-review-label">Name:</span>
                    <span className="ab-review-value">{wizardState.name}</span>
                  </div>
                  <div className="ab-review-item">
                    <span className="ab-review-label">Key:</span>
                    <span className="ab-review-value ab-badge ab-badge-primary">{wizardState.projectKey}</span>
                  </div>
                  <div className="ab-review-item">
                    <span className="ab-review-label">Description:</span>
                    <span className="ab-review-value">{wizardState.description || 'No description'}</span>
                  </div>
                  <div className="ab-review-item">
                    <span className="ab-review-label">Default Assignee:</span>
                    <span className="ab-review-value">
                      {wizardState.defaultAssigneeType === 'PROJECT_LEAD' ? 'Project Lead' : 'Unassigned'}
                    </span>
                  </div>
                </div>

                {wizardState.template && (
                  <div className="ab-review-card">
                    <h4 className="ab-review-section-title">Configurations</h4>
                    <div className="ab-review-item">
                      <span className="ab-review-label">Issue Types:</span>
                      <span className="ab-review-value">Based on {wizardState.template.name} template</span>
                    </div>
                    <div className="ab-review-item">
                      <span className="ab-review-label">Workflow:</span>
                      <span className="ab-review-value">Default {wizardState.template.name} workflow</span>
                    </div>
                  </div>
                )}
              </div>

              {createMutation.isError && (
                <div className="ab-alert ab-alert-danger" style={{ marginTop: '1rem' }}>
                  <span>Failed to create project. Please try again.</span>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="ab-wizard-footer">
          {wizardState.step > 1 && (
            <button className="ab-btn ab-btn-secondary" onClick={handleBack}>
              Back
            </button>
          )}
          {wizardState.step < 4 ? (
            <button
              className="ab-btn ab-btn-primary"
              onClick={handleNext}
              disabled={!canProceed()}
            >
              Next
            </button>
          ) : (
            <button
              className="ab-btn ab-btn-primary"
              onClick={handleSubmit}
              disabled={createMutation.isPending}
            >
              {createMutation.isPending ? 'Creating...' : 'Create Project'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
