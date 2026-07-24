import { useState } from 'react';
import Editor from '@monaco-editor/react';
import { scriptApi, ScriptConsoleResponse } from '../../../api/scriptApi';

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
  const [scriptBody, setScriptBody] = useState(initialScript || '// JDC Script — Available APIs:\n// jdc.issue: getCurrentIssue(), getFieldValue(), setFieldValue(), getComments(), getHistory(), addComment()\n// jdc.project: getCurrentProject(), getProjectByKey(), getVersions(), getComponents()\n// jdc.user: getCurrentUser(), getUser(), isInGroup(), hasPermission(), getUserGroups()\n// jdc.search: jql(query, maxResults), findIssues(projectKey, statusName)\n// jdc.workflow: getCurrentTransition(), getAllStatuses()\n// jdc.log: info(), warn(), error(), debug()\n// http: get(url, headers), post(url, body, headers) — whitelisted domains only\n// console: log(), info(), warn(), error(), debug()\n\nvar priority = jdc.issue.getFieldValue("priority");\npriority === "Critical";\n');
  const [contextJson, setContextJson] = useState(DEFAULT_CONTEXT);
  const [scriptType, setScriptType] = useState(initialType);
  const [result, setResult] = useState<ScriptConsoleResponse | null>(null);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
