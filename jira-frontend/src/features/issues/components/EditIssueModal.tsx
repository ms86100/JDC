import { useState, useEffect } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { issueApi, IssueResponse } from '../../../api/issueApi';
import { labelApi, LabelResponse } from '../../../api/labelApi';
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
  const [form, setForm] = useState({
    title: (issue as { title?: string }).title || '',
    description: (issue as { description?: string }).description || '',
    status: (issue as { status?: string }).status || 'To Do',
    priority: (issue as { priority?: string }).priority || 'Medium',
    issueType: (issue as { issueType?: string }).issueType || 'Task',
  });
  const [newLabel, setNewLabel] = useState('');
  const [labels, setLabels] = useState<LabelResponse[]>([]);

  useEffect(() => {
    labelApi.getAll(issueId).then(res => setLabels(res.data || []));
  }, [issueId]);

  const updateMutation = useMutation({
    mutationFn: (data: Partial<any>) => issueApi.update(issueId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['issue', issueId] });
      queryClient.invalidateQueries({ queryKey: ['issues'] });
      onSuccess();
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
    updateMutation.mutate(form);
  };

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
                      value={form.priority}
                      onChange={(e) => setForm({ ...form, priority: e.target.value })}
                    >
                      <option value="Highest">🔴 Highest</option>
                      <option value="High">🟠 High</option>
                      <option value="Medium">🟡 Medium</option>
                      <option value="Low">🟢 Low</option>
                      <option value="Lowest">⚪ Lowest</option>
                    </select>
                  </div>
                )}

                {isFieldVisible('assignee') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Assignee</label>
                    <input
                      type="text"
                      className="ab-input"
                      placeholder="Type to search..."
                      readOnly
                    />
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
                    <input type="date" className="ab-input" />
                  </div>
                )}

                {isFieldVisible('labels') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Labels</label>
                    <input
                      type="text"
                      className="ab-input"
                      placeholder="Start typing to get a list..."
                    />
                  </div>
                )}

                {isFieldVisible('sprint') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Sprint</label>
                    <select className="ab-select">
                      <option value="">Backlog</option>
                      <option value="sprint1">PROJ Sprint 1</option>
                    </select>
                  </div>
                )}

                {isFieldVisible('epicLink') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Epic Link</label>
                    <select className="ab-select">
                      <option value="">None</option>
                    </select>
                  </div>
                )}

                {isFieldVisible('storyPoints') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Story Points</label>
                    <input type="number" className="ab-input" placeholder="-" />
                  </div>
                )}

                {isFieldVisible('components') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Component/s</label>
                    <input
                      type="text"
                      className="ab-input"
                      placeholder="Start typing to get a list..."
                    />
                  </div>
                )}

                {isFieldVisible('affectsVersions') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Affects Version/s</label>
                    <input
                      type="text"
                      className="ab-input"
                      placeholder="Start typing to get a list..."
                    />
                  </div>
                )}

                {isFieldVisible('fixVersions') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Fix Version/s</label>
                    <input
                      type="text"
                      className="ab-input"
                      placeholder="Start typing to get a list..."
                    />
                  </div>
                )}

                {isFieldVisible('linkedIssues') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Linked Issues</label>
                    <div className="ab-linked-issues-row">
                      <select className="ab-select ab-select-sm" style={{ width: 140 }}>
                        <option>blocks</option>
                        <option>is blocked by</option>
                        <option>relates to</option>
                        <option>duplicates</option>
                        <option>is duplicated by</option>
                      </select>
                      <input
                        type="text"
                        className="ab-input"
                        placeholder="Issue key or name..."
                        style={{ flex: 1 }}
                      />
                      <button type="button" className="ab-btn ab-btn-secondary ab-btn-sm">Add</button>
                    </div>
                  </div>
                )}

                {isFieldVisible('securityLevel') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Security Level</label>
                    <select className="ab-select">
                      <option>None</option>
                    </select>
                  </div>
                )}

                {isFieldVisible('timeTracking') && (
                  <div className="ab-form-group">
                    <label className="ab-label">Time Tracking</label>
                    <div className="ab-time-tracking-row">
                      <div className="ab-time-field">
                        <label className="ab-time-label">Remaining Estimate</label>
                        <input type="text" className="ab-input" placeholder="-" />
                      </div>
                      <div className="ab-time-field">
                        <label className="ab-time-label">Time Spent</label>
                        <input type="text" className="ab-input" placeholder="-" />
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