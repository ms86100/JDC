import React, { useState, useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';
import combinedApi from '../../../api/testApi';
import {
  Settings, Globe, Shield, Link2, Bell, Save, RotateCcw, Plus, Trash2,
  Check, X, ChevronRight, ChevronDown, Edit2, Eye, EyeOff, AlertCircle, CheckCircle, Loader2
} from 'lucide-react';

// Tab interface
interface Tab {
  id: string;
  label: string;
  icon: React.ReactNode;
}

// Toast notification
const Toast: React.FC<{ message: string; type: 'success' | 'error' | 'info'; onClose: () => void }> = ({ message, type, onClose }) => {
  useEffect(() => {
    const timer = setTimeout(onClose, 3000);
    return () => clearTimeout(timer);
  }, [onClose]);

  const bgColor = type === 'success' ? 'bg-green-500' : type === 'error' ? 'bg-red-500' : 'bg-blue-500';

  return (
    <div className={`fixed bottom-4 right-4 ${bgColor} text-white px-4 py-3 rounded-lg shadow-lg flex items-center gap-2 z-50`}>
      {type === 'success' && <CheckCircle className="w-5 h-5" />}
      <span>{message}</span>
      <button onClick={onClose} className="ml-2 hover:opacity-80">
        <X className="w-4 h-4" />
      </button>
    </div>
  );
};

// Confirmation Dialog
const ConfirmDialog: React.FC<{
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  variant?: 'default' | 'danger';
}> = ({ open, title, message, confirmLabel = 'Confirm', onConfirm, onCancel, variant = 'default' }) => {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onCancel}></div>
        <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h3 className="text-lg font-semibold mb-2">{title}</h3>
          <p className="text-gray-600 mb-6">{message}</p>
          <div className="flex justify-end gap-3">
            <button onClick={onCancel} className="btn btn-secondary">Cancel</button>
            <button onClick={onConfirm} className={`btn ${variant === 'danger' ? 'bg-red-600 hover:bg-red-700 text-white' : 'btn-primary'}`}>
              {confirmLabel}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// Tab Components
interface GeneralTabProps {
  projectId: string;
  onSettingsChange?: () => void;
}

const defaultSettings = {
  defaultTestType: 'MANUAL',
  defaultPriority: 'MEDIUM',
  allowDuplicateNames: false,
  requireDescription: false,
  autoArchiveAfterDays: 90,
  enableTestChaining: true,
  maxStepsPerTest: 50,
  requirePrecondition: false,
};

const GeneralTab: React.FC<GeneralTabProps> = ({ projectId, onSettingsChange }) => {
  const [settings, setSettings] = useState(defaultSettings);

  // Load settings from localStorage on mount
  useEffect(() => {
    const settingsKey = `avisys-test-settings-${projectId}`;
    const stored = localStorage.getItem(settingsKey);
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        if (parsed.general) {
          setSettings({ ...defaultSettings, ...parsed.general });
        }
      } catch (e) {
        console.error('Failed to parse stored settings:', e);
      }
    }
  }, [projectId]);

  const updateSetting = (key: string, value: any) => {
    setSettings({ ...settings, [key]: value });
    window.dispatchEvent(new CustomEvent('test-settings-change'));
    if (onSettingsChange) onSettingsChange();
  };

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-lg border p-4">
        <h3 className="font-semibold mb-4">Default Test Settings</h3>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium mb-1">Default Test Type</label>
            <select
              value={settings.defaultTestType}
              onChange={(e) => updateSetting('defaultTestType', e.target.value)}
              className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="MANUAL">Manual</option>
              <option value="AUTOMATED">Automated</option>
              <option value="BDD">BDD (Gherkin)</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Default Priority</label>
            <select
              value={settings.defaultPriority}
              onChange={(e) => updateSetting('defaultPriority', e.target.value)}
              className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-lg border p-4">
        <h3 className="font-semibold mb-4">Validation Rules</h3>
        <div className="space-y-3">
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={settings.allowDuplicateNames}
              onChange={(e) => updateSetting('allowDuplicateNames', e.target.checked)}
              className="w-4 h-4 rounded border-gray-300"
            />
            <div>
              <span className="font-medium">Allow Duplicate Names</span>
              <p className="text-sm text-gray-500">Allow multiple tests with the same name</p>
            </div>
          </label>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={settings.requireDescription}
              onChange={(e) => updateSetting('requireDescription', e.target.checked)}
              className="w-4 h-4 rounded border-gray-300"
            />
            <div>
              <span className="font-medium">Require Description</span>
              <p className="text-sm text-gray-500">Test creation requires a description</p>
            </div>
          </label>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={settings.requirePrecondition}
              onChange={(e) => updateSetting('requirePrecondition', e.target.checked)}
              className="w-4 h-4 rounded border-gray-300"
            />
            <div>
              <span className="font-medium">Require Precondition</span>
              <p className="text-sm text-gray-500">Tests must have preconditions defined</p>
            </div>
          </label>
        </div>
      </div>

      <div className="bg-white rounded-lg border p-4">
        <h3 className="font-semibold mb-4">Limits & Boundaries</h3>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium mb-1">Max Steps Per Test</label>
            <input
              type="number"
              value={settings.maxStepsPerTest}
              onChange={(e) => updateSetting('maxStepsPerTest', parseInt(e.target.value))}
              className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              min="1"
              max="200"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Auto-Archive After (days)</label>
            <input
              type="number"
              value={settings.autoArchiveAfterDays}
              onChange={(e) => updateSetting('autoArchiveAfterDays', parseInt(e.target.value))}
              className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              min="30"
              max="365"
            />
          </div>
        </div>
      </div>
    </div>
  );
};

const WorkflowsTab: React.FC<{ projectId: string }> = ({ projectId }) => {
  const [workflows, setWorkflows] = useState([
    { id: '1', name: 'Manual Test Workflow', stages: ['Draft', 'Ready', 'In Review', 'Approved'] },
    { id: '2', name: 'Automated Test Workflow', stages: ['Created', 'Running', 'Passed', 'Failed'] },
  ]);

  const [editingWorkflow, setEditingWorkflow] = useState<string | null>(null);

  const addWorkflow = () => {
    setWorkflows([...workflows, {
      id: `new-${Date.now()}`,
      name: 'New Workflow',
      stages: ['Draft', 'Ready'],
    }]);
  };

  const removeWorkflow = (id: string) => {
    setWorkflows(workflows.filter(w => w.id !== id));
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="font-semibold">Status Workflows</h3>
          <p className="text-sm text-gray-500">Define how tests transition between statuses</p>
        </div>
        <button onClick={addWorkflow} className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg">
          <Plus className="w-4 h-4" /> Add Workflow
        </button>
      </div>

      <div className="space-y-4">
        {workflows.map((workflow) => (
          <div key={workflow.id} className="bg-white rounded-lg border p-4">
            <div className="flex items-center justify-between mb-3">
              <input
                type="text"
                value={workflow.name}
                onChange={(e) => {
                  const updated = workflows.map(w => w.id === workflow.id ? { ...w, name: e.target.value } : w);
                  setWorkflows(updated);
                }}
                className="font-semibold text-lg px-2 py-1 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <div className="flex items-center gap-2">
                <button onClick={() => setEditingWorkflow(workflow.id)} className="text-blue-600 hover:text-blue-800">
                  <Edit2 className="w-4 h-4" />
                </button>
                <button onClick={() => removeWorkflow(workflow.id)} className="text-red-500 hover:text-red-700">
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>

            <div className="flex items-center gap-2">
              {workflow.stages.map((stage, i) => (
                <React.Fragment key={i}>
                  <div className="px-3 py-1 bg-gray-100 rounded text-sm">{stage}</div>
                  {i < workflow.stages.length - 1 && (
                    <ChevronRight className="w-4 h-4 text-gray-400" />
                  )}
                </React.Fragment>
              ))}
            </div>
          </div>
        ))}
      </div>

      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
        <div className="flex items-start gap-3">
          <AlertCircle className="w-5 h-5 text-yellow-600 mt-0.5" />
          <div>
            <h4 className="font-medium text-yellow-800">Workflow Migrations</h4>
            <p className="text-sm text-yellow-700 mt-1">
              When you modify a workflow, existing tests will need to be migrated to the new status flow.
              You can schedule a bulk migration or apply changes incrementally.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

const PermissionsTab: React.FC<{ projectId: string }> = ({ projectId }) => {
  const [permissions, setPermissions] = useState([
    { role: 'Test Manager', createTest: true, editTest: true, deleteTest: true, executeTest: true, managePlans: true },
    { role: 'Test Lead', createTest: true, editTest: true, deleteTest: false, executeTest: true, managePlans: true },
    { role: 'Tester', createTest: true, editTest: false, deleteTest: false, executeTest: true, managePlans: false },
    { role: 'Viewer', createTest: false, editTest: false, deleteTest: false, executeTest: false, managePlans: false },
  ]);

  const togglePermission = (roleIndex: number, permission: string) => {
    const updated = [...permissions];
    updated[roleIndex] = {
      ...updated[roleIndex],
      [permission]: !updated[roleIndex][permission as keyof typeof updated[number]]
    };
    setPermissions(updated);
  };

  return (
    <div className="space-y-6">
      <div>
        <h3 className="font-semibold mb-2">Role-Based Permissions</h3>
        <p className="text-sm text-gray-500">Configure what each role can do with tests</p>
      </div>

      <div className="bg-white rounded-lg border overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="bg-gray-50 border-b">
              <th className="p-3 text-left font-medium">Role</th>
              <th className="p-3 text-center font-medium">Create</th>
              <th className="p-3 text-center font-medium">Edit</th>
              <th className="p-3 text-center font-medium">Delete</th>
              <th className="p-3 text-center font-medium">Execute</th>
              <th className="p-3 text-center font-medium">Manage Plans</th>
            </tr>
          </thead>
          <tbody>
            {permissions.map((perm, i) => (
              <tr key={i} className="border-b last:border-b-0">
                <td className="p-3 font-medium">{perm.role}</td>
                {['createTest', 'editTest', 'deleteTest', 'executeTest', 'managePlans'].map(permName => (
                  <td key={permName} className="p-3 text-center">
                    <input
                      type="checkbox"
                      checked={perm[permName as keyof typeof perm] as boolean}
                      onChange={() => togglePermission(i, permName)}
                      className="w-4 h-4 rounded border-gray-300"
                    />
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
        <div className="flex items-start gap-3">
          <Shield className="w-5 h-5 text-blue-600 mt-0.5" />
          <div>
            <h4 className="font-medium text-blue-800">Permission Inheritance</h4>
            <p className="text-sm text-blue-700 mt-1">
              Permissions cascade from project to test plan to test execution.
              Users with higher-level permissions automatically inherit lower-level permissions.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

const IntegrationsTab: React.FC<{ projectId: string }> = ({ projectId }) => {
  const [integrations, setIntegrations] = useState({
    avisys: { enabled: true, url: 'https://avisys.example.com', apiKey: '****', autoLink: true },
    ci: { enabled: false, provider: 'jenkins', url: '', webhookSecret: '' },
    slack: { enabled: false, webhookUrl: '', notifyOn: ['failure', 'completed'] },
  });

  const [showApiKey, setShowApiKey] = useState(false);

  const updateIntegration = (key: string, value: any) => {
    setIntegrations({ ...integrations, [key]: value });
    window.dispatchEvent(new CustomEvent('test-settings-change'));
  };

  const saveIntegrationsToStorage = () => {
    const settingsKey = `avisys-test-settings-${projectId}`;
    const existing = localStorage.getItem(settingsKey);
    const parsed = existing ? JSON.parse(existing) : {};
    parsed.integrations = integrations;
    localStorage.setItem(settingsKey, JSON.stringify(parsed));
    window.dispatchEvent(new CustomEvent('test-settings-change'));
  };

  return (
    <div className="space-y-6">
      {/* Avionics Systems Integration */}
      <div className="bg-white rounded-lg border p-4">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
              <Link2 className="w-5 h-5 text-blue-600" />
            </div>
            <div>
              <h3 className="font-semibold">Systems Integration</h3>
              <p className="text-sm text-gray-500">Link tests to Systems issues</p>
            </div>
          </div>
          <label className="flex items-center gap-2">
            <span className="text-sm text-gray-500">{integrations.avisys.enabled ? 'Enabled' : 'Disabled'}</span>
            <input
              type="checkbox"
              checked={integrations.avisys.enabled}
              onChange={(e) => setIntegrations({ ...integrations, avisys: { ...integrations.avisys, enabled: e.target.checked } })}
              className="w-4 h-4 rounded border-gray-300"
            />
          </label>
        </div>

        {integrations.avisys.enabled && (
          <div className="space-y-4 mt-4">
            <div>
              <label className="block text-sm font-medium mb-1">Systems URL</label>
              <input
                type="url"
                value={integrations.avisys.url}
                onChange={(e) => setIntegrations({ ...integrations, avisys: { ...integrations.avisys, url: e.target.value } })}
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="https://your-domain.atlassian.net"
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">API Key</label>
              <div className="relative">
                <input
                  type={showApiKey ? 'text' : 'password'}
                  value={integrations.avisys.apiKey}
                  onChange={(e) => setIntegrations({ ...integrations, avisys: { ...integrations.avisys, apiKey: e.target.value } })}
                  className="w-full px-3 py-2 pr-10 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <button onClick={() => setShowApiKey(!showApiKey)} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400">
                  {showApiKey ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={integrations.avisys.autoLink}
                onChange={(e) => setIntegrations({ ...integrations, avisys: { ...integrations.avisys, autoLink: e.target.checked } })}
                className="w-4 h-4 rounded border-gray-300"
              />
              <span className="text-sm">Auto-link test results to Systems issues</span>
            </label>
          </div>
        )}
      </div>

      {/* CI/CD Integration */}
      <div className="bg-white rounded-lg border p-4">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
              <Link2 className="w-5 h-5 text-green-600" />
            </div>
            <div>
              <h3 className="font-semibold">CI/CD Integration</h3>
              <p className="text-sm text-gray-500">Import test results from CI pipelines</p>
            </div>
          </div>
          <label className="flex items-center gap-2">
            <span className="text-sm text-gray-500">{integrations.ci.enabled ? 'Enabled' : 'Disabled'}</span>
            <input
              type="checkbox"
              checked={integrations.ci.enabled}
              onChange={(e) => setIntegrations({ ...integrations, ci: { ...integrations.ci, enabled: e.target.checked } })}
              className="w-4 h-4 rounded border-gray-300"
            />
          </label>
        </div>

        {integrations.ci.enabled && (
          <div className="space-y-4 mt-4">
            <div>
              <label className="block text-sm font-medium mb-1">Provider</label>
              <select
                value={integrations.ci.provider}
                onChange={(e) => setIntegrations({ ...integrations, ci: { ...integrations.ci, provider: e.target.value } })}
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="jenkins">Jenkins</option>
                <option value="github">GitHub Actions</option>
                <option value="gitlab">GitLab CI</option>
                <option value="azure">Azure DevOps</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Webhook URL</label>
              <input
                type="url"
                value={integrations.ci.url}
                onChange={(e) => setIntegrations({ ...integrations, ci: { ...integrations.ci, url: e.target.value } })}
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="https://ci.example.com/webhook"
              />
            </div>
          </div>
        )}
      </div>

      {/* Slack Integration */}
      <div className="bg-white rounded-lg border p-4">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
              <Bell className="w-5 h-5 text-purple-600" />
            </div>
            <div>
              <h3 className="font-semibold">Slack Notifications</h3>
              <p className="text-sm text-gray-500">Send test results to Slack channels</p>
            </div>
          </div>
          <label className="flex items-center gap-2">
            <span className="text-sm text-gray-500">{integrations.slack.enabled ? 'Enabled' : 'Disabled'}</span>
            <input
              type="checkbox"
              checked={integrations.slack.enabled}
              onChange={(e) => setIntegrations({ ...integrations, slack: { ...integrations.slack, enabled: e.target.checked } })}
              className="w-4 h-4 rounded border-gray-300"
            />
          </label>
        </div>

        {integrations.slack.enabled && (
          <div className="space-y-4 mt-4">
            <div>
              <label className="block text-sm font-medium mb-1">Webhook URL</label>
              <input
                type="url"
                value={integrations.slack.webhookUrl}
                onChange={(e) => setIntegrations({ ...integrations, slack: { ...integrations.slack, webhookUrl: e.target.value } })}
                className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="https://hooks.slack.com/services/..."
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-2">Notify On</label>
              <div className="flex gap-4">
                {['failure', 'completed', 'quarantine'].map((event) => (
                  <label key={event} className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      checked={integrations.slack.notifyOn.includes(event)}
                      onChange={(e) => {
                        const notifyOn = e.target.checked
                          ? [...integrations.slack.notifyOn, event]
                          : integrations.slack.notifyOn.filter(n => n !== event);
                        setIntegrations({ ...integrations, slack: { ...integrations.slack, notifyOn } });
                      }}
                      className="w-4 h-4 rounded border-gray-300"
                    />
                    <span className="text-sm capitalize">{event}</span>
                  </label>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

const NotificationsTab: React.FC<{ projectId: string }> = ({ projectId }) => {
  const [notificationRules, setNotificationRules] = useState([
    { id: '1', name: 'Test Failure Alert', event: 'test_failure', recipients: ['test_manager', 'test_lead'], channels: ['email', 'slack'] },
    { id: '2', name: 'Flaky Test Detection', event: 'flaky_detected', recipients: ['test_lead'], channels: ['email'] },
    { id: '3', name: 'Execution Completed', event: 'execution_completed', recipients: ['test_owner'], channels: ['slack'] },
  ]);

  const addRule = () => {
    setNotificationRules([...notificationRules, {
      id: `new-${Date.now()}`,
      name: 'New Rule',
      event: 'test_failure',
      recipients: [],
      channels: [],
    }]);
  };

  const removeRule = (id: string) => {
    setNotificationRules(notificationRules.filter(r => r.id !== id));
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="font-semibold">Notification Rules</h3>
          <p className="text-sm text-gray-500">Configure when and how to notify users</p>
        </div>
        <button onClick={addRule} className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg">
          <Plus className="w-4 h-4" /> Add Rule
        </button>
      </div>

      <div className="space-y-4">
        {notificationRules.map((rule) => (
          <div key={rule.id} className="bg-white rounded-lg border p-4">
            <div className="flex items-center justify-between mb-3">
              <input
                type="text"
                value={rule.name}
                onChange={(e) => {
                  const updated = notificationRules.map(r => r.id === rule.id ? { ...r, name: e.target.value } : r);
                  setNotificationRules(updated);
                }}
                className="font-semibold text-lg px-2 py-1 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <button onClick={() => removeRule(rule.id)} className="text-red-500 hover:text-red-700">
                <Trash2 className="w-4 h-4" />
              </button>
            </div>

            <div className="grid grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium mb-1">Event</label>
                <select
                  value={rule.event}
                  onChange={(e) => {
                    const updated = notificationRules.map(r => r.id === rule.id ? { ...r, event: e.target.value } : r);
                    setNotificationRules(updated);
                  }}
                  className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="test_failure">Test Failure</option>
                  <option value="flaky_detected">Flaky Test Detected</option>
                  <option value="execution_completed">Execution Completed</option>
                  <option value="quarantine_triggered">Quarantine Triggered</option>
                  <option value="test_created">Test Created</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Recipients</label>
                <select
                  multiple
                  value={rule.recipients}
                  onChange={(e) => {
                    const selected = Array.from(e.target.selectedOptions, opt => opt.value);
                    const updated = notificationRules.map(r => r.id === rule.id ? { ...r, recipients: selected } : r);
                    setNotificationRules(updated);
                  }}
                  className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="test_manager">Test Manager</option>
                  <option value="test_lead">Test Lead</option>
                  <option value="test_owner">Test Owner</option>
                  <option value="project_lead">Project Lead</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">Channels</label>
                <div className="flex gap-3 pt-2">
                  {['email', 'slack', 'teams'].map((channel) => (
                    <label key={channel} className="flex items-center gap-1">
                      <input
                        type="checkbox"
                        checked={rule.channels.includes(channel)}
                        onChange={(e) => {
                          const channels = e.target.checked
                            ? [...rule.channels, channel]
                            : rule.channels.filter(c => c !== channel);
                          const updated = notificationRules.map(r => r.id === rule.id ? { ...r, channels } : r);
                          setNotificationRules(updated);
                        }}
                        className="w-4 h-4 rounded border-gray-300"
                      />
                      <span className="text-sm capitalize">{channel}</span>
                    </label>
                  ))}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

// Main Page Component
export const TestSettingsPage: React.FC<{ projectId?: string }> = ({ projectId: propProjectId }) => {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState('general');
  const [hasChanges, setHasChanges] = useState(false);
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'info' } | null>(null);

  // Listen for settings changes from child tabs
  useEffect(() => {
    const handleSettingsChange = () => {
      setHasChanges(true);
    };

    window.addEventListener('test-settings-change', handleSettingsChange);
    return () => window.removeEventListener('test-settings-change', handleSettingsChange);
  }, []);

  const tabs: Tab[] = [
    { id: 'general', label: 'General', icon: <Settings className="w-4 h-4" /> },
    { id: 'workflows', label: 'Workflows', icon: <Globe className="w-4 h-4" /> },
    { id: 'permissions', label: 'Permissions', icon: <Shield className="w-4 h-4" /> },
    { id: 'integrations', label: 'Integrations', icon: <Link2 className="w-4 h-4" /> },
    { id: 'notifications', label: 'Notifications', icon: <Bell className="w-4 h-4" /> },
  ];

  // Save mutation with API fallback to localStorage
  const saveMutation = useMutation({
    mutationFn: async (settings: Record<string, unknown>) => {
      if (propProjectId) {
        // Try to save to backend
        try {
          await combinedApi.saveTestSettings(propProjectId, settings);
          return { saved: true, source: 'api' };
        } catch (error) {
          console.warn('Backend save failed, falling back to localStorage:', error);
        }
      }
      // Fallback to localStorage
      const settingsKey = propProjectId ? `avisys-test-settings-${propProjectId}` : 'avisys-test-settings';
      const settingsWithMeta = {
        ...settings,
        lastSaved: new Date().toISOString(),
        projectId: propProjectId,
      };
      localStorage.setItem(settingsKey, JSON.stringify(settingsWithMeta));
      return { saved: true, source: 'localStorage' };
    },
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['test-settings', propProjectId] });
      setHasChanges(false);
      const message = result.source === 'api'
        ? 'Settings saved to server successfully!'
        : 'Settings saved locally (server unavailable)';
      setToast({ message, type: 'success' });
    },
    onError: (error) => {
      console.error('Failed to save settings:', error);
      setToast({ message: 'Failed to save settings', type: 'error' });
    },
  });

  const handleSave = async () => {
    // Collect all settings from localStorage
    const settingsKey = propProjectId ? `avisys-test-settings-${propProjectId}` : 'avisys-test-settings';
    const settings = localStorage.getItem(settingsKey);
    const parsed = settings ? JSON.parse(settings) : {};

    saveMutation.mutate(parsed);
  };

  const handleDiscard = () => {
    setHasChanges(false);
    setToast({ message: 'Changes discarded', type: 'info' });
  };

  const renderTabContent = () => {
    if (!propProjectId) {
      return (
        <div className="flex flex-col items-center justify-center h-64 text-center">
          <Settings className="w-12 h-12 text-gray-400 mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">No project selected</h3>
          <p className="text-gray-500">Select a project to configure test settings</p>
        </div>
      );
    }

    switch (activeTab) {
      case 'general':
        return <GeneralTab projectId={propProjectId} onSettingsChange={() => setHasChanges(true)} />;
      case 'workflows':
        return <WorkflowsTab projectId={propProjectId} />;
      case 'permissions':
        return <PermissionsTab projectId={propProjectId} />;
      case 'integrations':
        return <IntegrationsTab projectId={propProjectId} />;
      case 'notifications':
        return <NotificationsTab projectId={propProjectId} />;
      default:
        return null;
    }
  };

  return (
    <div className="h-full flex flex-col bg-gray-50">
      {/* Toast */}
      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}

      {/* Header */}
      <div className="bg-white px-6 py-4 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Test Settings</h1>
            <p className="text-sm text-gray-500 mt-1">Configure test management behavior for this project</p>
          </div>
          {hasChanges && (
            <div className="flex items-center gap-3">
              <button
                onClick={handleDiscard}
                className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                <RotateCcw className="w-4 h-4" />
                Discard
              </button>
              <button
                onClick={handleSave}
                disabled={saveMutation.isPending}
                className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg disabled:opacity-50"
              >
                {saveMutation.isPending && <Loader2 className="w-4 h-4 animate-spin" />}
                <Save className="w-4 h-4" />
                {saveMutation.isPending ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="bg-white border-b border-gray-200">
        <div className="flex px-6">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`flex items-center gap-2 px-4 py-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab.id
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              {tab.icon}
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-6">
        {renderTabContent()}
      </div>
    </div>
  );
};

export default TestSettingsPage;