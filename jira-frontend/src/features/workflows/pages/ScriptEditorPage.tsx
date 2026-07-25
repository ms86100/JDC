import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams, Link, useNavigate, useBlocker } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import Editor, { useMonaco } from '@monaco-editor/react';
import { scriptApi, ScriptVersion } from '../../../api/scriptApi';
import { appNotify } from '../../../lib/appNotify';
import { registerJdcCompletionProvider } from '../utils/jdcCompletionProvider';
import './ScriptEditorPage.css';

interface UsageData {
  scriptKey: string;
  listeners: unknown[];
  fieldBehaviors: unknown[];
  calculatedFields: unknown[];
  includedBy: string[];
}

function extractError(err: unknown): string {
  if (err && typeof err === 'object') {
    const axiosErr = err as {
      response?: { data?: { message?: string; error?: string; validationErrors?: Record<string, string> } };
      message?: string;
    };
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

export default function ScriptEditorPage() {
  const { scriptId } = useParams<{ scriptId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const monaco = useMonaco();

  const [editorBody, setEditorBody] = useState('');
  const [savedBody, setSavedBody] = useState('');
  const [changeSummary, setChangeSummary] = useState('');
  const [showSaveDialog, setShowSaveDialog] = useState(false);
  const [previewVersion, setPreviewVersion] = useState<ScriptVersion | null>(null);
  const [showConsole, setShowConsole] = useState(false);
  const [consoleOutput, setConsoleOutput] = useState('');

  const completionDisposableRef = useRef<{ dispose: () => void } | null>(null);

  // Register JDC autocomplete provider
  useEffect(() => {
    if (monaco) {
      if (completionDisposableRef.current) {
        completionDisposableRef.current.dispose();
      }
      completionDisposableRef.current = registerJdcCompletionProvider(monaco);
    }
    return () => {
      if (completionDisposableRef.current) {
        completionDisposableRef.current.dispose();
        completionDisposableRef.current = null;
      }
    };
  }, [monaco]);

  // Fetch script
  const {
    data: script,
    isLoading,
    isError,
    error: fetchError,
  } = useQuery({
    queryKey: ['script-detail', scriptId],
    queryFn: () => scriptApi.get(scriptId!).then((r) => r.data),
    enabled: !!scriptId,
  });

  // Sync editor body when script loads
  useEffect(() => {
    if (script) {
      setEditorBody(script.scriptBody);
      setSavedBody(script.scriptBody);
    }
  }, [script]);

  // Fetch versions
  const { data: versions = [] } = useQuery({
    queryKey: ['script-versions', scriptId],
    queryFn: () => scriptApi.getVersions(scriptId!).then((r) => r.data),
    enabled: !!scriptId,
  });

  // Fetch usage
  const { data: usage } = useQuery<UsageData>({
    queryKey: ['script-usage', scriptId],
    queryFn: () => scriptApi.getUsage(scriptId!).then((r) => r.data),
    enabled: !!scriptId,
  });

  const hasUnsavedChanges = editorBody !== savedBody;

  // Block navigation when unsaved changes exist
  const blocker = useBlocker(hasUnsavedChanges);

  useEffect(() => {
    if (blocker.state === 'blocked') {
      const leave = window.confirm(
        'You have unsaved changes. Are you sure you want to leave?'
      );
      if (leave) {
        blocker.proceed();
      } else {
        blocker.reset();
      }
    }
  }, [blocker]);

  // Warn on browser close/refresh
  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      if (hasUnsavedChanges) {
        e.preventDefault();
      }
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [hasUnsavedChanges]);

  // Save mutation
  const saveMutation = useMutation({
    mutationFn: (summary: string) =>
      scriptApi.update(scriptId!, {
        scriptBody: editorBody,
        changeSummary: summary || undefined,
      }),
    onSuccess: (res) => {
      const updated = res.data;
      setSavedBody(updated.scriptBody);
      setChangeSummary('');
      setShowSaveDialog(false);
      queryClient.invalidateQueries({ queryKey: ['script-detail', scriptId] });
      queryClient.invalidateQueries({ queryKey: ['script-versions', scriptId] });
      appNotify.success('Script saved successfully');
    },
    onError: (err: unknown) => {
      appNotify.error(extractError(err));
    },
  });

  // Validate mutation
  const validateMutation = useMutation({
    mutationFn: () => scriptApi.validate(editorBody),
    onSuccess: (res) => {
      const result = res.data;
      if (result.valid) {
        appNotify.success('Script is valid');
      } else {
        appNotify.error(`Validation error: ${result.error || 'Unknown error'}`);
      }
    },
    onError: (err: unknown) => {
      appNotify.error(extractError(err));
    },
  });

  // Run in console mutation
  const runMutation = useMutation({
    mutationFn: () =>
      scriptApi.executeConsole({
        scriptBody: editorBody,
        scriptType: script?.scriptType || 'CONSOLE',
        context: {},
      }),
    onSuccess: (res) => {
      const result = res.data;
      setShowConsole(true);
      if (result.success) {
        const output = [
          result.consoleOutput || '',
          result.result !== undefined && result.result !== null
            ? `=> ${JSON.stringify(result.result, null, 2)}`
            : '',
          `Execution time: ${result.executionMs}ms`,
        ]
          .filter(Boolean)
          .join('\n');
        setConsoleOutput(output);
        appNotify.success(`Executed in ${result.executionMs}ms`);
      } else {
        setConsoleOutput(`Error: ${result.errorMessage || 'Unknown error'}\n\n${result.consoleOutput || ''}`);
        appNotify.error(result.errorMessage || 'Execution failed');
      }
    },
    onError: (err: unknown) => {
      appNotify.error(extractError(err));
    },
  });

  // Export handler
  const handleExport = useCallback(async () => {
    if (!scriptId) return;
    try {
      const res = await scriptApi.exportScript(scriptId);
      const blob = new Blob([JSON.stringify(res.data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${script?.scriptKey || 'script'}-export.json`;
      a.click();
      URL.revokeObjectURL(url);
      appNotify.success('Script exported');
    } catch (err: unknown) {
      appNotify.error(extractError(err));
    }
  }, [scriptId, script?.scriptKey]);

  // Save with Ctrl+S
  const handleSave = useCallback(() => {
    if (!hasUnsavedChanges) return;
    setShowSaveDialog(true);
  }, [hasUnsavedChanges]);

  const confirmSave = useCallback(() => {
    saveMutation.mutate(changeSummary);
  }, [saveMutation, changeSummary]);

  // Keyboard shortcut for Ctrl+S
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        e.preventDefault();
        handleSave();
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [handleSave]);

  // Version click handler
  const handleVersionClick = (version: ScriptVersion) => {
    if (previewVersion?.id === version.id) {
      setPreviewVersion(null);
    } else {
      setPreviewVersion(version);
    }
  };

  if (!scriptId) {
    return (
      <div className="se-page">
        <div className="se-error">Missing script ID.</div>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="se-page">
        <div className="se-loading">Loading script...</div>
      </div>
    );
  }

  if (isError || !script) {
    return (
      <div className="se-page">
        <div className="se-error">
          {extractError(fetchError) || 'Failed to load script.'}
          <Link to="/workflows/admin/scripts" className="ab-btn ab-btn-secondary ab-btn-sm">
            Back to Scripts
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="se-page">
      {/* Header / Toolbar */}
      <header className="se-header">
        <div className="se-header-left">
          <Link to="/workflows/admin/scripts" className="se-back-link">
            &larr; Back to Scripts
          </Link>
          <div className="se-title-group">
            <h1 className="se-title">{script.name}</h1>
            <code className="se-key">{script.scriptKey}</code>
            {hasUnsavedChanges && <span className="se-unsaved-badge">Unsaved</span>}
          </div>
        </div>
        <div className="se-header-actions">
          <button
            type="button"
            className="ab-btn ab-btn-secondary"
            onClick={handleExport}
          >
            Export
          </button>
          <button
            type="button"
            className="ab-btn ab-btn-secondary"
            onClick={() => runMutation.mutate()}
            disabled={runMutation.isPending}
          >
            {runMutation.isPending ? 'Running...' : 'Run in Console'}
          </button>
          <button
            type="button"
            className="ab-btn ab-btn-secondary"
            onClick={() => validateMutation.mutate()}
            disabled={validateMutation.isPending}
          >
            {validateMutation.isPending ? 'Checking...' : 'Validate'}
          </button>
          <button
            type="button"
            className="ab-btn ab-btn-primary"
            onClick={handleSave}
            disabled={!hasUnsavedChanges || saveMutation.isPending}
          >
            {saveMutation.isPending ? 'Saving...' : 'Save'}
          </button>
        </div>
      </header>

      {/* Main body */}
      <div className="se-body">
        {/* Editor area */}
        <div className="se-editor-area">
          <div className="se-editor-container">
            {previewVersion ? (
              <div className="se-preview-banner">
                Previewing version {previewVersion.version}
                {previewVersion.changeSummary && ` - ${previewVersion.changeSummary}`}
                <button
                  type="button"
                  className="ab-btn ab-btn-sm ab-btn-secondary"
                  onClick={() => setPreviewVersion(null)}
                >
                  Close preview
                </button>
                <button
                  type="button"
                  className="ab-btn ab-btn-sm ab-btn-primary"
                  onClick={() => {
                    setEditorBody(previewVersion.scriptBody);
                    setPreviewVersion(null);
                  }}
                >
                  Restore this version
                </button>
              </div>
            ) : null}
            <Editor
              height="calc(100vh - 140px)"
              language="javascript"
              theme="vs-dark"
              value={previewVersion ? previewVersion.scriptBody : editorBody}
              onChange={(value) => {
                if (!previewVersion) {
                  setEditorBody(value || '');
                }
              }}
              options={{
                readOnly: !!previewVersion,
                minimap: { enabled: true },
                lineNumbers: 'on',
                wordWrap: 'on',
                scrollBeyondLastLine: false,
                fontSize: 13,
                tabSize: 2,
                automaticLayout: true,
                suggestOnTriggerCharacters: true,
              }}
            />
          </div>

          {/* Inline console panel */}
          {showConsole && (
            <div className="se-console-panel">
              <div className="se-console-header">
                <span className="se-console-title">Console Output</span>
                <button
                  type="button"
                  className="se-console-close"
                  onClick={() => setShowConsole(false)}
                >
                  &times;
                </button>
              </div>
              <pre className="se-console-output">{consoleOutput || 'No output'}</pre>
            </div>
          )}
        </div>

        {/* Right sidebar */}
        <aside className="se-sidebar">
          {/* Version History section */}
          <div className="se-sidebar-section">
            <h3 className="se-sidebar-heading">Version History</h3>
            {versions.length === 0 ? (
              <p className="se-sidebar-empty">No versions yet</p>
            ) : (
              <ul className="se-version-list">
                {versions.map((v) => (
                  <li key={v.id} className="se-version-item">
                    <button
                      type="button"
                      className={`se-version-btn ${previewVersion?.id === v.id ? 'se-version-btn--active' : ''}`}
                      onClick={() => handleVersionClick(v)}
                    >
                      <span className="se-version-number">
                        v{v.version}
                        {v.version === script.version && (
                          <span className="se-version-current"> (current)</span>
                        )}
                      </span>
                      <span className="se-version-date">
                        {new Date(v.createdAt).toLocaleDateString()}
                      </span>
                      {v.changeSummary && (
                        <span className="se-version-summary">{v.changeSummary}</span>
                      )}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <hr className="se-sidebar-divider" />

          {/* Usage section */}
          <div className="se-sidebar-section">
            <h3 className="se-sidebar-heading">Usage</h3>
            {!usage ? (
              <p className="se-sidebar-empty">Loading...</p>
            ) : (
              <div className="se-usage-list">
                {usage.listeners.length > 0 && (
                  <div className="se-usage-group">
                    <span className="se-usage-label">Listeners</span>
                    <span className="se-usage-count">{usage.listeners.length}</span>
                  </div>
                )}
                {usage.fieldBehaviors.length > 0 && (
                  <div className="se-usage-group">
                    <span className="se-usage-label">Field Behaviors</span>
                    <span className="se-usage-count">{usage.fieldBehaviors.length}</span>
                  </div>
                )}
                {usage.calculatedFields.length > 0 && (
                  <div className="se-usage-group">
                    <span className="se-usage-label">Calculated Fields</span>
                    <span className="se-usage-count">{usage.calculatedFields.length}</span>
                  </div>
                )}
                {usage.includedBy.length > 0 && (
                  <div className="se-usage-group">
                    <span className="se-usage-label">Included By</span>
                    {usage.includedBy.map((key) => (
                      <code key={key} className="se-usage-ref">{key}</code>
                    ))}
                  </div>
                )}
                {usage.listeners.length === 0 &&
                  usage.fieldBehaviors.length === 0 &&
                  usage.calculatedFields.length === 0 &&
                  usage.includedBy.length === 0 && (
                    <p className="se-sidebar-empty">Not referenced anywhere</p>
                  )}
              </div>
            )}
          </div>
        </aside>
      </div>

      {/* Save dialog */}
      {showSaveDialog && (
        <div className="se-save-overlay" onClick={() => setShowSaveDialog(false)}>
          <div className="se-save-dialog" onClick={(e) => e.stopPropagation()}>
            <h3 className="se-save-dialog-title">Save Script</h3>
            <div className="se-save-dialog-body">
              <label className="se-save-label" htmlFor="changeSummary">
                Change summary (optional)
              </label>
              <input
                id="changeSummary"
                type="text"
                className="se-save-input"
                value={changeSummary}
                onChange={(e) => setChangeSummary(e.target.value)}
                placeholder="What changed in this version?"
                autoFocus
                onKeyDown={(e) => {
                  if (e.key === 'Enter') confirmSave();
                }}
              />
            </div>
            <div className="se-save-dialog-footer">
              <button
                type="button"
                className="ab-btn ab-btn-secondary"
                onClick={() => setShowSaveDialog(false)}
              >
                Cancel
              </button>
              <button
                type="button"
                className="ab-btn ab-btn-primary"
                onClick={confirmSave}
                disabled={saveMutation.isPending}
              >
                {saveMutation.isPending ? 'Saving...' : 'Save'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
