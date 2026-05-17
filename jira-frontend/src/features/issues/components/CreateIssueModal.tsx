import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { projectApi, ProjectResponse } from '../../../api/projectApi';
import { issueApi, CreateIssueRequest, IssueType, IssuePriority } from '../../../api/issueApi';
import apiClient from '../../../api/axiosClient';
import './CreateIssueModal.css';

interface CreateIssueModalProps {
  onClose: () => void;
  onSuccess?: () => void;
  projectId?: string;
  projectKey?: string;
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

export default function CreateIssueModal({ onClose, onSuccess, projectId: propProjectId, projectKey: propProjectKey }: CreateIssueModalProps) {
  const [form, setForm] = useState<CreateIssueRequest & { linkedIssues: LinkedIssue[] }>({
    projectId: propProjectId || '',
    title: '',
    description: '',
    issueTypeId: '',
    priorityId: '',
    assigneeId: '',
    reporterId: '',
    sprintId: '',
    epicId: '',
    parentId: '',
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
      return response.data || [];
    },
    enabled: !!form.projectId,
  });

  // Versions
  const { data: versions = [] } = useQuery({
    queryKey: ['versions', form.projectId],
    queryFn: async () => {
      if (!form.projectId) return [];
      const response = await projectApi.getVersions(form.projectId);
      return response.data || [];
    },
    enabled: !!form.projectId,
  });

  // Components
  const { data: components = [] } = useQuery({
    queryKey: ['components', form.projectId],
    queryFn: async () => {
      if (!form.projectId) return [];
      const response = await projectApi.getComponents(form.projectId);
      return response.data || [];
    },
    enabled: !!form.projectId,
  });

  // Create Mutation
  const createMutation = useMutation({
    mutationFn: async (data: CreateIssueRequest & { linkedIssues: LinkedIssue[] }) => {
      const { linkedIssues, ...issueData } = data;
      const response = await issueApi.create(issueData as CreateIssueRequest);

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
      alert('Please fill in required fields');
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

              <div className="ab-form-group">
                <label className="ab-label">Description</label>
                <textarea
                  className="ab-textarea"
                  value={form.description || ''}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  placeholder="Detailed description of the issue"
                  rows={4}
                />
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Environment</label>
                <textarea
                  className="ab-textarea"
                  value={form.environment || ''}
                  onChange={(e) => setForm({ ...form, environment: e.target.value })}
                  placeholder="Environment where the issue was found"
                  rows={2}
                />
              </div>
            </div>

            {/* RIGHT COLUMN - Additional fields */}
            <div className="form-column right-col">
              <div className="ab-form-group">
                <label className="ab-label">Priority</label>
                <select
                  className="ab-select"
                  value={form.priorityId || ''}
                  onChange={(e) => setForm({ ...form, priorityId: e.target.value || undefined })}
                >
                  <option value="">None</option>
                  {priorities.map((priority: IssuePriority) => (
                    <option key={priority.id} value={priority.id}>
                      {priority.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Assignee</label>
                <select
                  className="ab-select"
                  value={form.assigneeId || ''}
                  onChange={(e) => setForm({ ...form, assigneeId: e.target.value || undefined })}
                >
                  <option value="">Unassigned</option>
                  {projectUsers.map((user: any) => (
                    <option key={user.id} value={user.id}>
                      {user.displayName || user.email}
                    </option>
                  ))}
                </select>
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Reporter</label>
                <select
                  className="ab-select"
                  value={form.reporterId || ''}
                  onChange={(e) => setForm({ ...form, reporterId: e.target.value || undefined })}
                >
                  <option value="">Auto-assigned</option>
                  {projectUsers.map((user: any) => (
                    <option key={user.id} value={user.id}>
                      {user.displayName || user.email}
                    </option>
                  ))}
                </select>
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Sprint</label>
                <select
                  className="ab-select"
                  value={form.sprintId || ''}
                  onChange={(e) => setForm({ ...form, sprintId: e.target.value || undefined })}
                >
                  <option value="">No Sprint</option>
                  {(sprints as any[]).map((sprint: any) => (
                    <option key={sprint.id} value={sprint.id}>
                      {sprint.name} ({sprint.state})
                    </option>
                  ))}
                </select>
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Epic Link</label>
                <input
                  type="text"
                  className="ab-input"
                  value={form.epicId || ''}
                  onChange={(e) => setForm({ ...form, epicId: e.target.value || undefined })}
                  placeholder="PROJ-Epic-123"
                />
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Parent Issue</label>
                <input
                  type="text"
                  className="ab-input"
                  value={form.parentId || ''}
                  onChange={(e) => setForm({ ...form, parentId: e.target.value || undefined })}
                  placeholder="PROJ-123"
                />
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Fix Version(s)</label>
                <select
                  className="ab-select"
                  multiple
                  value={form.fixVersionIds || []}
                  onChange={(e) => {
                    const selected = Array.from(e.target.selectedOptions, opt => opt.value);
                    setForm({ ...form, fixVersionIds: selected });
                  }}
                  style={{ minHeight: '80px' }}
                >
                  {(versions as any[]).filter((v: any) => !v.archived).map((version: any) => (
                    <option key={version.id} value={version.id}>
                      {version.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Affects Version(s)</label>
                <select
                  className="ab-select"
                  multiple
                  value={form.affectsVersionIds || []}
                  onChange={(e) => {
                    const selected = Array.from(e.target.selectedOptions, opt => opt.value);
                    setForm({ ...form, affectsVersionIds: selected });
                  }}
                  style={{ minHeight: '80px' }}
                >
                  {(versions as any[]).filter((v: any) => !v.archived).map((version: any) => (
                    <option key={version.id} value={version.id}>
                      {version.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Components</label>
                <select
                  className="ab-select"
                  multiple
                  value={form.componentIds || []}
                  onChange={(e) => {
                    const selected = Array.from(e.target.selectedOptions, opt => opt.value);
                    setForm({ ...form, componentIds: selected });
                  }}
                  style={{ minHeight: '80px' }}
                >
                  {(components as any[]).map((component: any) => (
                    <option key={component.id} value={component.id}>
                      {component.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Labels</label>
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
                  />
                  <button type="button" className="ab-btn ab-btn-secondary btn-sm" onClick={handleAddLabel}>
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
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Story Points</label>
                <input
                  type="number"
                  className="ab-input"
                  value={form.storyPoints || ''}
                  onChange={(e) => setForm({ ...form, storyPoints: e.target.value ? parseInt(e.target.value) : undefined })}
                  placeholder="e.g., 5"
                  min="0"
                />
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Original Estimate</label>
                <input
                  type="text"
                  className="ab-input"
                  value={form.originalEstimateSeconds ? `${Math.floor(form.originalEstimateSeconds / 3600)}h` : ''}
                  onChange={(e) => {
                    setForm({ ...form, originalEstimateSeconds: parseTimeToSeconds(e.target.value) });
                  }}
                  placeholder="e.g., 4h"
                />
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Remaining Estimate</label>
                <input
                  type="text"
                  className="ab-input"
                  value={form.remainingEstimateSeconds ? `${Math.floor(form.remainingEstimateSeconds / 3600)}h` : ''}
                  onChange={(e) => {
                    setForm({ ...form, remainingEstimateSeconds: parseTimeToSeconds(e.target.value) });
                  }}
                  placeholder="e.g., 4h"
                />
              </div>

              <div className="ab-form-group">
                <label className="ab-label">Due Date</label>
                <input
                  type="date"
                  className="ab-input"
                  value={form.dueDate || ''}
                  onChange={(e) => setForm({ ...form, dueDate: e.target.value || undefined })}
                />
              </div>
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