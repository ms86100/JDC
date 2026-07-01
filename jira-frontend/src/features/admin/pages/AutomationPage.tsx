import React, { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import AdminLayout from '../components/AdminLayout';
import apiClient from '../../../api/axiosClient';
import './AutomationPage.css';

// ==================== Types ====================

interface AutomationRule {
  id: string;
  name: string;
  description?: string;
  projectId: string;
  projectName?: string;
  triggerType: string;
  conditions: AutomationCondition[];
  actions: AutomationAction[];
  isEnabled: boolean;
  ownerId: string;
  createdAt: string;
  lastTriggered?: string;
  triggerCount: number;
}

interface AutomationCondition {
  field: string;
  operator: string;
  value: string;
}

interface AutomationAction {
  type: string;
  config: Record<string, any>;
}

interface AutomationLog {
  id: string;
  ruleId: string;
  ruleName: string;
  status: 'SUCCESS' | 'FAILED' | 'PARTIAL';
  error?: string;
  triggeredAt: string;
  executionTimeMs: number;
  context: Record<string, any>;
}

interface TriggerOption {
  key: string;
  label: string;
  description: string;
  icon: string;
}

interface ActionOption {
  key: string;
  label: string;
  description: string;
  icon: string;
}

// ==================== Constants ====================

const TRIGGER_OPTIONS: TriggerOption[] = [
  { key: 'ISSUE_CREATED', label: 'Issue Created', description: 'Triggers when an issue is created', icon: '📝' },
  { key: 'ISSUE_UPDATED', label: 'Issue Updated', description: 'Triggers when an issue is updated', icon: '✏️' },
  { key: 'ISSUE_STATUS_CHANGED', label: 'Status Changed', description: 'Triggers when an issue status changes', icon: '🔄' },
  { key: 'ISSUE_ASSIGNED', label: 'Issue Assigned', description: 'Triggers when an issue is assigned', icon: '👤' },
  { key: 'ISSUE_COMMENTED', label: 'Comment Added', description: 'Triggers when a comment is added', icon: '💬' },
  { key: 'SPRINT_STARTED', label: 'Sprint Started', description: 'Triggers when a sprint starts', icon: '▶️' },
  { key: 'SPRINT_COMPLETED', label: 'Sprint Completed', description: 'Triggers when a sprint is completed', icon: '✅' },
  { key: 'EPIC_STATUS_CHANGED', label: 'Epic Status Changed', description: 'Triggers when an epic status changes', icon: '⚡' },
];

const ACTION_OPTIONS: ActionOption[] = [
  { key: 'TRANSITION_ISSUE', label: 'Transition Issue', description: 'Move issue to a different status', icon: '🔄' },
  { key: 'ASSIGN_ISSUE', label: 'Assign Issue', description: 'Assign issue to a user', icon: '👤' },
  { key: 'ADD_LABEL', label: 'Add Label', description: 'Add a label to the issue', icon: '🏷️' },
  { key: 'REMOVE_LABEL', label: 'Remove Label', description: 'Remove a label from the issue', icon: '🏷️' },
  { key: 'SEND_NOTIFICATION', label: 'Send Notification', description: 'Send a notification to users', icon: '📧' },
  { key: 'ADD_COMMENT', label: 'Add Comment', description: 'Add a comment to the issue', icon: '💬' },
  { key: 'UPDATE_FIELD', label: 'Update Field', description: 'Update an issue field', icon: '✏️' },
  { key: 'ASSIGN_TO_SPRINT', label: 'Assign to Sprint', description: 'Add issue to a sprint', icon: '📋' },
];

const CONDITION_OPERATORS = [
  { key: 'EQUALS', label: 'equals' },
  { key: 'NOT_EQUALS', label: 'does not equal' },
  { key: 'CONTAINS', label: 'contains' },
  { key: 'NOT_CONTAINS', label: 'does not contain' },
  { key: 'STARTS_WITH', label: 'starts with' },
  { key: 'ENDS_WITH', label: 'ends with' },
  { key: 'IS_EMPTY', label: 'is empty' },
  { key: 'IS_NOT_EMPTY', label: 'is not empty' },
];

const ISSUE_FIELDS = [
  { key: 'status', label: 'Status' },
  { key: 'priority', label: 'Priority' },
  { key: 'assignee', label: 'Assignee' },
  { key: 'reporter', label: 'Reporter' },
  { key: 'labels', label: 'Labels' },
  { key: 'issueType', label: 'Issue Type' },
  { key: 'sprint', label: 'Sprint' },
  { key: 'project', label: 'Project' },
  { key: 'summary', label: 'Summary' },
  { key: 'description', label: 'Description' },
  { key: 'dueDate', label: 'Due Date' },
  { key: 'components', label: 'Components' },
  { key: 'fixVersion', label: 'Fix Version' },
];

// ==================== Mock Data ====================

const MOCK_RULES: AutomationRule[] = [
  {
    id: 'rule-1',
    name: 'Auto-assign Bug to QA Lead',
    description: 'Automatically assigns any new Bug to the QA Lead',
    projectId: 'proj-1',
    projectName: 'Systems and Avionics',
    triggerType: 'ISSUE_CREATED',
    conditions: [{ field: 'issueType', operator: 'EQUALS', value: 'Bug' }],
    actions: [{ type: 'ASSIGN_ISSUE', config: { assigneeId: 'qa-lead-1' } }],
    isEnabled: true,
    ownerId: 'user-1',
    createdAt: '2026-05-01T10:00:00Z',
    lastTriggered: '2026-05-13T14:30:00Z',
    triggerCount: 47,
  },
  {
    id: 'rule-2',
    name: 'High Priority Alert',
    description: 'Send notification when a Critical priority issue is created',
    projectId: 'proj-1',
    projectName: 'Systems and Avionics',
    triggerType: 'ISSUE_CREATED',
    conditions: [{ field: 'priority', operator: 'EQUALS', value: 'Critical' }],
    actions: [{ type: 'SEND_NOTIFICATION', config: { recipients: ['project-lead'], message: 'High priority issue created' } }],
    isEnabled: true,
    ownerId: 'user-1',
    createdAt: '2026-05-02T09:00:00Z',
    lastTriggered: '2026-05-12T11:20:00Z',
    triggerCount: 12,
  },
  {
    id: 'rule-3',
    name: 'Auto-close Done Issues',
    description: 'Move issues to Closed status after 7 days in Done',
    projectId: 'proj-2',
    projectName: 'Mobile App',
    triggerType: 'ISSUE_STATUS_CHANGED',
    conditions: [{ field: 'status', operator: 'EQUALS', value: 'Done' }],
    actions: [{ type: 'TRANSITION_ISSUE', config: { status: 'Closed', delay: '7d' } }],
    isEnabled: false,
    ownerId: 'user-2',
    createdAt: '2026-04-28T15:00:00Z',
    lastTriggered: undefined,
    triggerCount: 0,
  },
];

const MOCK_LOGS: AutomationLog[] = [
  {
    id: 'log-1',
    ruleId: 'rule-1',
    ruleName: 'Auto-assign Bug to QA Lead',
    status: 'SUCCESS',
    triggeredAt: '2026-05-13T14:30:00Z',
    executionTimeMs: 245,
    context: { issueKey: 'JIRA-123', assignee: 'QA Lead' },
  },
  {
    id: 'log-2',
    ruleId: 'rule-2',
    ruleName: 'High Priority Alert',
    status: 'SUCCESS',
    triggeredAt: '2026-05-12T11:20:00Z',
    executionTimeMs: 156,
    context: { issueKey: 'JIRA-456', priority: 'Critical' },
  },
  {
    id: 'log-3',
    ruleId: 'rule-1',
    ruleName: 'Auto-assign Bug to QA Lead',
    status: 'FAILED',
    error: 'User not found: qa-lead-1',
    triggeredAt: '2026-05-12T09:15:00Z',
    executionTimeMs: 89,
    context: { issueKey: 'JIRA-789' },
  },
];

// ==================== Component ====================

export default function AutomationPage() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'rules' | 'logs' | 'settings'>('rules');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingRule, setEditingRule] = useState<AutomationRule | null>(null);
  const [selectedRule, setSelectedRule] = useState<AutomationRule | null>(null);

  // Form state
  const [ruleName, setRuleName] = useState('');
  const [ruleDescription, setRuleDescription] = useState('');
  const [ruleProject, setRuleProject] = useState('');
  const [ruleTrigger, setRuleTrigger] = useState('');
  const [ruleConditions, setRuleConditions] = useState<AutomationCondition[]>([]);
  const [ruleActions, setRuleActions] = useState<AutomationAction[]>([]);
  const [ruleEnabled, setRuleEnabled] = useState(true);

  // Filter state
  const [projectFilter, setProjectFilter] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState('');

  const filteredRules = MOCK_RULES.filter(rule => {
    if (projectFilter && rule.projectId !== projectFilter) return false;
    if (statusFilter === 'enabled' && !rule.isEnabled) return false;
    if (statusFilter === 'disabled' && rule.isEnabled) return false;
    if (searchQuery && !rule.name.toLowerCase().includes(searchQuery.toLowerCase())) return false;
    return true;
  });

  const handleOpenCreate = () => {
    setEditingRule(null);
    setRuleName('');
    setRuleDescription('');
    setRuleProject('');
    setRuleTrigger('');
    setRuleConditions([]);
    setRuleActions([]);
    setRuleEnabled(true);
    setShowCreateModal(true);
  };

  const handleOpenEdit = (rule: AutomationRule) => {
    setEditingRule(rule);
    setRuleName(rule.name);
    setRuleDescription(rule.description || '');
    setRuleProject(rule.projectId);
    setRuleTrigger(rule.triggerType);
    setRuleConditions([...rule.conditions]);
    setRuleActions([...rule.actions]);
    setRuleEnabled(rule.isEnabled);
    setShowCreateModal(true);
  };

  const handleAddCondition = () => {
    setRuleConditions([...ruleConditions, { field: '', operator: 'EQUALS', value: '' }]);
  };

  const handleRemoveCondition = (index: number) => {
    setRuleConditions(ruleConditions.filter((_, i) => i !== index));
  };

  const handleUpdateCondition = (index: number, field: string, value: string) => {
    const updated = [...ruleConditions];
    updated[index] = { ...updated[index], [field]: value };
    setRuleConditions(updated);
  };

  const handleAddAction = () => {
    setRuleActions([...ruleActions, { type: '', config: {} }]);
  };

  const handleRemoveAction = (index: number) => {
    setRuleActions(ruleActions.filter((_, i) => i !== index));
  };

  const handleUpdateAction = (index: number, type: string) => {
    const updated = [...ruleActions];
    updated[index] = { type, config: {} };
    setRuleActions(updated);
  };

  const handleToggleRule = (ruleId: string) => {
    // API call would go here
    console.log('Toggle rule:', ruleId);
  };

  const handleDeleteRule = (ruleId: string) => {
    if (!confirm('Are you sure you want to delete this rule?')) return;
    // API call would go here
    console.log('Delete rule:', ruleId);
  };

  const formatDate = (dateStr: string | undefined) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('en-US', {
      month: 'short', day: 'numeric', year: 'numeric',
      hour: 'numeric', minute: '2-digit',
    });
  };

  const getRelativeTime = (dateStr: string | undefined) => {
    if (!dateStr) return 'Never';
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return formatDate(dateStr);
  };

  return (
    <AdminLayout>
      <div className="automation-page">
        <div className="automation-header">
          <div className="automation-header-left">
            <h1>Automation</h1>
            <p>Create rules to automate your project's workflow</p>
          </div>
          <div className="automation-header-right">
            <button className="ab-btn ab-btn-primary" onClick={handleOpenCreate}>
              + Create Rule
            </button>
          </div>
        </div>

        <div className="automation-tabs">
          <button
            className={`automation-tab ${activeTab === 'rules' ? 'active' : ''}`}
            onClick={() => setActiveTab('rules')}
          >
            <span className="tab-icon">⚡</span>
            Rules
            <span className="tab-count">{MOCK_RULES.length}</span>
          </button>
          <button
            className={`automation-tab ${activeTab === 'logs' ? 'active' : ''}`}
            onClick={() => setActiveTab('logs')}
          >
            <span className="tab-icon">📋</span>
            Logs
            <span className="tab-count">{MOCK_LOGS.length}</span>
          </button>
          <button
            className={`automation-tab ${activeTab === 'settings' ? 'active' : ''}`}
            onClick={() => setActiveTab('settings')}
          >
            <span className="tab-icon">⚙️</span>
            Settings
          </button>
        </div>

        {activeTab === 'rules' && (
          <>
            <div className="automation-toolbar">
              <div className="automation-toolbar-left">
                <input
                  type="text"
                  placeholder="Search rules..."
                  className="automation-search"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
                <select
                  className="automation-filter-select"
                  value={projectFilter}
                  onChange={(e) => setProjectFilter(e.target.value)}
                >
                  <option value="">All Projects</option>
                  <option value="proj-1">Systems and Avionics</option>
                  <option value="proj-2">Mobile App</option>
                </select>
                <select
                  className="automation-filter-select"
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                >
                  <option value="">All Status</option>
                  <option value="enabled">Enabled</option>
                  <option value="disabled">Disabled</option>
                </select>
              </div>
            </div>

            <div className="automation-rules-table">
              <table className="ab-table">
                <thead>
                  <tr>
                    <th style={{ width: '40px' }}></th>
                    <th>Name</th>
                    <th>Project</th>
                    <th>Trigger</th>
                    <th>Last Run</th>
                    <th>Executions</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredRules.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="empty-row">
                        <div className="empty-state">
                          <span className="empty-icon">⚡</span>
                          <p>No automation rules found</p>
                          <button className="ab-btn ab-btn-primary ab-btn-sm" onClick={handleOpenCreate}>
                            Create your first rule
                          </button>
                        </div>
                      </td>
                    </tr>
                  ) : (
                    filteredRules.map((rule) => (
                      <tr key={rule.id} className={!rule.isEnabled ? 'disabled-rule' : ''}>
                        <td>
                          <label className="ab-toggle">
                            <input
                              type="checkbox"
                              checked={rule.isEnabled}
                              onChange={() => handleToggleRule(rule.id)}
                            />
                            <span className="ab-toggle-slider"></span>
                          </label>
                        </td>
                        <td>
                          <div className="rule-name-cell">
                            <button className="rule-name-btn" onClick={() => handleOpenEdit(rule)}>
                              {rule.name}
                            </button>
                            {rule.description && (
                              <span className="rule-description">{rule.description}</span>
                            )}
                          </div>
                        </td>
                        <td>
                          <span className="project-badge">{rule.projectName || 'Global'}</span>
                        </td>
                        <td>
                          <span className="trigger-badge">
                            {TRIGGER_OPTIONS.find(t => t.key === rule.triggerType)?.icon || '⚡'}
                            {TRIGGER_OPTIONS.find(t => t.key === rule.triggerType)?.label || rule.triggerType}
                          </span>
                        </td>
                        <td>
                          <span className="last-run">{getRelativeTime(rule.lastTriggered)}</span>
                        </td>
                        <td>
                          <span className="execution-count">{rule.triggerCount}</span>
                        </td>
                        <td>
                          <div className="action-buttons">
                            <button
                              className="ab-btn ab-btn-ghost ab-btn-sm"
                              onClick={() => handleOpenEdit(rule)}
                            >
                              Edit
                            </button>
                            <button
                              className="ab-btn ab-btn-ghost ab-btn-sm ab-btn-danger"
                              onClick={() => handleDeleteRule(rule.id)}
                            >
                              Delete
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </>
        )}

        {activeTab === 'logs' && (
          <div className="automation-logs">
            <table className="ab-table">
              <thead>
                <tr>
                  <th>Rule</th>
                  <th>Status</th>
                  <th>Triggered</th>
                  <th>Duration</th>
                  <th>Details</th>
                </tr>
              </thead>
              <tbody>
                {MOCK_LOGS.map((log) => (
                  <tr key={log.id}>
                    <td>
                      <span className="rule-name-log">{log.ruleName}</span>
                    </td>
                    <td>
                      <span className={`status-badge status-${log.status.toLowerCase()}`}>
                        {log.status === 'SUCCESS' ? '✓' : log.status === 'FAILED' ? '✗' : '⚠'}
                        {log.status}
                      </span>
                    </td>
                    <td>
                      <span className="trigger-time">{formatDate(log.triggeredAt)}</span>
                    </td>
                    <td>
                      <span className="duration">{log.executionTimeMs}ms</span>
                    </td>
                    <td>
                      <button className="ab-btn ab-btn-ghost ab-btn-sm" onClick={() => alert('View details for: ' + log.ruleName)}>
                        View Details
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {activeTab === 'settings' && (
          <div className="automation-settings">
            <div className="settings-section">
              <h3>Automation Settings</h3>
              <p className="settings-description">
                Configure global automation settings for your Systems and Avionics instance.
              </p>

              <div className="settings-form">
                <div className="settings-item">
                  <label>Maximum Rules Per Project</label>
                  <input type="number" className="ab-input" defaultValue="100" />
                </div>
                <div className="settings-item">
                  <label>Maximum Conditions Per Rule</label>
                  <input type="number" className="ab-input" defaultValue="10" />
                </div>
                <div className="settings-item">
                  <label>Maximum Actions Per Rule</label>
                  <input type="number" className="ab-input" defaultValue="10" />
                </div>
                <div className="settings-item">
                  <label>Log Retention (days)</label>
                  <input type="number" className="ab-input" defaultValue="30" />
                </div>
                <div className="settings-item">
                  <label className="ab-toggle-label">
                    <input type="checkbox" className="ab-toggle" defaultChecked />
                    <span>Enable Automation for New Projects</span>
                  </label>
                </div>
                <div className="settings-item">
                  <label className="ab-toggle-label">
                    <input type="checkbox" className="ab-toggle" defaultChecked />
                    <span>Notify on Rule Failure</span>
                  </label>
                </div>
              </div>

              <div className="settings-actions">
                <button className="ab-btn ab-btn-primary" onClick={() => alert('Settings saved!')}>Save Settings</button>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Create/Edit Rule Modal */}
      {showCreateModal && (
        <div className="ab-modal-overlay" onClick={() => setShowCreateModal(false)}>
          <div className="ab-modal automation-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ab-modal-header">
              <h2>{editingRule ? 'Edit Rule' : 'Create Rule'}</h2>
              <button className="ab-btn ab-btn-ghost" onClick={() => setShowCreateModal(false)}>×</button>
            </div>
            <div className="ab-modal-body">
              <div className="rule-form">
                {/* Basic Info */}
                <div className="form-section">
                  <h3>Rule Details</h3>
                  <div className="form-group">
                    <label className="ab-form-label">Name *</label>
                    <input
                      type="text"
                      className="ab-input"
                      placeholder="Enter rule name"
                      value={ruleName}
                      onChange={(e) => setRuleName(e.target.value)}
                    />
                  </div>
                  <div className="form-group">
                    <label className="ab-form-label">Description</label>
                    <textarea
                      className="ab-textarea"
                      placeholder="What does this rule do?"
                      rows={2}
                      value={ruleDescription}
                      onChange={(e) => setRuleDescription(e.target.value)}
                    />
                  </div>
                  <div className="form-group">
                    <label className="ab-form-label">Project</label>
                    <select
                      className="ab-select"
                      value={ruleProject}
                      onChange={(e) => setRuleProject(e.target.value)}
                    >
                      <option value="">All Projects</option>
                      <option value="proj-1">Systems and Avionics</option>
                      <option value="proj-2">Mobile App</option>
                    </select>
                  </div>
                </div>

                {/* Trigger */}
                <div className="form-section">
                  <h3>When... (Trigger)</h3>
                  <div className="trigger-selector">
                    {TRIGGER_OPTIONS.map((trigger) => (
                      <button
                        key={trigger.key}
                        className={`trigger-option ${ruleTrigger === trigger.key ? 'selected' : ''}`}
                        onClick={() => setRuleTrigger(trigger.key)}
                      >
                        <span className="trigger-icon">{trigger.icon}</span>
                        <span className="trigger-label">{trigger.label}</span>
                      </button>
                    ))}
                  </div>
                </div>

                {/* Conditions */}
                <div className="form-section">
                  <div className="section-header">
                    <h3>If... (Conditions)</h3>
                    <button className="ab-btn ab-btn-ghost ab-btn-sm" onClick={handleAddCondition}>
                      + Add Condition
                    </button>
                  </div>
                  {ruleConditions.length === 0 ? (
                    <p className="section-hint">No conditions - rule will trigger for all matching triggers</p>
                  ) : (
                    <div className="conditions-list">
                      {ruleConditions.map((condition, index) => (
                        <div key={index} className="condition-row">
                          <select
                            className="ab-select"
                            value={condition.field}
                            onChange={(e) => handleUpdateCondition(index, 'field', e.target.value)}
                          >
                            <option value="">Select field...</option>
                            {ISSUE_FIELDS.map((field) => (
                              <option key={field.key} value={field.key}>{field.label}</option>
                            ))}
                          </select>
                          <select
                            className="ab-select"
                            value={condition.operator}
                            onChange={(e) => handleUpdateCondition(index, 'operator', e.target.value)}
                          >
                            {CONDITION_OPERATORS.map((op) => (
                              <option key={op.key} value={op.key}>{op.label}</option>
                            ))}
                          </select>
                          <input
                            type="text"
                            className="ab-input"
                            placeholder="Value"
                            value={condition.value}
                            onChange={(e) => handleUpdateCondition(index, 'value', e.target.value)}
                          />
                          <button
                            className="ab-btn ab-btn-ghost ab-btn-sm"
                            onClick={() => handleRemoveCondition(index)}
                          >
                            ×
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Actions */}
                <div className="form-section">
                  <div className="section-header">
                    <h3>Then... (Actions)</h3>
                    <button className="ab-btn ab-btn-ghost ab-btn-sm" onClick={handleAddAction}>
                      + Add Action
                    </button>
                  </div>
                  {ruleActions.length === 0 ? (
                    <p className="section-hint error">At least one action is required</p>
                  ) : (
                    <div className="actions-list">
                      {ruleActions.map((action, index) => (
                        <div key={index} className="action-row">
                          <select
                            className="ab-select"
                            value={action.type}
                            onChange={(e) => handleUpdateAction(index, e.target.value)}
                          >
                            <option value="">Select action...</option>
                            {ACTION_OPTIONS.map((act) => (
                              <option key={act.key} value={act.key}>{act.label}</option>
                            ))}
                          </select>
                          {action.type && (
                            <div className="action-config">
                              {action.type === 'ASSIGN_ISSUE' && (
                                <select className="ab-select">
                                  <option value="">Select assignee...</option>
                                  <option value="user-1">User 1</option>
                                  <option value="user-2">User 2</option>
                                </select>
                              )}
                              {action.type === 'TRANSITION_ISSUE' && (
                                <select className="ab-select">
                                  <option value="">Select status...</option>
                                  <option value="todo">To Do</option>
                                  <option value="in_progress">In Progress</option>
                                  <option value="done">Done</option>
                                </select>
                              )}
                              {action.type === 'SEND_NOTIFICATION' && (
                                <input type="text" className="ab-input" placeholder="Notification message..." />
                              )}
                            </div>
                          )}
                          <button
                            className="ab-btn ab-btn-ghost ab-btn-sm"
                            onClick={() => handleRemoveAction(index)}
                          >
                            ×
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Enable Toggle */}
                <div className="form-section">
                  <label className="ab-toggle-label">
                    <input
                      type="checkbox"
                      className="ab-toggle"
                      checked={ruleEnabled}
                      onChange={(e) => setRuleEnabled(e.target.checked)}
                    />
                    <span>Enable this rule</span>
                  </label>
                </div>
              </div>
            </div>
            <div className="ab-modal-footer">
              <button className="ab-btn ab-btn-secondary" onClick={() => setShowCreateModal(false)}>
                Cancel
              </button>
              <button
                className="ab-btn ab-btn-primary"
                disabled={!ruleName || !ruleTrigger || ruleActions.length === 0}
                onClick={() => {
                  // Save rule
                  setShowCreateModal(false);
                }}
              >
                {editingRule ? 'Save Changes' : 'Create Rule'}
              </button>
            </div>
          </div>
        </div>
      )}

      <style>{`
        .automation-page {
          padding: 24px;
        }

        .automation-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          margin-bottom: 24px;
        }

        .automation-header h1 {
          margin: 0 0 4px;
          font-size: 24px;
          font-weight: 600;
        }

        .automation-header p {
          margin: 0;
          color: var(--ab-gray-500, #6b7280);
          font-size: 14px;
        }

        .automation-tabs {
          display: flex;
          gap: 4px;
          border-bottom: 1px solid var(--ab-gray-200, #e5e7eb);
          margin-bottom: 24px;
        }

        .automation-tab {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 12px 20px;
          background: none;
          border: none;
          border-bottom: 2px solid transparent;
          cursor: pointer;
          font-size: 14px;
          font-weight: 500;
          color: var(--ab-gray-500, #6b7280);
          transition: all 0.2s;
        }

        .automation-tab:hover {
          color: var(--ab-gray-700, #374151);
        }

        .automation-tab.active {
          color: var(--ab-primary-600, #002d80);
          border-bottom-color: var(--ab-primary-500, #00205b);
        }

        .tab-icon { font-size: 16px; }
        .tab-count {
          background: var(--ab-gray-100, #f3f4f6);
          padding: 2px 8px;
          border-radius: 10px;
          font-size: 12px;
        }

        .automation-toolbar {
          display: flex;
          justify-content: space-between;
          margin-bottom: 16px;
        }

        .automation-toolbar-left {
          display: flex;
          gap: 12px;
        }

        .automation-search {
          padding: 8px 12px;
          border: 1px solid var(--ab-gray-300, #d1d5db);
          border-radius: 4px;
          font-size: 14px;
          width: 250px;
        }

        .automation-filter-select {
          padding: 8px 12px;
          border: 1px solid var(--ab-gray-300, #d1d5db);
          border-radius: 4px;
          font-size: 14px;
          background: white;
        }

        .automation-rules-table {
          background: white;
          border-radius: 8px;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
          overflow: hidden;
        }

        .ab-table {
          width: 100%;
          border-collapse: collapse;
        }

        .ab-table th {
          text-align: left;
          padding: 12px 16px;
          background: var(--ab-gray-50, #f9fafb);
          font-size: 12px;
          font-weight: 600;
          color: var(--ab-gray-500, #6b7280);
          text-transform: uppercase;
          border-bottom: 1px solid var(--ab-gray-200, #e5e7eb);
        }

        .ab-table td {
          padding: 12px 16px;
          border-bottom: 1px solid var(--ab-gray-100, #f3f4f6);
          font-size: 14px;
        }

        .ab-table tr:last-child td {
          border-bottom: none;
        }

        .disabled-rule {
          opacity: 0.6;
        }

        .rule-name-cell {
          display: flex;
          flex-direction: column;
          gap: 2px;
        }

        .rule-name-btn {
          background: none;
          border: none;
          padding: 0;
          font-size: 14px;
          font-weight: 600;
          color: var(--ab-primary-600, #002d80);
          cursor: pointer;
          text-align: left;
        }

        .rule-name-btn:hover {
          text-decoration: underline;
        }

        .rule-description {
          font-size: 12px;
          color: var(--ab-gray-500, #6b7280);
        }

        .project-badge {
          display: inline-block;
          padding: 2px 8px;
          background: var(--ab-gray-100, #f3f4f6);
          border-radius: 4px;
          font-size: 12px;
          color: var(--ab-gray-600, #4b5563);
        }

        .trigger-badge {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          padding: 4px 8px;
          background: var(--ab-primary-50, #f0f4ff);
          border-radius: 4px;
          font-size: 12px;
          color: var(--ab-primary-700, #00205b);
        }

        .last-run {
          font-size: 13px;
          color: var(--ab-gray-600, #4b5563);
        }

        .execution-count {
          font-family: monospace;
          font-weight: 600;
          color: var(--ab-gray-700, #1f2937);
        }

        .action-buttons {
          display: flex;
          gap: 4px;
        }

        .empty-row td {
          padding: 48px;
        }

        .empty-state {
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: 12px;
        }

        .empty-icon {
          font-size: 48px;
        }

        .empty-state p {
          color: var(--ab-gray-500, #6b7280);
          margin: 0;
        }

        /* Toggle */
        .ab-toggle {
          position: relative;
          width: 36px;
          height: 20px;
          appearance: none;
          background: var(--ab-gray-300, #d1d5db);
          border-radius: 10px;
          cursor: pointer;
          transition: background 0.2s;
        }

        .ab-toggle:checked {
          background: var(--ab-primary-500, #00205b);
        }

        .ab-toggle::before {
          content: '';
          position: absolute;
          top: 2px;
          left: 2px;
          width: 16px;
          height: 16px;
          background: white;
          border-radius: 50%;
          transition: transform 0.2s;
        }

        .ab-toggle:checked::before {
          transform: translateX(16px);
        }

        /* Logs */
        .automation-logs {
          background: white;
          border-radius: 8px;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
          overflow: hidden;
        }

        .status-badge {
          display: inline-flex;
          align-items: center;
          gap: 4px;
          padding: 2px 8px;
          border-radius: 4px;
          font-size: 12px;
          font-weight: 500;
        }

        .status-success {
          background: #dcfce7;
          color: #166534;
        }

        .status-failed {
          background: #fee2e2;
          color: #991b1b;
        }

        .status-partial {
          background: #fff3cd;
          color: #856404;
        }

        .rule-name-log {
          font-weight: 500;
        }

        .trigger-time {
          font-size: 13px;
          color: var(--ab-gray-600, #4b5563);
        }

        .duration {
          font-family: monospace;
          font-size: 13px;
        }

        /* Settings */
        .automation-settings {
          background: white;
          border-radius: 8px;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
          padding: 24px;
        }

        .settings-section h3 {
          margin: 0 0 4px;
          font-size: 18px;
          font-weight: 600;
        }

        .settings-description {
          color: var(--ab-gray-500, #6b7280);
          margin: 0 0 24px;
        }

        .settings-form {
          display: flex;
          flex-direction: column;
          gap: 16px;
          max-width: 500px;
        }

        .settings-item {
          display: flex;
          flex-direction: column;
          gap: 4px;
        }

        .settings-item label {
          font-size: 14px;
          font-weight: 500;
          color: var(--ab-gray-700, #374151);
        }

        .ab-input, .ab-select {
          padding: 8px 12px;
          border: 1px solid var(--ab-gray-300, #d1d5db);
          border-radius: 4px;
          font-size: 14px;
        }

        .ab-toggle-label {
          display: flex;
          align-items: center;
          gap: 8px;
          cursor: pointer;
        }

        .settings-actions {
          margin-top: 24px;
        }

        /* Modal */
        .automation-modal {
          width: 800px;
          max-width: 90vw;
          max-height: 90vh;
          overflow-y: auto;
        }

        .rule-form {
          display: flex;
          flex-direction: column;
          gap: 24px;
        }

        .form-section {
          padding-bottom: 20px;
          border-bottom: 1px solid var(--ab-gray-200, #e5e7eb);
        }

        .form-section:last-child {
          border-bottom: none;
        }

        .form-section h3 {
          margin: 0 0 12px;
          font-size: 14px;
          font-weight: 600;
          color: var(--ab-gray-700, #374151);
        }

        .section-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 12px;
        }

        .section-header h3 {
          margin: 0;
        }

        .section-hint {
          font-size: 13px;
          color: var(--ab-gray-500, #6b7280);
          margin: 0;
        }

        .section-hint.error {
          color: var(--ab-danger-600, #dc2626);
        }

        .form-group {
          margin-bottom: 12px;
        }

        .ab-form-label {
          display: block;
          font-size: 14px;
          font-weight: 500;
          margin-bottom: 4px;
          color: var(--ab-gray-700, #374151);
        }

        .ab-textarea {
          width: 100%;
          padding: 8px 12px;
          border: 1px solid var(--ab-gray-300, #d1d5db);
          border-radius: 4px;
          font-size: 14px;
          resize: vertical;
        }

        .trigger-selector {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 8px;
        }

        .trigger-option {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 12px;
          background: var(--ab-gray-50, #f9fafb);
          border: 2px solid var(--ab-gray-200, #e5e7eb);
          border-radius: 6px;
          cursor: pointer;
          text-align: left;
          transition: all 0.2s;
        }

        .trigger-option:hover {
          border-color: var(--ab-primary-300, #86a8e9);
        }

        .trigger-option.selected {
          background: var(--ab-primary-50, #f0f4ff);
          border-color: var(--ab-primary-500, #00205b);
        }

        .trigger-icon { font-size: 18px; }
        .trigger-label { font-weight: 500; font-size: 14px; }

        .conditions-list, .actions-list {
          display: flex;
          flex-direction: column;
          gap: 8px;
        }

        .condition-row, .action-row {
          display: flex;
          align-items: center;
          gap: 8px;
        }

        .condition-row .ab-select,
        .condition-row .ab-input {
          flex: 1;
        }

        .action-row .ab-select {
          flex: 1;
        }

        .action-config {
          flex: 1;
        }
      `}</style>
    </AdminLayout>
  );
}