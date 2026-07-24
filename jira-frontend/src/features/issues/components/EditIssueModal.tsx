import { useState, useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { issueApi, IssueResponse, IssuePriority, securityLevelApi } from '../../../api/issueApi';
import { versionApi } from '../../../api/versionApi';
import { componentApi } from '../../../api/componentApi';
import { labelApi, LabelResponse } from '../../../api/labelApi';
import { commentApi } from '../../../api/commentApi';
import { issueLinkApi } from '../../../api/issueLinkApi';
import { sprintApi } from '../../../api/sprintApi';
import { resolveIssueByKey } from '../../../api/issueLookup';
import apiClient from '../../../api/axiosClient';
import ConfigureFieldsPopover from './ConfigureFieldsPopover';
import './EditIssueModal.css';

interface EditIssueModalProps {
  issue: IssueResponse | Record<string, unknown>;
  onClose: () => void;
  onSuccess: () => void;
}

const ISSUE_STATUSES = ['To Do', 'In Progress', 'In Review', 'Done', 'Blocked'];

// Available fields for configure
export const ALL_CONFIGURABLE_FIELDS = [
  // System fields
  { id: 'summary', label: 'Summary', type: 'system' },
  { id: 'description', label: 'Description', type: 'system' },
  { id: 'fixVersions', label: 'Fix Version/s', type: 'system' },
  { id: 'environment', label: 'Environment', type: 'system' },
  { id: 'components', label: 'Component/s', type: 'system' },
  { id: 'linkedIssues', label: 'Linked Issues', type: 'system' },
  { id: 'priority', label: 'Priority', type: 'system' },
  { id: 'assignee', label: 'Assignee', type: 'system' },
  { id: 'reporter', label: 'Reporter', type: 'system' },
  { id: 'dueDate', label: 'Due Date', type: 'system' },
  { id: 'labels', label: 'Labels', type: 'custom' },
  { id: 'epicLink', label: 'Epic Link', type: 'custom' },
  { id: 'sprint', label: 'Sprint', type: 'custom' },
  { id: 'storyPoints', label: 'Story Points', type: 'custom' },
  { id: 'timeTracking', label: 'Time Tracking', type: 'system' },
  { id: 'securityLevel', label: 'Security Level', type: 'system' },
  { id: 'attachment', label: 'Attachment', type: 'system' },
  { id: 'affectsVersions', label: 'Affects Version/s', type: 'system' },
];

const DEFAULT_VISIBLE_FIELDS = [
  'summary', 'description', 'fixVersions', 'environment',
  'components', 'linkedIssues', 'priority', 'assignee',
  'reporter', 'dueDate', 'labels', 'epicLink', 'sprint',
  'storyPoints', 'timeTracking', 'securityLevel',
];

export default function EditIssueModal({ issue, onClose, onSuccess }: EditIssueModalProps) {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'details' | 'labels' | 'links'>('details');
  const [showConfigureFields, setShowConfigureFields] = useState(false);
  const [visibleFields, setVisibleFields] = useState<Set<string>>(
    new Set(DEFAULT_VISIBLE_FIELDS)
  );
  const issueId = (issue as { id: string }).id;
  const issueData = issue as IssueResponse;
  const projectId = issueData.projectId || '';

  const [form, setForm] = useState({
    title: issueData.title || '',
    description: issueData.description || '',
    environment: issueData.environment || '',
    priorityId: issueData.priorityId || '',
    statusId: issueData.statusId || '',
    issueTypeId: issueData.issueTypeId || '',
    assigneeId: issueData.assigneeId || '',
    dueDate: issueData.dueDate ? String(issueData.dueDate).split('T')[0] : '',
    storyPoints: issueData.storyPoints,
    epicId: issueData.epicId || '',
    securityLevelId: issueData.securityLevelId || '',
    componentIds: (issueData.componentIds as string[]) || [],
    fixVersionIds: (issueData.fixVersionIds as string[]) || [],
    affectsVersionIds: (issueData.affectsVersionIds as string[]) || [],
    remainingEstimateMinutes: issueData.remainingEstimate
      ? Math.round(issueData.remainingEstimate / 60)
      : undefined,
    timeSpentMinutes: issueData.timeSpent ? Math.round(issueData.timeSpent / 60) : undefined,
  });
  const [editComment, setEditComment] = useState('');
  const [linkedIssueKey, setLinkedIssueKey] = useState('');
  const [linkType, setLinkType] = useState('blocks');
  const [saveError, setSaveError] = useState<string | null>(null);

  const { data: priorities = [] } = useQuery<IssuePriority[]>({
    queryKey: ['priorities'],
    queryFn: async () => {
      const response = await issueApi.getPriorities();
      return Array.isArray(response.data) ? response.data : [];
    },
  });

  const { data: projectUsers = [] } = useQuery({
    queryKey: ['projectUsers', projectId],
    queryFn: async () => {
      const response = await apiClient.get<{ id: string; userName?: string; displayName?: string }[]>(
        `/api/projects/${projectId}/members`
      );
      return Array.isArray(response.data) ? response.data : [];
    },
    enabled: !!projectId,
  });

  const { data: versions = [] } = useQuery({
    queryKey: ['project-versions', projectId],
    queryFn: async () => {
      const data = await versionApi.getByProject(projectId);
      return Array.isArray(data) ? data : [];
    },
    enabled: !!projectId,
  });

  const { data: components = [] } = useQuery({
    queryKey: ['project-components', projectId],
    queryFn: async () => {
      const data = await componentApi.getByProject(projectId);
      return Array.isArray(data) ? data : [];
    },
    enabled: !!projectId,
  });

  const { data: securityLevels = [] } = useQuery({
    queryKey: ['securityLevels'],
    queryFn: async () => {
      const response = await securityLevelApi.getAll();
      return Array.isArray(response.data) ? response.data : [];
    },
  });

  const { data: sprints = [] } = useQuery({
    queryKey: ['sprints', projectId],
    queryFn: async () => {
      const data = await sprintApi.getAll(projectId);
      return Array.isArray(data) ? data : [];
    },
    enabled: !!projectId,
  });
  const [newLabel, setNewLabel] = useState('');
  const [labels, setLabels] = useState<LabelResponse[]>([]);
  const [sprintId, setSprintId] = useState(issueData.sprintId || '');

  useEffect(() => {
    labelApi.getAll(issueId).then(res => setLabels(res.data || []));
  }, [issueId]);

  const updateMutation = useMutation({
    mutationFn: async (data: Record<string, unknown>) => {
      await apiClient.put(`/api/issues/${issueId}`, data);
      // Post-save operations (best-effort, won't throw)
      const originalSprint = issueData.sprintId || '';
      const newSprint = sprintId || '';
      if (originalSprint && originalSprint !== newSprint) await sprintApi.removeIssue(originalSprint, issueId).catch(() => {});
      if (newSprint && newSprint !== originalSprint) await sprintApi.addIssue(newSprint, issueId).catch(() => {});
      if (editComment.trim()) await commentApi.create({ issueId, content: editComment.trim() }).catch(() => {});
    },
    onSettled: () => {
      setSaveError(null);
      queryClient.invalidateQueries({ queryKey: ['issue', issueId] });
      queryClient.invalidateQueries({ queryKey: ['issues'] });
      queryClient.invalidateQueries({ queryKey: ['comments', issueId] });
      queryClient.invalidateQueries({ queryKey: ['issue-links-outward', issueId] });
      if (typeof onSuccess === 'function') onSuccess();
    },
  });

  const addLabelMutation = useMutation({
    mutationFn: (name: string) => labelApi.add(issueId, name),
    onSuccess: () => {
      labelApi.getAll(issueId).then(res => setLabels(res.data || []));
    },
  });

  const removeLabelMutation = useMutation({
    mutationFn: (labelName: string) => labelApi.remove(issue.id, labelName),
    onSuccess: () => {
      labelApi.getAll(issue.id).then(res => setLabels(res.data || []));
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setSaveError(null);
    const payload: Record<string, unknown> = {};
    if (form.title) payload.title = form.title;
    if (form.description != null) payload.description = form.description;
    if (form.environment) payload.environment = form.environment;
    if (form.priorityId) payload.priorityId = form.priorityId;
    if (form.statusId) payload.statusId = form.statusId;
    if (form.issueTypeId) payload.issueTypeId = form.issueTypeId;
    if (form.assigneeId) payload.assigneeId = form.assigneeId;
    if (form.dueDate) payload.dueDate = form.dueDate;
    if (form.storyPoints != null) payload.storyPoints = form.storyPoints;
    if (form.epicId) payload.epicId = form.epicId;
    if (form.securityLevelId) payload.securityLevelId = form.securityLevelId;
    if (form.componentIds?.length) payload.componentIds = form.componentIds;
    if (form.fixVersionIds?.length) payload.fixVersionIds = form.fixVersionIds;
    if (form.affectsVersionIds?.length) payload.affectsVersionIds = form.affectsVersionIds;
    if (form.remainingEstimateMinutes != null) payload.remainingEstimateSeconds = form.remainingEstimateMinutes * 60;
    if (form.timeSpentMinutes != null) payload.timeSpentSeconds = form.timeSpentMinutes * 60;
    updateMutation.mutate(payload);
  };

  const toggleId = (list: string[], id: string) =>
    list.includes(id) ? list.filter((x) => x !== id) : [...list, id];

  const handleAddLabel = () => {
    if (newLabel.trim()) {
      addLabelMutation.mutate(newLabel.trim().toLowerCase());
      setNewLabel('');
    }
  };

  const handleApplyFields = (fields: Set<string>) => {
    setVisibleFields(fields);
    setShowConfigureFields(false);
  };

  const isFieldVisible = (fieldId: string) => visibleFields.has(fieldId);

  return (
    <div className="ab-modal-overlay" onClick={onClose}>
      <div className="ab-modal" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="ab-modal-header">
          <div className="ab-modal-title-row">
            <span className="ab-issue-key">{(issue as { issueKey?: string }).issueKey}</span>
            <h2>Edit Issue</h2>
          </div>
          <div className="ab-modal-header-right">
            <div className="ab-configure-fields-wrapper">
              <button
                className="ab-configure-btn"
                title="Configure fields"
                onClick={() => setShowConfigureFields(!showConfigureFields)}
              >
                <span className="ab-configure-icon">⚙</span>
                <span className="ab-configure-label">Configure Fields</span>
                <span className="ab-configure-caret">▾</span>
              </button>
              {showConfigureFields && (
                <ConfigureFieldsPopover
                  visibleFields={visibleFields}
                  onApply={handleApplyFields}
                  onCancel={() => setShowConfigureFields(false)}
                />
              )}
            </div>
            <button className="ab-modal-close" onClick={onClose}>×</button>
          </div>
        </div>

        {/* Tabs */}
        <div className="ab-modal-tabs">
          <button
            className={`ab-modal-tab ${activeTab === 'details' ? 'ab-modal-tab-active' : ''}`}
            onClick={() => setActiveTab('details')}
          >
            Details
          </button>
          <button
            className={`ab-modal-tab ${activeTab === 'labels' ? 'ab-modal-tab-active' : ''}`}
            onClick={() => setActiveTab('labels')}
          >
            Labels ({labels.length})
          </button>
          <button
            className={`ab-modal-tab ${activeTab === 'links' ? 'ab-modal-tab-active' : ''}`}
            onClick={() => setActiveTab('links')}
          >
            Links
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="ab-modal-body">
            {activeTab === 'details' && (
              <div className="ab-edit-details">
                {isFieldVisible('summary') && (
                  <div className="ab-form-group">
                    <label className="ab-label">
                      Summary <span className="ab-required">*</span>
                    </label>
                    <input
                      type="text"
                      className="ab-input"
                      value={form.title}
                      onChange={(e) => setForm({ ...form, title: e.target.value })}
                      required
                    />
                  </div>
                )}

                {isFieldVisible('description') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Description</label>
                    <textarea
                      className="ab-textarea"
                      value={form.description}
                      onChange={(e) => setForm({ ...form, description: e.target.value })}
                      placeholder="Add a description..."
                      rows={5}
                    />
                  </div>
                )}

                {isFieldVisible('priority') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Priority</label>
                    <select
                      className="ab-select"
                      value={form.priorityId}
                      onChange={(e) => setForm({ ...form, priorityId: e.target.value })}
                    >
                      <option value="">Select priority</option>
                      {priorities.map((p) => (
                        <option key={p.id} value={p.id}>{p.name}</option>
                      ))}
                    </select>
                  </div>
                )}

                {isFieldVisible('assignee') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Assignee</label>
                    <select
                      className="ab-select"
                      value={form.assigneeId}
                      onChange={(e) => setForm({ ...form, assigneeId: e.target.value })}
                    >
                      <option value="">Unassigned</option>
                      {projectUsers.map((u) => (
                        <option key={u.id} value={u.id}>
                          {u.displayName || u.userName || u.id}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                {isFieldVisible('reporter') && (
                  <div className="ab-form-group">
                    <label className="ab-label">
                      Reporter
                      <button className="ab-help-icon" title="Reporter help">?</button>
                    </label>
                    <input
                      type="text"
                      className="ab-input"
                      defaultValue="Sagar Sharma"
                      readOnly
                    />
                  </div>
                )}

                {isFieldVisible('dueDate') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Due Date</label>
                    <input
                      type="date"
                      className="ab-input"
                      value={form.dueDate}
                      onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
                    />
                  </div>
                )}

                {isFieldVisible('labels') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Labels</label>
                    <div className="ab-label-input-row">
                      <input
                        type="text"
                        className="ab-input"
                        value={newLabel}
                        onChange={(e) => setNewLabel(e.target.value)}
                        placeholder="Add label..."
                        onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), handleAddLabel())}
                      />
                      <button type="button" className="ab-btn ab-btn-secondary ab-btn-sm" onClick={handleAddLabel}>
                        Add
                      </button>
                    </div>
                    <div className="ab-labels-list" style={{ marginTop: 8 }}>
                      {labels.map((l) => (
                        <span key={l.id} className="ab-label-tag">
                          {l.name}
                          <button type="button" onClick={() => removeLabelMutation.mutate(l.name)}>×</button>
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                {isFieldVisible('sprint') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Sprint</label>
                    <select
                      className="ab-select"
                      value={sprintId}
                      onChange={(e) => setSprintId(e.target.value)}
                    >
                      <option value="">Backlog</option>
                      {(sprints as any[]).map((s: any) => (
                        <option key={s.id} value={s.id}>
                          {s.name} ({s.status})
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                {isFieldVisible('epicLink') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Epic Link</label>
                    <input
                      type="text"
                      className="ab-input"
                      value={form.epicId}
                      onChange={(e) => setForm({ ...form, epicId: e.target.value })}
                      placeholder="Epic issue id or key"
                    />
                  </div>
                )}

                {isFieldVisible('storyPoints') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Story Points</label>
                    <input
                      type="number"
                      className="ab-input"
                      placeholder="-"
                      value={form.storyPoints ?? ''}
                      onChange={(e) =>
                        setForm({
                          ...form,
                          storyPoints: e.target.value === '' ? undefined : Number(e.target.value),
                        })
                      }
                    />
                  </div>
                )}

                {isFieldVisible('components') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Component/s</label>
                    <select
                      multiple
                      className="ab-select"
                      value={form.componentIds}
                      onChange={(e) => {
                        const selected = Array.from(e.target.selectedOptions, (o) => o.value);
                        setForm({ ...form, componentIds: selected });
                      }}
                    >
                      {components.map((c: { id: string; name: string }) => (
                        <option key={c.id} value={c.id}>{c.name}</option>
                      ))}
                    </select>
                  </div>
                )}

                {isFieldVisible('affectsVersions') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Affects Version/s</label>
                    <select
                      multiple
                      className="ab-select"
                      value={form.affectsVersionIds}
                      onChange={(e) => {
                        const selected = Array.from(e.target.selectedOptions, (o) => o.value);
                        setForm({ ...form, affectsVersionIds: selected });
                      }}
                    >
                      {versions.map((v: { id: string; name: string }) => (
                        <option key={v.id} value={v.id}>{v.name}</option>
                      ))}
                    </select>
                  </div>
                )}

                {isFieldVisible('fixVersions') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Fix Version/s</label>
                    <select
                      multiple
                      className="ab-select"
                      value={form.fixVersionIds}
                      onChange={(e) => {
                        const selected = Array.from(e.target.selectedOptions, (o) => o.value);
                        setForm({ ...form, fixVersionIds: selected });
                      }}
                    >
                      {versions.map((v: { id: string; name: string }) => (
                        <option key={v.id} value={v.id}>{v.name}</option>
                      ))}
                    </select>
                  </div>
                )}

                {isFieldVisible('linkedIssues') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Linked Issues</label>
                    <div className="ab-linked-issues-row">
                      <select
                        className="ab-select ab-select-sm"
                        style={{ width: 140 }}
                        value={linkType}
                        onChange={(e) => setLinkType(e.target.value)}
                      >
                        <option value="blocks">blocks</option>
                        <option value="is blocked by">is blocked by</option>
                        <option value="relates to">relates to</option>
                        <option value="duplicates">duplicates</option>
                        <option value="is duplicated by">is duplicated by</option>
                      </select>
                      <input
                        type="text"
                        className="ab-input"
                        placeholder="Issue key or name..."
                        style={{ flex: 1 }}
                        value={linkedIssueKey}
                        onChange={(e) => setLinkedIssueKey(e.target.value)}
                      />
                    </div>
                    <p className="ab-hint" style={{ marginTop: 4, fontSize: 12 }}>
                      Link is created when you click Update (with issue key filled in).
                    </p>
                  </div>
                )}

                {isFieldVisible('securityLevel') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Security Level</label>
                    <select
                      className="ab-select"
                      value={form.securityLevelId}
                      onChange={(e) => setForm({ ...form, securityLevelId: e.target.value })}
                    >
                      <option value="">None</option>
                      {securityLevels.map((s: { id: string; name: string }) => (
                        <option key={s.id} value={s.id}>{s.name}</option>
                      ))}
                    </select>
                  </div>
                )}

                {isFieldVisible('timeTracking') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Time Tracking</label>
                    <div className="ab-time-tracking-row">
                      <div className="ab-time-field">
                        <label className="ab-time-label">Remaining Estimate (minutes)</label>
                        <input
                          type="number"
                          className="ab-input"
                          value={form.remainingEstimateMinutes ?? ''}
                          onChange={(e) =>
                            setForm({
                              ...form,
                              remainingEstimateMinutes:
                                e.target.value === '' ? undefined : Number(e.target.value),
                            })
                          }
                        />
                      </div>
                      <div className="ab-time-field">
                        <label className="ab-time-label">Time Spent (minutes)</label>
                        <input
                          type="number"
                          className="ab-input"
                          value={form.timeSpentMinutes ?? ''}
                          onChange={(e) =>
                            setForm({
                              ...form,
                              timeSpentMinutes:
                                e.target.value === '' ? undefined : Number(e.target.value),
                            })
                          }
                        />
                      </div>
                    </div>
                  </div>
                )}

                {isFieldVisible('attachment') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Attachment</label>
                    <div className="ab-drop-zone">
                      <span className="ab-drop-zone-text">
                        Drop files here or <a href="#" className="ab-drop-zone-link">browse</a> to attach
                      </span>
                    </div>
                  </div>
                )}

                {isFieldVisible('environment') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Environment</label>
                    <textarea
                      className="ab-textarea"
                      placeholder="Add environment details..."
                      rows={3}
                      value={form.environment}
                      onChange={(e) => setForm({ ...form, environment: e.target.value })}
                    />
                  </div>
                )}

                {/* Comment */}
                <div className="ab-form-group">
                  <label className="ab-label">Comment</label>
                  <textarea
                    className="ab-textarea"
                    placeholder="Add a comment..."
                    rows={4}
                    value={editComment}
                    onChange={(e) => setEditComment(e.target.value)}
                  />
                  <div className="ab-comment-visibility">
                    <span className="ab-visibility-label">Viewable by</span>
                    <button className="ab-visibility-btn">All Users</button>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'labels' && (
              <div className="ab-edit-labels">
                <div className="ab-form-group">
                  <label className="ab-label">Add Label</label>
                  <div className="ab-label-input-row">
                    <input
                      type="text"
                      className="ab-input"
                      value={newLabel}
                      onChange={(e) => setNewLabel(e.target.value)}
                      placeholder="Enter label name"
                      onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), handleAddLabel())}
                    />
                    <button
                      type="button"
                      className="ab-btn ab-btn-primary ab-btn-sm"
                      onClick={handleAddLabel}
                    >
                      Add
                    </button>
                  </div>
                </div>
                <div className="ab-labels-list">
                  {labels.map((label) => (
                    <div key={label.id} className="ab-label-item">
                      <span className="ab-label-chip">{label.name}</span>
                      <button
                        type="button"
                        className="ab-label-remove"
                        onClick={() => removeLabelMutation.mutate(label.name)}
                      >
                        ×
                      </button>
                    </div>
                  ))}
                  {labels.length === 0 && (
                    <p className="ab-no-labels">No labels assigned</p>
                  )}
                </div>
              </div>
            )}

            {activeTab === 'links' && (
              <div className="ab-edit-links">
                <p className="ab-links-info">
                  Link this issue to other issues using the Links tab on the issue detail page.
                </p>
                <div className="ab-current-links">
                  <h4>Quick Actions</h4>
                  <div className="ab-link-actions">
                    <button type="button" className="ab-btn ab-btn-secondary">
                      🔗 Add Related Issue
                    </button>
                    <button type="button" className="ab-btn ab-btn-secondary">
                      ⛔ Add Blocked By
                    </button>
                    <button type="button" className="ab-btn ab-btn-secondary">
                      📁 Set as Parent
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>

          <div className="ab-modal-footer">
            {saveError && (
              <p className="ab-save-error" style={{ color: '#de350b', flex: 1, margin: 0 }}>
                {saveError}
              </p>
            )}
            <button type="button" className="ab-btn ab-btn-secondary" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="ab-btn ab-btn-primary" disabled={updateMutation.isPending}>
              {updateMutation.isPending ? 'Saving...' : 'Update'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}