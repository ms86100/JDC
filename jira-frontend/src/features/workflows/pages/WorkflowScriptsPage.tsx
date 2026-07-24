import { useEffect, useState } from 'react';
import Editor from '@monaco-editor/react';
import { scriptApi, ScriptDefinition, ScriptExecutionLog } from '../../../api/scriptApi';
import ScriptConsole from '../components/ScriptConsole';

type Tab = 'scripts' | 'console' | 'logs';

const TYPE_LABELS: Record<string, string> = {
  CONDITION: 'Condition',
  VALIDATOR: 'Validator',
  POST_FUNCTION: 'Post-Function',
};

export default function WorkflowScriptsPage() {
  const [activeTab, setActiveTab] = useState<Tab>('scripts');
  const [scripts, setScripts] = useState<ScriptDefinition[]>([]);
  const [logs, setLogs] = useState<ScriptExecutionLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editingScript, setEditingScript] = useState<ScriptDefinition | null>(null);

  const [formName, setFormName] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [formType, setFormType] = useState('CONDITION');
  const [formKey, setFormKey] = useState('');
  const [formBody, setFormBody] = useState('// Write your script here\n');
  const [formChangeSummary, setFormChangeSummary] = useState('');
  const [saving, setSaving] = useState(false);

  const fetchScripts = async () => {
    setLoading(true);
    try {
      const res = await scriptApi.list();
      setScripts(res.data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load';
      alert(msg);
    } finally {
      setLoading(false);
    }
  };

  const fetchLogs = async () => {
    try {
      const res = await scriptApi.getAllExecutionLogs(0, 50);
      setLogs(res.data.content || []);
    } catch {
      /* ignore */
    }
  };

  useEffect(() => {
    fetchScripts();
  }, []);

  useEffect(() => {
    if (activeTab === 'logs') fetchLogs();
  }, [activeTab]);

  const generateKey = (name: string) =>
    name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '').slice(0, 64);

  const openCreate = () => {
    setEditingScript(null);
    setFormName('');
    setFormDescription('');
    setFormType('CONDITION');
    setFormKey('');
    setFormBody('// Write your script here\n');
    setFormChangeSummary('');
    setShowModal(true);
  };

  const openEdit = (script: ScriptDefinition) => {
    setEditingScript(script);
    setFormName(script.name);
    setFormDescription(script.description || '');
    setFormType(script.scriptType);
    setFormKey(script.scriptKey);
    setFormBody(script.scriptBody);
    setFormChangeSummary('');
    setShowModal(true);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      if (editingScript) {
        await scriptApi.update(editingScript.id, {
          name: formName,
          description: formDescription,
          scriptBody: formBody,
          changeSummary: formChangeSummary || undefined,
        });
      } else {
        await scriptApi.create({
          name: formName,
          description: formDescription,
          scriptType: formType,
          scriptKey: formKey,
          scriptBody: formBody,
        });
      }
      setShowModal(false);
      fetchScripts();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load';
      alert(msg);
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this script?')) return;
    try {
      await scriptApi.delete(id);
      fetchScripts();
    } catch {
      /* ignore */
    }
  };

  const handleToggle = async (id: string, enabled: boolean) => {
    try {
      await scriptApi.toggle(id, enabled);
      fetchScripts();
    } catch {
      /* ignore */
    }
  };

  const tabs: { key: Tab; label: string }[] = [
    { key: 'scripts', label: 'Scripts' },
    { key: 'console', label: 'Console' },
    { key: 'logs', label: 'Execution Log' },
  ];

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-xl font-bold">JDC Script Engine</h2>
          <p className="text-sm text-gray-600 mt-1">
            Create and manage JavaScript scripts for workflow conditions, validators, and post-functions.
          </p>
        </div>
        {activeTab === 'scripts' && (
          <button
            onClick={openCreate}
            className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded hover:bg-blue-700"
          >
            Create Script
          </button>
        )}
      </div>

      <div className="flex gap-1 border-b border-gray-200 mb-4">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors ${
              activeTab === tab.key
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-600 hover:text-gray-800'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'scripts' && (
        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
          {loading ? (
            <div className="p-8 text-center text-gray-500">Loading scripts...</div>
          ) : scripts.length === 0 ? (
            <div className="p-8 text-center text-gray-500">
              No scripts yet. Click "Create Script" to get started.
            </div>
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
                  <tr key={script.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3">
                      <div className="font-medium">{script.name}</div>
                      {script.description && (
                        <div className="text-xs text-gray-500 mt-0.5 truncate max-w-xs">
                          {script.description}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <code className="text-xs bg-gray-100 px-1.5 py-0.5 rounded">{script.scriptKey}</code>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-block text-xs px-2 py-0.5 rounded-full font-medium ${
                        script.scriptType === 'CONDITION'
                          ? 'bg-blue-100 text-blue-800'
                          : script.scriptType === 'VALIDATOR'
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-green-100 text-green-800'
                      }`}>
                        {TYPE_LABELS[script.scriptType] || script.scriptType}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-600">v{script.version}</td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => handleToggle(script.id, !script.isEnabled)}
                        className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors ${
                          script.isEnabled ? 'bg-green-500' : 'bg-gray-300'
                        }`}
                      >
                        <span
                          className={`inline-block h-3.5 w-3.5 rounded-full bg-white transition-transform ${
                            script.isEnabled ? 'translate-x-4' : 'translate-x-1'
                          }`}
                        />
                      </button>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => openEdit(script)}
                        className="text-blue-600 hover:text-blue-800 text-sm mr-3"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDelete(script.id)}
                        className="text-red-600 hover:text-red-800 text-sm"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
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
                    <td className="px-4 py-3">
                      <code className="text-xs bg-gray-100 px-1.5 py-0.5 rounded">{log.scriptKey}</code>
                    </td>
                    <td className="px-4 py-3 text-gray-600">{TYPE_LABELS[log.scriptType] || log.scriptType}</td>
                    <td className="px-4 py-3">
                      <span className={`text-xs px-2 py-0.5 rounded ${
                        log.executionMode === 'CONSOLE'
                          ? 'bg-purple-100 text-purple-700'
                          : 'bg-gray-100 text-gray-700'
                      }`}>
                        {log.executionMode}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      {log.success ? (
                        <span className="text-green-600 font-medium">OK</span>
                      ) : (
                        <span className="text-red-600 font-medium" title={log.errorMessage || ''}>
                          FAIL
                        </span>
                      )}
                      {log.resultValue && (
                        <span className="ml-2 text-xs text-gray-500">{log.resultValue}</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-gray-600">{log.executionMs}ms</td>
                    <td className="px-4 py-3 text-gray-500 text-xs">
                      {new Date(log.createdAt).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-5xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
              <h3 className="text-lg font-bold">
                {editingScript ? 'Edit Script' : 'Create Script'}
              </h3>
              <button onClick={() => setShowModal(false)} className="text-gray-400 hover:text-gray-600 text-xl">
                &times;
              </button>
            </div>

            <div className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
                  <input
                    type="text"
                    value={formName}
                    onChange={(e) => {
                      setFormName(e.target.value);
                      if (!editingScript) setFormKey(generateKey(e.target.value));
                    }}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
                    placeholder="Auto-assign Critical Issues"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Script Key</label>
                  <input
                    type="text"
                    value={formKey}
                    onChange={(e) => setFormKey(e.target.value)}
                    disabled={!!editingScript}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm disabled:bg-gray-100"
                    placeholder="auto-assign-critical"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Type</label>
                  <select
                    value={formType}
                    onChange={(e) => setFormType(e.target.value)}
                    disabled={!!editingScript}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm disabled:bg-gray-100"
                  >
                    <option value="CONDITION">Condition (returns boolean)</option>
                    <option value="VALIDATOR">Validator (returns error or null)</option>
                    <option value="POST_FUNCTION">Post-Function (side effects)</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                  <input
                    type="text"
                    value={formDescription}
                    onChange={(e) => setFormDescription(e.target.value)}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
                    placeholder="What does this script do?"
                  />
                </div>
              </div>

              {editingScript && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Change Summary</label>
                  <input
                    type="text"
                    value={formChangeSummary}
                    onChange={(e) => setFormChangeSummary(e.target.value)}
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
                    placeholder="What changed in this version?"
                  />
                </div>
              )}

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Script Body (JavaScript)</label>
                <div className="border border-gray-300 rounded overflow-hidden">
                  <Editor
                    height="350px"
                    language="javascript"
                    theme="vs-dark"
                    value={formBody}
                    onChange={(value) => setFormBody(value || '')}
                    options={{
                      minimap: { enabled: false },
                      lineNumbers: 'on',
                      wordWrap: 'on',
                      scrollBeyondLastLine: false,
                      fontSize: 13,
                      tabSize: 2,
                    }}
                  />
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-3 px-6 py-4 border-t border-gray-200 bg-gray-50">
              <button
                onClick={() => setShowModal(false)}
                className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded hover:bg-gray-100"
              >
                Cancel
              </button>
              <button
                onClick={handleSave}
                disabled={saving || !formName || !formKey || !formBody.trim()}
                className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded hover:bg-blue-700 disabled:opacity-50"
              >
                {saving ? 'Saving...' : editingScript ? 'Update Script' : 'Create Script'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
