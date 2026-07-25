import { Fragment, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Editor from '@monaco-editor/react';
import { scriptApi, ScriptDefinition, ScriptExecutionLog, ScriptVersion, ScriptSchedule, ScriptListener, ScriptFieldBehavior } from '../../../api/scriptApi';
import ScriptConsole from '../components/ScriptConsole';
import ProfilerDashboard from '../components/ScriptProfilerDashboard';

type Tab = 'scripts' | 'console' | 'logs' | 'listeners' | 'behaviors' | 'profiler';

const TYPE_LABELS: Record<string, string> = {
  CONDITION: 'Condition',
  VALIDATOR: 'Validator',
  POST_FUNCTION: 'Post-Function',
};

const KEY_PATTERN = /^[a-z][a-z0-9-]{2,63}$/;

function extractError(err: unknown): string {
  if (err && typeof err === 'object') {
    const axiosErr = err as { response?: { data?: { message?: string; error?: string; validationErrors?: Record<string, string> } }; message?: string };
    if (axiosErr.response?.data) {
      const d = axiosErr.response.data;
      if (d.validationErrors) return Object.values(d.validationErrors).join('; ');
      if (d.message) return d.message;
      if (d.error) return d.error;
    }
    if (axiosErr.message) return axiosErr.message;
  }
  return 'An unexpected error occurred';
}

export default function WorkflowScriptsPage() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<Tab>('scripts');
  const [scripts, setScripts] = useState<ScriptDefinition[]>([]);
  const [logs, setLogs] = useState<ScriptExecutionLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editingScript, setEditingScript] = useState<ScriptDefinition | null>(null);
  const [toast, setToast] = useState<{ msg: string; type: 'error' | 'success' } | null>(null);

  const [formName, setFormName] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [formType, setFormType] = useState('CONDITION');
  const [formKey, setFormKey] = useState('');
  const [formBody, setFormBody] = useState('// Write your script here\n');
  const [formChangeSummary, setFormChangeSummary] = useState('');
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  const [versions, setVersions] = useState<ScriptVersion[]>([]);
  const [showVersions, setShowVersions] = useState<string | null>(null);
  const [schedule, setSchedule] = useState<ScriptSchedule | null>(null);
  const [showSchedule, setShowSchedule] = useState<string | null>(null);
  const [cronInput, setCronInput] = useState('0 0 9 * * MON-FRI');

  // Listeners tab state
  const [listeners, setListeners] = useState<ScriptListener[]>([]);
  const [showAddListener, setShowAddListener] = useState(false);
  const [listenerScriptId, setListenerScriptId] = useState('');
  const [listenerEventType, setListenerEventType] = useState('ISSUE_CREATED');
  const [listenerProjectFilter, setListenerProjectFilter] = useState('');
  const [listenerIssueTypeFilter, setListenerIssueTypeFilter] = useState('');

  // Behaviors tab state
  const [behaviors, setBehaviors] = useState<ScriptFieldBehavior[]>([]);
  const [showAddBehavior, setShowAddBehavior] = useState(false);
  const [behaviorScriptId, setBehaviorScriptId] = useState('');
  const [behaviorScreenContext, setBehaviorScreenContext] = useState('CREATE');
  const [behaviorProjectFilter, setBehaviorProjectFilter] = useState('');
  const [behaviorIssueTypeFilter, setBehaviorIssueTypeFilter] = useState('');

  const EVENT_TYPES = [
    'ISSUE_CREATED', 'ISSUE_UPDATED', 'ISSUE_TRANSITIONED', 'ISSUE_DELETED',
    'COMMENT_ADDED', 'COMMENT_UPDATED', 'COMMENT_DELETED',
    'WORKLOG_ADDED', 'ATTACHMENT_ADDED', 'ATTACHMENT_DELETED',
    'VERSION_RELEASED', 'SPRINT_STARTED', 'SPRINT_COMPLETED',
  ];

  const SCREEN_CONTEXTS = ['CREATE', 'EDIT', 'TRANSITION', 'VIEW'];

  const showToast = (msg: string, type: 'error' | 'success') => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 5000);
  };

  const fetchScripts = async () => {
    setLoading(true);
    try {
      const res = await scriptApi.list();
      setScripts(res.data);
    } catch (err: unknown) {
      showToast(extractError(err), 'error');
    } finally {
      setLoading(false);
    }
  };

  const fetchLogs = async () => {
    try {
      const res = await scriptApi.getAllExecutionLogs(0, 50);
      setLogs(res.data.content || []);
    } catch (err: unknown) {
      showToast(extractError(err), 'error');
    }
  };

  useEffect(() => { fetchScripts(); }, []);
  useEffect(() => { if (activeTab === 'logs') fetchLogs(); }, [activeTab]);
  useEffect(() => { if (activeTab === 'listeners') { fetchListeners(); fetchScripts(); } }, [activeTab]);
  useEffect(() => { if (activeTab === 'behaviors') { fetchBehaviors(); fetchScripts(); } }, [activeTab]);

  const generateKey = (name: string) =>
    name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '').slice(0, 64);

  const openCreate = () => {
    setEditingScript(null);
    setFormName(''); setFormDescription(''); setFormType('CONDITION');
    setFormKey(''); setFormBody('// Write your script here\n');
    setFormChangeSummary(''); setFormError('');
    setShowModal(true);
  };

  const openEdit = (script: ScriptDefinition) => {
    setEditingScript(script);
    setFormName(script.name); setFormDescription(script.description || '');
    setFormType(script.scriptType); setFormKey(script.scriptKey);
    setFormBody(script.scriptBody); setFormChangeSummary(''); setFormError('');
    setShowModal(true);
  };

  const validateForm = (): string | null => {
    if (!formName.trim()) return 'Name is required';
    if (!editingScript && !KEY_PATTERN.test(formKey)) return 'Key must be 3-64 chars, lowercase letters/numbers/dashes, starting with a letter';
    if (!formBody.trim()) return 'Script body is required';
    return null;
  };

  const handleSave = async () => {
    const err = validateForm();
    if (err) { setFormError(err); return; }
    setSaving(true); setFormError('');
    try {
      if (editingScript) {
        await scriptApi.update(editingScript.id, {
          name: formName, description: formDescription,
          scriptBody: formBody, changeSummary: formChangeSummary || undefined,
        });
        showToast('Script updated', 'success');
      } else {
        await scriptApi.create({
          name: formName, description: formDescription,
          scriptType: formType, scriptKey: formKey, scriptBody: formBody,
        });
        showToast('Script created', 'success');
      }
      setShowModal(false);
      fetchScripts();
    } catch (err: unknown) {
      setFormError(extractError(err));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Delete this script? This cannot be undone.')) return;
    try {
      await scriptApi.delete(id);
      showToast('Script deleted', 'success');
      fetchScripts();
    } catch (err: unknown) {
      showToast(extractError(err), 'error');
    }
  };

  const handleToggle = async (id: string, enabled: boolean) => {
    try {
      await scriptApi.toggle(id, enabled);
      fetchScripts();
    } catch (err: unknown) {
      showToast(extractError(err), 'error');
    }
  };

  const loadVersions = async (scriptId: string) => {
    if (showVersions === scriptId) { setShowVersions(null); return; }
    try {
      const res = await scriptApi.getVersions(scriptId);
      setVersions(res.data);
      setShowVersions(scriptId);
    } catch (err: unknown) {
      showToast(extractError(err), 'error');
    }
  };

  const handleRevert = async (scriptId: string, version: number) => {
    if (!window.confirm(`Revert to version ${version}?`)) return;
    try {
      await scriptApi.revertToVersion(scriptId, version);
      showToast(`Reverted to version ${version}`, 'success');
      fetchScripts();
      loadVersions(scriptId);
    } catch (err: unknown) {
      showToast(extractError(err), 'error');
    }
  };

  const loadSchedule = async (scriptId: string) => {
    if (showSchedule === scriptId) { setShowSchedule(null); return; }
    try {
      const res = await scriptApi.getSchedule(scriptId);
      setSchedule(res.data);
    } catch { setSchedule(null); }
    setShowSchedule(scriptId);
  };

  const handleCreateSchedule = async (scriptId: string) => {
    try {
      await scriptApi.createSchedule(scriptId, cronInput);
      showToast('Schedule created', 'success');
      loadSchedule(scriptId);
    } catch (err: unknown) {
      showToast(extractError(err), 'error');
    }
  };

  const handleDeleteSchedule = async (scriptId: string) => {
    try {
      await scriptApi.deleteSchedule(scriptId);
      showToast('Schedule removed', 'success');
      setSchedule(null);
    } catch (err: unknown) {
      showToast(extractError(err), 'error');
    }
  };

  // === Listeners ===
  const fetchListeners = async () => {
    try {
      const res = await scriptApi.getAllListeners();
      setListeners(res.data);
    } catch (err: unknown) {
      showToast(extractError(err), 'error');
    }
  };

  const handleCreateListener = async () => {
    if (!listenerScriptId) { showToast('Please select a script', 'error'); return; }
    try {
      await scriptApi.createListener(listenerScriptId, {
        eventType: listenerEventType,
        projectFilter: listenerProjectFilter || undefined,
        issueTypeFilter: listenerIssueTypeFilter || undefined,
      });
      showToast('Listener created', 'success');
      setShowAddListener(false);
      setListenerScriptId(''); setListenerProjectFilter(''); setListenerIssueTypeFilter('');
      fetchListeners();
    } catch (err: unknown) {
      showToast(extractError(err), 'error');
    }
  };

  const handleToggleListener = async (listenerId: string) => {
    try {
      await scriptApi.toggleListener(listenerId);
      fetchListeners();
    } catch (err: unknown) {
      showToast(extractError(err), 'error');
    }
  };

  const handleDeleteListener = async (listenerId: string) => {
    if (!window.confirm('Delete this listener?')) return;
    try {
      await scriptApi.deleteListener(listenerId);
      showToast('Listener deleted', 'success');
      fetchListeners();
    } catch (err: unknown) {
      showToast(extractError(err), 'error');
    }
  };

  // === Behaviors ===
  const fetchBehaviors = async () => {
    try {
      const res = await scriptApi.getAllBehaviors();
      setBehaviors(res.data);
    } catch (err: unknown) {
      showToast(extractError(err), 'error');
    }
  };

  const handleCreateBehavior = async () => {
    if (!behaviorScriptId) { showToast('Please select a script', 'error'); return; }
    try {
      await scriptApi.createFieldBehavior(behaviorScriptId, {
        screenContext: behaviorScreenContext,
        projectId: behaviorProjectFilter || undefined,
        issueTypeId: behaviorIssueTypeFilter || undefined,
      });
      showToast('Behavior created', 'success');
      setShowAddBehavior(false);
      setBehaviorScriptId(''); setBehaviorProjectFilter(''); setBehaviorIssueTypeFilter('');
      fetchBehaviors();
    } catch (err: unknown) {
      showToast(extractError(err), 'error');
    }
  };

  const handleDeleteBehavior = async (behaviorId: string) => {
    if (!window.confirm('Delete this behavior?')) return;
    try {
      await scriptApi.deleteFieldBehavior(behaviorId);
      showToast('Behavior deleted', 'success');
      fetchBehaviors();
    } catch (err: unknown) {
      showToast(extractError(err), 'error');
    }
  };

  const handleExport = async (id: string) => {
    try {
      const res = await scriptApi.exportScript(id);
      const blob = new Blob([JSON.stringify(res.data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a'); a.href = url; a.download = `script-export.json`; a.click();
      URL.revokeObjectURL(url);
    } catch (err: unknown) {
      showToast(extractError(err), 'error');
    }
  };

  const tabs: { key: Tab; label: string }[] = [
    { key: 'scripts', label: 'Scripts' },
    { key: 'console', label: 'Console' },
    { key: 'logs', label: 'Execution Log' },
    { key: 'listeners', label: 'Listeners' },
    { key: 'behaviors', label: 'Behaviors' },
    { key: 'profiler', label: 'Profiler' },
  ];

  const getScriptName = (scriptId: string) => {
    const script = scripts.find(s => s.id === scriptId);
    return script ? script.name : scriptId;
  };

  const getScriptKey = (scriptId: string) => {
    const script = scripts.find(s => s.id === scriptId);
    return script ? script.scriptKey : '';
  };

  return (
    <div>
      {toast && (
        <div className={`fixed top-4 right-4 z-50 px-4 py-3 rounded shadow-lg text-sm font-medium ${
          toast.type === 'error' ? 'bg-red-100 text-red-800 border border-red-300' : 'bg-green-100 text-green-800 border border-green-300'
        }`}>
          {toast.msg}
        </div>
      )}

      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-xl font-bold">JDC Script Engine</h2>
          <p className="text-sm text-gray-600 mt-1">
            Create and manage JavaScript scripts for workflow conditions, validators, and post-functions.
          </p>
        </div>
        {activeTab === 'scripts' && (
          <button onClick={openCreate} className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded hover:bg-blue-700">
            Create Script
          </button>
        )}
      </div>

      <div className="flex gap-1 border-b border-gray-200 mb-4">
        {tabs.map((tab) => (
          <button key={tab.key} onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors ${
              activeTab === tab.key ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-600 hover:text-gray-800'
            }`}>{tab.label}</button>
        ))}
      </div>

      {activeTab === 'scripts' && (
        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
          {loading ? (
            <div className="p-8 text-center text-gray-500">Loading scripts...</div>
          ) : scripts.length === 0 ? (
            <div className="p-8 text-center text-gray-500">No scripts yet. Click "Create Script" to get started.</div>
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left px-4 py-3 font-medium text-gray-700">Name</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-700">Key</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-700">Type</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-700">Version</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-700">Enabled</th>
                  <th className="text-right px-4 py-3 font-medium text-gray-700">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {scripts.map((script) => (
                  <Fragment key={script.id}>
                    <tr className="hover:bg-gray-50">
                      <td className="px-4 py-3">
                        <div className="font-medium">{script.name}</div>
                        {script.description && <div className="text-xs text-gray-500 mt-0.5 truncate max-w-xs">{script.description}</div>}
                      </td>
                      <td className="px-4 py-3"><code className="text-xs bg-gray-100 px-1.5 py-0.5 rounded">{script.scriptKey}</code></td>
                      <td className="px-4 py-3">
                        <span className={`inline-block text-xs px-2 py-0.5 rounded-full font-medium ${
                          script.scriptType === 'CONDITION' ? 'bg-blue-100 text-blue-800'
                          : script.scriptType === 'VALIDATOR' ? 'bg-amber-100 text-amber-800'
                          : 'bg-green-100 text-green-800'
                        }`}>{TYPE_LABELS[script.scriptType] || script.scriptType}</span>
                      </td>
                      <td className="px-4 py-3 text-gray-600">v{script.version}</td>
                      <td className="px-4 py-3">
                        <button onClick={() => handleToggle(script.id, !script.isEnabled)}
                          className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors ${script.isEnabled ? 'bg-green-500' : 'bg-gray-300'}`}>
                          <span className={`inline-block h-3.5 w-3.5 rounded-full bg-white transition-transform ${script.isEnabled ? 'translate-x-4' : 'translate-x-1'}`} />
                        </button>
                      </td>
                      <td className="px-4 py-3 text-right space-x-2">
                        <button onClick={() => navigate(`/workflows/admin/scripts/${script.id}/edit`)} className="text-indigo-600 hover:text-indigo-800 text-sm font-medium">Open Editor</button>
                        <button onClick={() => openEdit(script)} className="text-blue-600 hover:text-blue-800 text-sm">Edit</button>
                        <button onClick={() => loadVersions(script.id)} className="text-gray-600 hover:text-gray-800 text-sm">Versions</button>
                        <button onClick={() => loadSchedule(script.id)} className="text-purple-600 hover:text-purple-800 text-sm">Schedule</button>
                        <button onClick={() => handleExport(script.id)} className="text-gray-600 hover:text-gray-800 text-sm">Export</button>
                        <button onClick={() => handleDelete(script.id)} className="text-red-600 hover:text-red-800 text-sm">Delete</button>
                      </td>
                    </tr>
                    {showVersions === script.id && (
                      <tr key={`${script.id}-versions`}>
                        <td colSpan={6} className="px-4 py-3 bg-gray-50">
                          <div className="text-xs font-semibold mb-2">Version History</div>
                          {versions.length === 0 ? <div className="text-xs text-gray-500">No versions</div> : (
                            <table className="w-full text-xs">
                              <thead><tr><th className="text-left py-1">Version</th><th className="text-left py-1">Summary</th><th className="text-left py-1">Date</th><th className="text-right py-1">Action</th></tr></thead>
                              <tbody>
                                {versions.map((v) => (
                                  <tr key={v.id} className="border-t border-gray-200">
                                    <td className="py-1">v{v.version}</td>
                                    <td className="py-1 text-gray-600">{v.changeSummary || 'No summary'}</td>
                                    <td className="py-1 text-gray-500">{new Date(v.createdAt).toLocaleString()}</td>
                                    <td className="py-1 text-right">
                                      {v.version !== script.version && (
                                        <button onClick={() => handleRevert(script.id, v.version)} className="text-blue-600 hover:text-blue-800">Revert</button>
                                      )}
                                    </td>
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          )}
                        </td>
                      </tr>
                    )}
                    {showSchedule === script.id && (
                      <tr key={`${script.id}-schedule`}>
                        <td colSpan={6} className="px-4 py-3 bg-purple-50">
                          <div className="text-xs font-semibold mb-2">Schedule</div>
                          {schedule ? (
                            <div className="flex items-center gap-4 text-xs">
                              <span>Cron: <code className="bg-white px-1 rounded">{schedule.cronExpression}</code></span>
                              <span>Enabled: {schedule.isEnabled ? 'Yes' : 'No'}</span>
                              <span>Runs: {schedule.runCount}</span>
                              {schedule.nextRunAt && <span>Next: {new Date(schedule.nextRunAt).toLocaleString()}</span>}
                              <button onClick={() => handleDeleteSchedule(script.id)} className="text-red-600 hover:text-red-800">Remove</button>
                            </div>
                          ) : (
                            <div className="flex items-center gap-2">
                              <input type="text" value={cronInput} onChange={(e) => setCronInput(e.target.value)}
                                className="border border-gray-300 rounded px-2 py-1 text-xs w-48" placeholder="Cron expression" />
                              <button onClick={() => handleCreateSchedule(script.id)} className="text-purple-600 hover:text-purple-800 text-xs font-medium">Create Schedule</button>
                            </div>
                          )}
                        </td>
                      </tr>
                    )}
                  </Fragment>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {activeTab === 'console' && <ScriptConsole />}

      {activeTab === 'logs' && (
        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
          {logs.length === 0 ? (
            <div className="p-8 text-center text-gray-500">No execution logs yet.</div>
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left px-4 py-3 font-medium text-gray-700">Script</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-700">Type</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-700">Mode</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-700">Result</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-700">Time</th>
                  <th className="text-left px-4 py-3 font-medium text-gray-700">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3"><code className="text-xs bg-gray-100 px-1.5 py-0.5 rounded">{log.scriptKey}</code></td>
                    <td className="px-4 py-3 text-gray-600">{TYPE_LABELS[log.scriptType] || log.scriptType}</td>
                    <td className="px-4 py-3">
                      <span className={`text-xs px-2 py-0.5 rounded ${
                        log.executionMode === 'CONSOLE' ? 'bg-purple-100 text-purple-700'
                        : log.executionMode === 'SCHEDULED' ? 'bg-orange-100 text-orange-700'
                        : 'bg-gray-100 text-gray-700'
                      }`}>{log.executionMode}</span>
                    </td>
                    <td className="px-4 py-3">
                      {log.success ? <span className="text-green-600 font-medium">OK</span>
                        : <span className="text-red-600 font-medium" title={log.errorMessage || ''}>FAIL</span>}
                      {log.resultValue && <span className="ml-2 text-xs text-gray-500">{log.resultValue}</span>}
                    </td>
                    <td className="px-4 py-3 text-gray-600">{log.executionMs}ms</td>
                    <td className="px-4 py-3 text-gray-500 text-xs">{new Date(log.createdAt).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {activeTab === 'listeners' && (
        <div>
          <div className="flex justify-end mb-3">
            <button onClick={() => setShowAddListener(!showAddListener)}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded hover:bg-blue-700">
              {showAddListener ? 'Cancel' : 'Add Listener'}
            </button>
          </div>

          {showAddListener && (
            <div className="bg-white border border-gray-200 rounded-lg p-4 mb-4 space-y-3">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Script</label>
                  <select value={listenerScriptId} onChange={(e) => setListenerScriptId(e.target.value)}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm">
                    <option value="">Select a script...</option>
                    {scripts.map(s => <option key={s.id} value={s.id}>{s.name} ({s.scriptKey})</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Event Type</label>
                  <select value={listenerEventType} onChange={(e) => setListenerEventType(e.target.value)}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm">
                    {EVENT_TYPES.map(et => <option key={et} value={et}>{et}</option>)}
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Project Filter (UUID, optional)</label>
                  <input type="text" value={listenerProjectFilter} onChange={(e) => setListenerProjectFilter(e.target.value)}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm" placeholder="Project UUID (optional)" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Issue Type Filter (UUID, optional)</label>
                  <input type="text" value={listenerIssueTypeFilter} onChange={(e) => setListenerIssueTypeFilter(e.target.value)}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm" placeholder="Issue Type UUID (optional)" />
                </div>
              </div>
              <div className="flex justify-end">
                <button onClick={handleCreateListener}
                  disabled={!listenerScriptId}
                  className="px-4 py-2 text-sm font-medium text-white bg-green-600 rounded hover:bg-green-700 disabled:opacity-50">
                  Create
                </button>
              </div>
            </div>
          )}

          <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
            {listeners.length === 0 ? (
              <div className="p-8 text-center text-gray-500">No listeners configured. Click "Add Listener" to get started.</div>
            ) : (
              <table className="w-full text-sm">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    <th className="text-left px-4 py-3 font-medium text-gray-700">Script Name</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-700">Key</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-700">Event Type</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-700">Project Filter</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-700">Issue Type Filter</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-700">Enabled</th>
                    <th className="text-right px-4 py-3 font-medium text-gray-700">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {listeners.map((listener) => (
                    <tr key={listener.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3 font-medium">{getScriptName(listener.scriptId)}</td>
                      <td className="px-4 py-3"><code className="text-xs bg-gray-100 px-1.5 py-0.5 rounded">{getScriptKey(listener.scriptId)}</code></td>
                      <td className="px-4 py-3">
                        <span className="text-xs px-2 py-0.5 rounded bg-indigo-100 text-indigo-700">{listener.eventType}</span>
                      </td>
                      <td className="px-4 py-3 text-gray-600 text-xs">{listener.projectFilter || '-'}</td>
                      <td className="px-4 py-3 text-gray-600 text-xs">{listener.issueTypeFilter || '-'}</td>
                      <td className="px-4 py-3">
                        <button onClick={() => handleToggleListener(listener.id)}
                          className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors ${listener.isEnabled ? 'bg-green-500' : 'bg-gray-300'}`}>
                          <span className={`inline-block h-3.5 w-3.5 rounded-full bg-white transition-transform ${listener.isEnabled ? 'translate-x-4' : 'translate-x-1'}`} />
                        </button>
                      </td>
                      <td className="px-4 py-3 text-right">
                        <button onClick={() => handleDeleteListener(listener.id)} className="text-red-600 hover:text-red-800 text-sm">Delete</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}

      {activeTab === 'behaviors' && (
        <div>
          <div className="flex justify-end mb-3">
            <button onClick={() => setShowAddBehavior(!showAddBehavior)}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded hover:bg-blue-700">
              {showAddBehavior ? 'Cancel' : 'Add Behavior'}
            </button>
          </div>

          {showAddBehavior && (
            <div className="bg-white border border-gray-200 rounded-lg p-4 mb-4 space-y-3">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Script</label>
                  <select value={behaviorScriptId} onChange={(e) => setBehaviorScriptId(e.target.value)}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm">
                    <option value="">Select a script...</option>
                    {scripts.map(s => <option key={s.id} value={s.id}>{s.name} ({s.scriptKey})</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Screen Context</label>
                  <select value={behaviorScreenContext} onChange={(e) => setBehaviorScreenContext(e.target.value)}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm">
                    {SCREEN_CONTEXTS.map(sc => <option key={sc} value={sc}>{sc}</option>)}
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Project Filter (UUID, optional)</label>
                  <input type="text" value={behaviorProjectFilter} onChange={(e) => setBehaviorProjectFilter(e.target.value)}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm" placeholder="Project UUID (optional)" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Issue Type Filter (UUID, optional)</label>
                  <input type="text" value={behaviorIssueTypeFilter} onChange={(e) => setBehaviorIssueTypeFilter(e.target.value)}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm" placeholder="Issue Type UUID (optional)" />
                </div>
              </div>
              <div className="flex justify-end">
                <button onClick={handleCreateBehavior}
                  disabled={!behaviorScriptId}
                  className="px-4 py-2 text-sm font-medium text-white bg-green-600 rounded hover:bg-green-700 disabled:opacity-50">
                  Create
                </button>
              </div>
            </div>
          )}

          <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
            {behaviors.length === 0 ? (
              <div className="p-8 text-center text-gray-500">No field behaviors configured. Click "Add Behavior" to get started.</div>
            ) : (
              <table className="w-full text-sm">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    <th className="text-left px-4 py-3 font-medium text-gray-700">Script Name</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-700">Key</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-700">Screen Context</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-700">Project Filter</th>
                    <th className="text-left px-4 py-3 font-medium text-gray-700">Issue Type Filter</th>
                    <th className="text-right px-4 py-3 font-medium text-gray-700">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {behaviors.map((behavior) => (
                    <tr key={behavior.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3 font-medium">{getScriptName(behavior.scriptId)}</td>
                      <td className="px-4 py-3"><code className="text-xs bg-gray-100 px-1.5 py-0.5 rounded">{getScriptKey(behavior.scriptId)}</code></td>
                      <td className="px-4 py-3">
                        <span className="text-xs px-2 py-0.5 rounded bg-teal-100 text-teal-700">{behavior.screenContext}</span>
                      </td>
                      <td className="px-4 py-3 text-gray-600 text-xs">{behavior.projectId || '-'}</td>
                      <td className="px-4 py-3 text-gray-600 text-xs">{behavior.issueTypeId || '-'}</td>
                      <td className="px-4 py-3 text-right">
                        <button onClick={() => handleDeleteBehavior(behavior.id)} className="text-red-600 hover:text-red-800 text-sm">Delete</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}

      {activeTab === 'profiler' && (
        <ProfilerDashboard />
      )}

      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-5xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
              <h3 className="text-lg font-bold">{editingScript ? 'Edit Script' : 'Create Script'}</h3>
              <button onClick={() => setShowModal(false)} className="text-gray-400 hover:text-gray-600 text-xl">&times;</button>
            </div>

            <div className="p-6 space-y-4">
              {formError && <div className="p-3 bg-red-50 border border-red-200 rounded text-sm text-red-700">{formError}</div>}

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
                  <input type="text" value={formName}
                    onChange={(e) => { setFormName(e.target.value); if (!editingScript) setFormKey(generateKey(e.target.value)); }}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm" placeholder="Auto-assign Critical Issues" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Script Key</label>
                  <input type="text" value={formKey} onChange={(e) => setFormKey(e.target.value)}
                    disabled={!!editingScript}
                    className={`w-full border rounded px-3 py-2 text-sm disabled:bg-gray-100 ${!editingScript && formKey && !KEY_PATTERN.test(formKey) ? 'border-red-400' : 'border-gray-300'}`}
                    placeholder="auto-assign-critical" />
                  {!editingScript && formKey && !KEY_PATTERN.test(formKey) && (
                    <p className="text-xs text-red-500 mt-1">3-64 chars, lowercase a-z, 0-9, dashes, must start with a letter</p>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Type</label>
                  <select value={formType} onChange={(e) => setFormType(e.target.value)}
                    disabled={!!editingScript}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm disabled:bg-gray-100">
                    <option value="CONDITION">Condition (returns boolean)</option>
                    <option value="VALIDATOR">Validator (returns error or null)</option>
                    <option value="POST_FUNCTION">Post-Function (side effects)</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                  <input type="text" value={formDescription} onChange={(e) => setFormDescription(e.target.value)}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm" placeholder="What does this script do?" />
                </div>
              </div>

              {editingScript && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Change Summary</label>
                  <input type="text" value={formChangeSummary} onChange={(e) => setFormChangeSummary(e.target.value)}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm" placeholder="What changed in this version?" />
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Script Body (JavaScript)</label>
                <div className="border border-gray-300 rounded overflow-hidden">
                  <Editor height="350px" language="javascript" theme="vs-dark" value={formBody}
                    onChange={(value) => setFormBody(value || '')}
                    options={{ minimap: { enabled: false }, lineNumbers: 'on', wordWrap: 'on', scrollBeyondLastLine: false, fontSize: 13, tabSize: 2 }} />
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-3 px-6 py-4 border-t border-gray-200 bg-gray-50">
              <button onClick={() => setShowModal(false)} className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded hover:bg-gray-100">Cancel</button>
              <button onClick={handleSave}
                disabled={saving || !formName || (!editingScript && (!formKey || !KEY_PATTERN.test(formKey))) || !formBody.trim()}
                className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded hover:bg-blue-700 disabled:opacity-50">
                {saving ? 'Saving...' : editingScript ? 'Update Script' : 'Create Script'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
