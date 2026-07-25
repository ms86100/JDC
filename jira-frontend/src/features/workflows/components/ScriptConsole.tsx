import { useState, useRef, useEffect } from 'react';
import Editor, { useMonaco } from '@monaco-editor/react';
import { scriptApi, ScriptConsoleResponse } from '../../../api/scriptApi';
import { registerJdcCompletionProvider } from '../utils/jdcCompletionProvider';

interface ScriptConsoleProps {
  initialScript?: string;
  initialType?: string;
}

const DEFAULT_CONTEXT = JSON.stringify(
  {
    issueId: '00000000-0000-0000-0000-000000000001',
    projectId: '00000000-0000-0000-0000-000000000002',
    userId: '00000000-0000-0000-0000-000000000003',
    issueData: {
      summary: 'Test Issue',
      priority: 'High',
      statusName: 'Open',
      issueTypeName: 'Bug',
      assigneeId: null,
      reporterId: '00000000-0000-0000-0000-000000000003',
      description: 'This is a test issue for script console testing.',
      labels: [],
    },
    userData: {
      displayName: 'Test User',
      username: 'testuser',
      groups: ['jira-users', 'developers'],
    },
    screenInput: {},
  },
  null,
  2
);

export default function ScriptConsole({ initialScript = '', initialType = 'CONDITION' }: ScriptConsoleProps) {
  const [scriptBody, setScriptBody] = useState(initialScript || '// JDC Script Engine — Quick Start\n// 232 API methods across 22 bindings. Type jdc. to see autocomplete.\n//\n// Available APIs:\n//   jdc.issue (43 methods)  — CRUD, comments, labels, worklogs, attachments\n//   jdc.project (18)        — versions, components, roles, properties\n//   jdc.user (15)           — groups, permissions, create/deactivate\n//   jdc.search (3)          — JQL queries (max 500 results)\n//   jdc.workflow (6)        — transitions, statuses, schemes\n//   jdc.log (4)             — info, warn, error, debug\n//   http (5)                — GET/POST/PUT/DELETE/PATCH (whitelisted)\n//   sql (4)                 — query, update, batch, getDataSources\n//   email (5)               — send, sendToUser, CC/BCC, attachments\n//   vars (8)                — persistent key-value (global/project/issue)\n//   xml (5), ldap (6), confluence (8), sprint (10)\n//   asset (11)              — CMDB objects, link to issues\n//   tempo (8)               — time tracking, reports\n//   file (8)                — in-memory file I/O (10MB limit)\n//   test (11)               — assertTrue, assertEquals, assertNotNull\n//   webhook (4), include (3), env (1)\n//\n// DSL Syntax: assignee = "john" auto-transpiles to setFieldValue()\n// Go to Scripts tab — 5 pre-loaded demos are ready to run.\n\nconsole.log("=== Quick Demo ===");\nconsole.log("Issue: " + issueId);\nconsole.log("Project: " + projectId);\nconsole.log("User: " + (userData.displayName || userId));\nconsole.log("Issue Type: " + (issueData.issueTypeName || "N/A"));\nconsole.log("Priority: " + (issueData.priority || "N/A"));\n\n// Test the assertion framework\ntest.assertTrue(typeof jdc === "object", "JDC API loaded");\ntest.assertNotNull(issueId, "Issue context available");\nconsole.log("\\nTests: " + test.getPassed() + " passed");\n\n// Persistent variable demo\nvars.set("last-console-run", new Date().toISOString());\nconsole.log("Stored timestamp: " + vars.get("last-console-run"));\n\n({ status: "engine operational", bindings: 22, methods: 232 });\n');
  const [contextJson, setContextJson] = useState(DEFAULT_CONTEXT);
  const [scriptType, setScriptType] = useState(initialType);
  const [result, setResult] = useState<ScriptConsoleResponse | null>(null);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const monaco = useMonaco();
  const completionDisposable = useRef<{ dispose(): void } | null>(null);

  useEffect(() => {
    if (monaco && !completionDisposable.current) {
      completionDisposable.current = registerJdcCompletionProvider(monaco);
    }
    return () => {
      completionDisposable.current?.dispose();
      completionDisposable.current = null;
    };
  }, [monaco]);

  const handleRun = async () => {
    setRunning(true);
    setError(null);
    setResult(null);
    try {
      let parsedContext = {};
      try {
        parsedContext = JSON.parse(contextJson);
      } catch {
        setError('Invalid JSON in context editor');
        setRunning(false);
        return;
      }
      const response = await scriptApi.executeConsole({
        scriptBody,
        scriptType,
        context: parsedContext,
      });
      setResult(response.data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Execution failed';
      setError(msg);
    } finally {
      setRunning(false);
    }
  };

  return (
    <div className="border border-gray-200 rounded-lg overflow-hidden">
      <div className="flex items-center justify-between bg-gray-50 border-b border-gray-200 px-4 py-2">
        <div className="flex items-center gap-3">
          <h3 className="font-semibold text-sm">Script Console</h3>
          <select
            value={scriptType}
            onChange={(e) => setScriptType(e.target.value)}
            className="text-sm border border-gray-300 rounded px-2 py-1"
          >
            <option value="CONDITION">Condition (returns boolean)</option>
            <option value="VALIDATOR">Validator (returns error or null)</option>
            <option value="POST_FUNCTION">Post-Function (side effects)</option>
          </select>
        </div>
        <button
          onClick={handleRun}
          disabled={running}
          className="px-4 py-1.5 text-sm font-medium text-white bg-green-600 rounded hover:bg-green-700 disabled:opacity-50"
        >
          {running ? 'Running...' : 'Run'}
        </button>
      </div>

      <div className="grid grid-cols-2 divide-x divide-gray-200" style={{ height: '500px' }}>
        <div className="flex flex-col">
          <div className="bg-gray-100 px-3 py-1 text-xs font-medium text-gray-600 border-b border-gray-200">
            Script (JavaScript)
          </div>
          <div className="flex-1">
            <Editor
              height="100%"
              language="javascript"
              theme="vs-dark"
              value={scriptBody}
              onChange={(value) => setScriptBody(value || '')}
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

        <div className="flex flex-col">
          <div className="bg-gray-100 px-3 py-1 text-xs font-medium text-gray-600 border-b border-gray-200">
            Mock Context (JSON)
          </div>
          <div style={{ height: '280px' }}>
            <Editor
              height="100%"
              language="json"
              theme="vs-dark"
              value={contextJson}
              onChange={(value) => setContextJson(value || '{}')}
              options={{
                minimap: { enabled: false },
                lineNumbers: 'on',
                wordWrap: 'on',
                scrollBeyondLastLine: false,
                fontSize: 12,
                tabSize: 2,
              }}
            />
          </div>

          <div className="bg-gray-100 px-3 py-1 text-xs font-medium text-gray-600 border-b border-t border-gray-200">
            Result
          </div>
          <div className="flex-1 p-3 overflow-auto bg-gray-900 text-sm font-mono">
            {error && <div className="text-red-400">Error: {error}</div>}
            {result && (
              <div className="space-y-1">
                <div className={result.success ? 'text-green-400' : 'text-red-400'}>
                  Status: {result.success ? 'SUCCESS' : 'ERROR'}
                </div>
                <div className="text-gray-300">
                  Time: {result.executionMs}ms
                </div>
                {result.result !== null && result.result !== undefined && (
                  <div className="text-blue-300">
                    Return: {JSON.stringify(result.result)}
                  </div>
                )}
                {result.errorMessage && (
                  <div className="text-red-400">
                    Error: {result.errorMessage}
                  </div>
                )}
                {result.consoleOutput && (
                  <div className="mt-2 pt-2 border-t border-gray-700">
                    <div className="text-gray-400 text-xs mb-1">Console Output:</div>
                    <pre className="text-yellow-300 whitespace-pre-wrap text-xs">{result.consoleOutput}</pre>
                  </div>
                )}
              </div>
            )}
            {!error && !result && (
              <div className="text-gray-500 italic">Click "Run" to execute the script...</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
