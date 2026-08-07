import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { projectApi, ProjectResponse } from '../../../api/projectApi';
import { versionApi } from '../../../api/versionApi';
import { componentApi } from '../../../api/componentApi';
import { issueApi, CreateIssueRequest, IssueType, IssuePriority } from '../../../api/issueApi';
import apiClient from '../../../api/axiosClient';
import { appNotify } from '../../../lib/appNotify';
import { useFieldBehaviors } from '../../../hooks/useFieldBehaviors';
import { useLiveFields } from '../../../hooks/useLiveFields';
import './CreateIssueModal.css';

interface CreateIssueModalProps {
  onClose: () => void;
  onSuccess?: () => void;
  projectId?: string;
  projectKey?: string;
  parentIssueId?: string;
  defaultTitle?: string;
}

interface LinkedIssue {
  targetIssueKey: string;
  linkType: string;
}

const ISSUE_LINK_TYPES = [
  { value: 'blocks', label: 'Blocks' },
  { value: 'is_blocked_by', label: 'Is blocked by' },
  { value: 'relates_to', label: 'Relates to' },
  { value: 'duplicates', label: 'Duplicates' },
  { value: 'clones', label: 'Clones' },
  { value: 'is_cloned_by', label: 'Is cloned by' },
];

export default function CreateIssueModal({
  onClose,
  onSuccess,
  projectId: propProjectId,
  projectKey: propProjectKey,
  parentIssueId: propParentIssueId,
  defaultTitle,
}: CreateIssueModalProps) {
  const [form, setForm] = useState<CreateIssueRequest & { linkedIssues: LinkedIssue[] }>({
    projectId: propProjectId || '',
    title: defaultTitle || '',
    description: '',
    issueTypeId: '',
    priorityId: '',
    assigneeId: '',
    reporterId: '',
    sprintId: '',
    epicId: '',
    parentIssueId: propParentIssueId || undefined,
    parentId: propParentIssueId || '',
    dueDate: '',
    storyPoints: undefined,
    originalEstimateSeconds: undefined,
    remainingEstimateSeconds: undefined,
    labels: [],
    securityLevelId: '',
    environment: '',
    teamId: '',
    fixVersionIds: [],
    affectsVersionIds: [],
    componentIds: [],
    linkedIssues: [],
  });

  const [labelInput, setLabelInput] = useState('');
  const [newLinkedIssue, setNewLinkedIssue] = useState<LinkedIssue>({ targetIssueKey: '', linkType: 'blocks' });

  // Project dropdown (only if no propProjectId)
  const { data: projects = [] } = useQuery<ProjectResponse[]>({
    queryKey: ['projects'],
    queryFn: async () => {
      const response = await projectApi.getAll();
      return response;
    },
  });

  // Issue Types
  const { data: issueTypesList = [] } = useQuery<IssueType[]>({
    queryKey: ['issueTypesList'],
    queryFn: async () => {
      const response = await issueApi.getTypes();
      return response.data;
    },
  });

  // Priorities
  const { data: priorities = [] } = useQuery<IssuePriority[]>({
    queryKey: ['priorities'],
    queryFn: async () => {
      const response = await issueApi.getPriorities();
      return response.data;
    },
  });

  // Project Members (for assignee/reporter)
  const { data: projectUsers = [] } = useQuery({
    queryKey: ['projectUsers', form.projectId],
    queryFn: async () => {
      if (!form.projectId) return [];
      const response = await apiClient.get<{ id: string; userName?: string }[]>(`/api/projects/${form.projectId}/members`);
      return response.data || [];
    },
    enabled: !!form.projectId,
  });

  // Sprints
  const { data: sprints = [] } = useQuery({
    queryKey: ['sprints', form.projectId],
    queryFn: async () => {
      if (!form.projectId) return [];
      const response = await projectApi.getSprints(form.projectId);
      const d = response.data;
      if (Array.isArray(d)) return d;
      return (d as any)?.content ?? [];
    },
    enabled: !!form.projectId,
  });

  // Versions
  const { data: versions = [] } = useQuery({
    queryKey: ['project-versions', form.projectId],
    queryFn: () => (form.projectId ? versionApi.getByProject(form.projectId) : []),
    enabled: !!form.projectId,
  });

  const { data: components = [] } = useQuery({
    queryKey: ['project-components', form.projectId],
    queryFn: () => (form.projectId ? componentApi.getByProject(form.projectId) : []),
    enabled: !!form.projectId,
  });

  const serverBehaviors = useFieldBehaviors({
    screenContext: 'CREATE',
    projectId: form.projectId || undefined,
    issueTypeId: form.issueTypeId || undefined,
    issueData: form as unknown as Record<string, unknown>,
    enabled: !!form.projectId,
  });

  const liveFields = useLiveFields();

  // Merge server-side directives into client-side Live Fields whenever they change
  const { directives: serverDirectives } = serverBehaviors;
  const prevDirectivesRef = useState<string>('')[1];
  const directivesJson = JSON.stringify(serverDirectives);
  if (directivesJson !== prevDirectivesRef.toString()) {
    liveFields.applyServerDirectives(serverDirectives.map(d => ({
      fieldName: d.fieldName,
      visible: d.visible,
      required: d.required,
      readOnly: d.readOnly,
      defaultValue: d.defaultValue,
      options: d.options,
      label: d.label,
      message: d.warning,
    })));
  }

  const fb = (fieldName: string) => ({
    visible: liveFields.isFieldVisible(fieldName) && serverBehaviors.isFieldVisible(fieldName),
    required: liveFields.isFieldRequired(fieldName) || serverBehaviors.isFieldRequired(fieldName),
    readOnly: liveFields.isFieldReadOnly(fieldName) || serverBehaviors.isFieldReadOnly(fieldName),
    warning: liveFields.getFieldMessage(fieldName) || serverBehaviors.getFieldWarning(fieldName),
    label: liveFields.getFieldLabel(fieldName) || serverBehaviors.getFieldLabel(fieldName),
    helpText: liveFields.getFieldDescription(fieldName) || serverBehaviors.getFieldHelpText(fieldName),
    options: liveFields.getFieldOptions(fieldName) || serverBehaviors.getFieldOptions(fieldName),
    defaultValue: getFieldDefault(fieldName),
  });

  const isFieldVisible = (fieldName: string) => fb(fieldName).visible;
  const isFieldRequired = (fieldName: string) => fb(fieldName).required;
  const getFieldLabel = (fieldName: string) => fb(fieldName).label;

  // Create Mutation
  const createMutation = useMutation({
    mutationFn: async (data: CreateIssueRequest & { linkedIssues: LinkedIssue[]; parentId?: string }) => {
      const { linkedIssues, parentId, ...issueData } = data;
      const payload: CreateIssueRequest = {
        ...(issueData as CreateIssueRequest),
        parentIssueId: issueData.parentIssueId || parentId || propParentIssueId || undefined,
      };
      const response = await issueApi.create(payload);
      const issueId = response.data?.id;
      if (issueId) {
        const { syncIssueVersionComponentLinks } = await import('../../../lib/syncIssueVersionComponentLinks');
        await syncIssueVersionComponentLinks(issueId, {
          fixVersionIds: payload.fixVersionIds,
          affectsVersionIds: payload.affectsVersionIds,
          componentIds: payload.componentIds,
        });
      }

      // If linked issues exist, add them after issue creation
      if (linkedIssues && linkedIssues.length > 0 && response.data?.id) {
        for (const link of linkedIssues) {
          try {
            await issueApi.linkIssue(response.data.id, {
              targetIssueKey: link.targetIssueKey,
              linkType: link.linkType,
            });
          } catch (err) {
            console.warn('Failed to link issue:', link.targetIssueKey, err);
          }
        }
      }
      return response;
    },
    onSuccess: () => {
      if (onSuccess) onSuccess();
      onClose();
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.projectId || !form.title || !form.issueTypeId) {
      appNotify.warning('Please fill in required fields');
      return;
    }
    createMutation.mutate(form);
  };

  const handleAddLabel = () => {
    if (labelInput.trim() && !form.labels?.includes(labelInput.trim())) {
      setForm({ ...form, labels: [...(form.labels || []), labelInput.trim()] });
      setLabelInput('');
    }
  };

  const handleRemoveLabel = (label: string) => {
    setForm({ ...form, labels: form.labels?.filter(l => l !== label) || [] });
  };

  const handleRemoveLinkedIssue = (index: number) => {
    setForm({
      ...form,
      linkedIssues: form.linkedIssues.filter((_, i) => i !== index),
    });
  };

  const getLinkTypeLabel = (linkType: string) => {
    return ISSUE_LINK_TYPES.find(lt => lt.value === linkType)?.label || linkType;
  };

  // Helper to parse time string to seconds (e.g., "4h" -> 14400)
  const parseTimeToSeconds = (timeStr: string): number | undefined => {
    if (!timeStr) return undefined;
    const hourMatch = timeStr.match(/^(\d+)h?$/);
    if (hourMatch) return parseInt(hourMatch[1]) * 3600;
    const dayMatch = timeStr.match(/^(\d+)d?$/);
    if (dayMatch) return parseInt(dayMatch[1]) * 8 * 3600; // Assume 8h work day
    return undefined;
  };

  return (
    <div className="ab-modal-overlay" onClick={onClose}>
      <div className="ab-modal create-issue-modal" onClick={(e) => e.stopPropagation()}>
        <div className="ab-modal-header">
          <h2 className="ab-modal-title">Create Issue</h2>
          <button className="ab-btn-icon" onClick={onClose}>
            <span className="ab-icon-close">✕</span>
          </button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="ab-modal-body create-issue-body">
            {/* LEFT COLUMN - Core fields */}
            <div className="form-column left-col">
              <div className="ab-form-group">
                <label className="ab-label">Project *</label>
                {propProjectId ? (
                  <input type="text" className="ab-input" value={propProjectKey || propProjectId} disabled />
                ) : (
                  <select
                    className="ab-select"
                    value={form.projectId}
                    onChange={(e) => setForm({ ...form, projectId: e.target.value })}
                    required
                  >
                    <option value="">Select Project</option>
                    {projects.map((project: any) => (
                      <option key={project.id} value={project.id}>
                        {project.name} ({project.projectKey})
                      </option>
                    ))}
                  </select>
                )}
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Issue Type *</label>
                <select
                  className="ab-select"
                  value={form.issueTypeId}
                  onChange={(e) => setForm({ ...form, issueTypeId: e.target.value })}
                  required
                >
                  <option value="">Select Type</option>
                  {(issueTypesList as IssueType[]).map((type: IssueType) => (
                    <option key={type.id} value={type.id}>
                      {type.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Summary *</label>
                <input
                  type="text"
                  className="ab-input"
                  value={form.title}
                  onChange={(e) => setForm({ ...form, title: e.target.value })}
                  placeholder="Brief description of the issue"
                  required
                />
              </div>

              {isFieldVisible('description') && (
              <div className="ab-form-group">
                <label className="ab-label">{getFieldLabel('description') || 'Description'}{isFieldRequired('description') ? ' *' : ''}</label>
                <textarea
                  className="ab-textarea"
                  value={form.description || ''}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  placeholder="Detailed description of the issue"
                  rows={4}
                  readOnly={isFieldReadOnly('description')}
                  required={isFieldRequired('description')}
                />
                {getFieldWarning('description') && (
                  <span className="ab-field-warning" style={{ color: '#b45309', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>
                    ⚠ {getFieldWarning('description')}
                  </span>
                )}
              </div>
              )}

              {isFieldVisible('environment') && (
              <div className="ab-form-group">
                <label className="ab-label">{getFieldLabel('environment') || 'Environment'}</label>
                <textarea
                  className="ab-textarea"
                  value={form.environment || ''}
                  onChange={(e) => setForm({ ...form, environment: e.target.value })}
                  placeholder="Environment where the issue was found"
                  rows={2}
                  readOnly={isFieldReadOnly('environment')}
                />
                {getFieldWarning('environment') && (
                  <span className="ab-field-warning" style={{ color: '#b45309', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>
                    ⚠ {getFieldWarning('environment')}
                  </span>
                )}
              </div>
              )}
            </div>

            {/* RIGHT COLUMN - Additional fields (all with field behavior support) */}
            <div className="form-column right-col">
              {fb('priority').visible && (
              <div className="ab-form-group">
                <label className="ab-label">{fb('priority').label || 'Priority'}{fb('priority').required ? ' *' : ''}</label>
                <select
                  className="ab-select"
                  value={form.priorityId || ''}
                  onChange={(e) => setForm({ ...form, priorityId: e.target.value || undefined })}
                  disabled={fb('priority').readOnly}
                  required={fb('priority').required}
                >
                  <option value="">None</option>
                  {(fb('priority').options || priorities).map((priority: any) => (
                    <option key={priority.id || priority.value} value={priority.id || priority.value}>
                      {priority.name || priority.label}
                    </option>
                  ))}
                </select>
                {fb('priority').warning && <span className="ab-field-warning" style={{ color: '#b45309', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>&#9888; {fb('priority').warning}</span>}
              </div>
              )}

              {fb('assignee').visible && (
              <div className="ab-form-group">
                <label className="ab-label">{fb('assignee').label || 'Assignee'}{fb('assignee').required ? ' *' : ''}</label>
                <select
                  className="ab-select"
                  value={form.assigneeId || ''}
                  onChange={(e) => setForm({ ...form, assigneeId: e.target.value || undefined })}
                  disabled={fb('assignee').readOnly}
                  required={fb('assignee').required}
                >
                  <option value="">Unassigned</option>
                  {projectUsers.map((user: any) => (
                    <option key={user.id} value={user.id}>
                      {user.displayName || user.email}
                    </option>
                  ))}
                </select>
                {fb('assignee').warning && <span className="ab-field-warning" style={{ color: '#b45309', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>&#9888; {fb('assignee').warning}</span>}
              </div>
              )}

              {fb('reporter').visible && (
              <div className="ab-form-group">
                <label className="ab-label">{fb('reporter').label || 'Reporter'}{fb('reporter').required ? ' *' : ''}</label>
                <select
                  className="ab-select"
                  value={form.reporterId || ''}
                  onChange={(e) => setForm({ ...form, reporterId: e.target.value || undefined })}
                  disabled={fb('reporter').readOnly}
                  required={fb('reporter').required}
                >
                  <option value="">Auto-assigned</option>
                  {projectUsers.map((user: any) => (
                    <option key={user.id} value={user.id}>
                      {user.displayName || user.email}
                    </option>
                  ))}
                </select>
                {fb('reporter').warning && <span className="ab-field-warning" style={{ color: '#b45309', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>&#9888; {fb('reporter').warning}</span>}
              </div>
              )}

              {fb('sprint').visible && (
              <div className="ab-form-group">
                <label className="ab-label">{fb('sprint').label || 'Sprint'}{fb('sprint').required ? ' *' : ''}</label>
                <select
                  className="ab-select"
                  value={form.sprintId || ''}
                  onChange={(e) => setForm({ ...form, sprintId: e.target.value || undefined })}
                  disabled={fb('sprint').readOnly}
                  required={fb('sprint').required}
                >
                  <option value="">No Sprint</option>
                  {(sprints as any[]).map((sprint: any) => (
                    <option key={sprint.id} value={sprint.id}>
                      {sprint.name} ({sprint.state})
                    </option>
                  ))}
                </select>
                {fb('sprint').warning && <span className="ab-field-warning" style={{ color: '#b45309', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>&#9888; {fb('sprint').warning}</span>}
              </div>
              )}

              {fb('epicLink').visible && (
              <div className="ab-form-group">
                <label className="ab-label">{fb('epicLink').label || 'Epic Link'}{fb('epicLink').required ? ' *' : ''}</label>
                <input
                  type="text"
                  className="ab-input"
                  value={form.epicId || ''}
                  onChange={(e) => setForm({ ...form, epicId: e.target.value || undefined })}
                  placeholder="PROJ-Epic-123"
                  readOnly={fb('epicLink').readOnly}
                  required={fb('epicLink').required}
                />
                {fb('epicLink').warning && <span className="ab-field-warning" style={{ color: '#b45309', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>&#9888; {fb('epicLink').warning}</span>}
              </div>
              )}

              {fb('parentIssue').visible && (
              <div className="ab-form-group">
                <label className="ab-label">{fb('parentIssue').label || 'Parent Issue'}{fb('parentIssue').required ? ' *' : ''}</label>
                <input
                  type="text"
                  className="ab-input"
                  value={form.parentId || ''}
                  onChange={(e) => setForm({ ...form, parentId: e.target.value || undefined })}
                  placeholder="PROJ-123"
                  readOnly={fb('parentIssue').readOnly}
                  required={fb('parentIssue').required}
                />
                {fb('parentIssue').warning && <span className="ab-field-warning" style={{ color: '#b45309', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>&#9888; {fb('parentIssue').warning}</span>}
              </div>
              )}

              {fb('fixVersions').visible && (
              <div className="ab-form-group">
                <label className="ab-label">{fb('fixVersions').label || 'Fix Version(s)'}{fb('fixVersions').required ? ' *' : ''}</label>
                <select
                  className="ab-select"
                  multiple
                  value={form.fixVersionIds || []}
                  onChange={(e) => {
                    const selected = Array.from(e.target.selectedOptions, opt => opt.value);
                    setForm({ ...form, fixVersionIds: selected });
                  }}
                  style={{ minHeight: '80px' }}
                  disabled={fb('fixVersions').readOnly}
                  required={fb('fixVersions').required}
                >
                  {(versions as any[]).filter((v: any) => !v.archived).map((version: any) => (
                    <option key={version.id} value={version.id}>
                      {version.name}
                    </option>
                  ))}
                </select>
                {fb('fixVersions').warning && <span className="ab-field-warning" style={{ color: '#b45309', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>&#9888; {fb('fixVersions').warning}</span>}
              </div>
              )}

              {fb('affectsVersions').visible && (
              <div className="ab-form-group">
                <label className="ab-label">{fb('affectsVersions').label || 'Affects Version(s)'}{fb('affectsVersions').required ? ' *' : ''}</label>
                <select
                  className="ab-select"
                  multiple
                  value={form.affectsVersionIds || []}
                  onChange={(e) => {
                    const selected = Array.from(e.target.selectedOptions, opt => opt.value);
                    setForm({ ...form, affectsVersionIds: selected });
                  }}
                  style={{ minHeight: '80px' }}
                  disabled={fb('affectsVersions').readOnly}
                  required={fb('affectsVersions').required}
                >
                  {(versions as any[]).filter((v: any) => !v.archived).map((version: any) => (
                    <option key={version.id} value={version.id}>
                      {version.name}
                    </option>
                  ))}
                </select>
                {fb('affectsVersions').warning && <span className="ab-field-warning" style={{ color: '#b45309', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>&#9888; {fb('affectsVersions').warning}</span>}
              </div>
              )}

              {fb('components').visible && (
              <div className="ab-form-group">
                <label className="ab-label">{fb('components').label || 'Components'}{fb('components').required ? ' *' : ''}</label>
                <select
                  className="ab-select"
                  multiple
                  value={form.componentIds || []}
                  onChange={(e) => {
                    const selected = Array.from(e.target.selectedOptions, opt => opt.value);
                    setForm({ ...form, componentIds: selected });
                  }}
                  style={{ minHeight: '80px' }}
                  disabled={fb('components').readOnly}
                  required={fb('components').required}
                >
                  {(components as any[]).map((component: any) => (
                    <option key={component.id} value={component.id}>
                      {component.name}
                    </option>
                  ))}
                </select>
                {fb('components').warning && <span className="ab-field-warning" style={{ color: '#b45309', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>&#9888; {fb('components').warning}</span>}
              </div>
              )}

              {fb('labels').visible && (
              <div className="ab-form-group">
                <label className="ab-label">{fb('labels').label || 'Labels'}{fb('labels').required ? ' *' : ''}</label>
                <div className="labels-input-wrapper">
                  <input
                    type="text"
                    className="ab-input labels-input"
                    value={labelInput}
                    onChange={(e) => setLabelInput(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        handleAddLabel();
                      }
                    }}
                    placeholder="Type label and press Enter"
                    readOnly={fb('labels').readOnly}
                  />
                  <button type="button" className="ab-btn ab-btn-secondary btn-sm" onClick={handleAddLabel} disabled={fb('labels').readOnly}>
                    Add
                  </button>
                </div>
                {form.labels && form.labels.length > 0 && (
                  <div className="labels-display">
                    {form.labels.map((label) => (
                      <span key={label} className="label-tag">
                        {label}
                        <button type="button" onClick={() => handleRemoveLabel(label)}>×</button>
                      </span>
                    ))}
                  </div>
                )}
                {fb('labels').warning && <span className="ab-field-warning" style={{ color: '#b45309', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>&#9888; {fb('labels').warning}</span>}
              </div>
              )}

              {fb('storyPoints').visible && (
              <div className="ab-form-group">
                <label className="ab-label">{fb('storyPoints').label || 'Story Points'}{fb('storyPoints').required ? ' *' : ''}</label>
                <input
                  type="number"
                  className="ab-input"
                  value={form.storyPoints || ''}
                  onChange={(e) => setForm({ ...form, storyPoints: e.target.value ? parseInt(e.target.value) : undefined })}
                  placeholder="e.g., 5"
                  min="0"
                  readOnly={fb('storyPoints').readOnly}
                  required={fb('storyPoints').required}
                />
                {fb('storyPoints').warning && <span className="ab-field-warning" style={{ color: '#b45309', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>&#9888; {fb('storyPoints').warning}</span>}
              </div>
              )}

              {fb('originalEstimate').visible && (
              <div className="ab-form-group">
                <label className="ab-label">{fb('originalEstimate').label || 'Original Estimate'}{fb('originalEstimate').required ? ' *' : ''}</label>
                <input
                  type="text"
                  className="ab-input"
                  value={form.originalEstimateSeconds ? `${Math.floor(form.originalEstimateSeconds / 3600)}h` : ''}
                  onChange={(e) => {
                    setForm({ ...form, originalEstimateSeconds: parseTimeToSeconds(e.target.value) });
                  }}
                  placeholder="e.g., 4h"
                  readOnly={fb('originalEstimate').readOnly}
                  required={fb('originalEstimate').required}
                />
                {fb('originalEstimate').warning && <span className="ab-field-warning" style={{ color: '#b45309', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>&#9888; {fb('originalEstimate').warning}</span>}
              </div>
              )}

              {fb('remainingEstimate').visible && (
              <div className="ab-form-group">
                <label className="ab-label">{fb('remainingEstimate').label || 'Remaining Estimate'}{fb('remainingEstimate').required ? ' *' : ''}</label>
                <input
                  type="text"
                  className="ab-input"
                  value={form.remainingEstimateSeconds ? `${Math.floor(form.remainingEstimateSeconds / 3600)}h` : ''}
                  onChange={(e) => {
                    setForm({ ...form, remainingEstimateSeconds: parseTimeToSeconds(e.target.value) });
                  }}
                  placeholder="e.g., 4h"
                  readOnly={fb('remainingEstimate').readOnly}
                  required={fb('remainingEstimate').required}
                />
                {fb('remainingEstimate').warning && <span className="ab-field-warning" style={{ color: '#b45309', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>&#9888; {fb('remainingEstimate').warning}</span>}
              </div>
              )}

              {fb('dueDate').visible && (
              <div className="ab-form-group">
                <label className="ab-label">{fb('dueDate').label || 'Due Date'}{fb('dueDate').required ? ' *' : ''}</label>
                <input
                  type="date"
                  className="ab-input"
                  value={form.dueDate || ''}
                  onChange={(e) => setForm({ ...form, dueDate: e.target.value || undefined })}
                  readOnly={fb('dueDate').readOnly}
                  required={fb('dueDate').required}
                />
                {fb('dueDate').warning && <span className="ab-field-warning" style={{ color: '#b45309', fontSize: '0.75rem', display: 'block', marginTop: '0.25rem' }}>&#9888; {fb('dueDate').warning}</span>}
              </div>
              )}
            </div>
          </div>

          {/* Linked Issues Section */}
          <div className="linked-issues-section">
            <div className="linked-issues-header">
              <label className="ab-label">Linked Issues</label>
              <button type="button" className="ab-btn ab-btn-secondary btn-sm" onClick={() => {
                const key = prompt('Enter issue key (e.g., PROJ-123):');
                if (key) {
                  setForm({
                    ...form,
                    linkedIssues: [...form.linkedIssues, { targetIssueKey: key.toUpperCase(), linkType: 'blocks' }],
                  });
                }
              }}>
                + Add Link
              </button>
            </div>
            {form.linkedIssues.length > 0 ? (
              <div className="linked-issues-list">
                {form.linkedIssues.map((link, index) => (
                  <div key={index} className="linked-issue-item">
                    <span className="link-type-badge">{getLinkTypeLabel(link.linkType)}</span>
                    <span className="link-arrow">→</span>
                    <span className="linked-issue-key">{link.targetIssueKey}</span>
                    <button
                      type="button"
                      className="ab-btn-icon btn-sm"
                      onClick={() => handleRemoveLinkedIssue(index)}
                    >
                      ×
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              <div className="linked-issues-empty">No linked issues</div>
            )}
          </div>

          <div className="ab-modal-footer">
            <button type="button" className="ab-btn ab-btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button
              type="submit"
              className="ab-btn ab-btn-primary"
              disabled={createMutation.isPending}
            >
              {createMutation.isPending ? 'Creating...' : 'Create Issue'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}