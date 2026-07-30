import React, { useState } from 'react';
import type { JiraDcApiConfig, JiraDcConnectionTestResult } from '../types/migration';
import { migrationApi } from '../../../api/serviceApi';

interface JiraDcApiImportPanelProps {
  config: JiraDcApiConfig;
  onConfigChange: (config: JiraDcApiConfig) => void;
  connectionResult: JiraDcConnectionTestResult | null;
  onConnectionResult: (result: JiraDcConnectionTestResult | null) => void;
  disabled?: boolean;
}

export default function JiraDcApiImportPanel({
  config,
  onConfigChange,
  connectionResult,
  onConnectionResult,
  disabled = false,
}: JiraDcApiImportPanelProps) {
  const [testing, setTesting] = useState(false);
  const [projectKeysInput, setProjectKeysInput] = useState(config.projectKeys.join(', '));

  const handleTestConnection = async () => {
    setTesting(true);
    onConnectionResult(null);
    try {
      const result = await migrationApi.testJiraDcConnection({
        jiraBaseUrl: config.jiraBaseUrl,
        pat: config.pat,
        projectKeys: config.projectKeys,
        trustAllCertificates: config.trustAllCertificates,
      });
      onConnectionResult(result.data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Connection failed';
      onConnectionResult({
        connected: false,
        error: message,
      });
    } finally {
      setTesting(false);
    }
  };

  const updateConfig = (partial: Partial<JiraDcApiConfig>) => {
    onConfigChange({ ...config, ...partial });
    if ('jiraBaseUrl' in partial || 'pat' in partial || 'projectKeys' in partial) {
      onConnectionResult(null);
    }
  };

  const handleProjectKeysChange = (value: string) => {
    setProjectKeysInput(value);
    const keys = value
      .split(',')
      .map((k) => k.trim())
      .filter(Boolean);
    updateConfig({ projectKeys: keys });
  };

  const isConnectionReady = config.jiraBaseUrl.trim() !== '' && config.pat.trim() !== '';

  return (
    <div className="space-y-6">
      <div className="bg-cyan-50 border border-cyan-200 rounded-lg p-4 flex items-start gap-3">
        <span className="text-cyan-600 text-lg mt-0.5">🔗</span>
        <div>
          <p className="text-cyan-800 text-sm font-medium">Systems Data Center API Import</p>
          <p className="text-cyan-600 text-xs mt-1">
            Connect directly to your Systems Data Center instance and import issues via the REST API.
            Requires a Personal Access Token (PAT) with Browse Projects permission.
          </p>
        </div>
      </div>

      {/* Connection Settings */}
      <div className="space-y-4">
        <h4 className="text-sm font-semibold text-gray-700 uppercase tracking-wider">
          Connection Settings
        </h4>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Data Center Base URL <span className="text-red-500">*</span>
          </label>
          <input
            type="url"
            value={config.jiraBaseUrl}
            onChange={(e) => updateConfig({ jiraBaseUrl: e.target.value })}
            placeholder="https://jira.example.com"
            disabled={disabled}
            className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm
                       focus:ring-cyan-500 focus:border-cyan-500 disabled:bg-gray-100"
          />
          <p className="text-xs text-gray-500 mt-1">
            The base URL of your Systems Data Center instance (no trailing slash)
          </p>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Personal Access Token (PAT) <span className="text-red-500">*</span>
          </label>
          <input
            type="password"
            value={config.pat}
            onChange={(e) => updateConfig({ pat: e.target.value })}
            placeholder="Enter your PAT"
            disabled={disabled}
            className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm
                       focus:ring-cyan-500 focus:border-cyan-500 disabled:bg-gray-100"
          />
          <p className="text-xs text-gray-500 mt-1">
            Generate a PAT from your profile &gt; Personal Access Tokens
          </p>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Project Keys
          </label>
          <input
            type="text"
            value={projectKeysInput}
            onChange={(e) => handleProjectKeysChange(e.target.value)}
            placeholder="e.g. PROJ, DEMO, ENG"
            disabled={disabled}
            className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm
                       focus:ring-cyan-500 focus:border-cyan-500 disabled:bg-gray-100"
          />
          <p className="text-xs text-gray-500 mt-1">
            Comma-separated project keys to import. Leave empty to import from all accessible projects.
          </p>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            JQL Filter (optional)
          </label>
          <input
            type="text"
            value={config.jqlFilter || ''}
            onChange={(e) => updateConfig({ jqlFilter: e.target.value || undefined })}
            placeholder="e.g. status != Done AND created >= -30d"
            disabled={disabled}
            className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm text-sm
                       focus:ring-cyan-500 focus:border-cyan-500 disabled:bg-gray-100"
          />
          <p className="text-xs text-gray-500 mt-1">
            Additional JQL clause to filter which issues to import
          </p>
        </div>

        {/* Test Connection Button */}
        <div className="flex items-center gap-3">
          <button
            onClick={handleTestConnection}
            disabled={disabled || testing || !isConnectionReady}
            className="px-4 py-2 bg-cyan-600 text-white text-sm font-medium rounded-md
                       hover:bg-cyan-700 disabled:bg-gray-300 disabled:cursor-not-allowed
                       transition-colors flex items-center gap-2"
          >
            {testing ? (
              <>
                <span className="animate-spin inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full" />
                Testing...
              </>
            ) : (
              'Test Connection'
            )}
          </button>

          {connectionResult && (
            <div
              className={`flex items-center gap-2 text-sm ${
                connectionResult.connected ? 'text-green-600' : 'text-red-600'
              }`}
            >
              <span>{connectionResult.connected ? '✓' : '✗'}</span>
              {connectionResult.connected ? (
                <span>
                  Connected to DC {connectionResult.jiraVersion} as {connectionResult.userName}
                  {connectionResult.issueCount !== undefined && (
                    <> &middot; {connectionResult.issueCount.toLocaleString()} issues found</>
                  )}
                </span>
              ) : (
                <span>{connectionResult.error || 'Connection failed'}</span>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Import Options */}
      <div className="space-y-3">
        <h4 className="text-sm font-semibold text-gray-700 uppercase tracking-wider">
          Import Options
        </h4>

        <div className="grid grid-cols-2 gap-4">
          <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
            <input
              type="checkbox"
              checked={config.includeComments}
              onChange={(e) => updateConfig({ includeComments: e.target.checked })}
              disabled={disabled}
              className="rounded border-gray-300 text-cyan-600 focus:ring-cyan-500"
            />
            Include comments
          </label>

          <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
            <input
              type="checkbox"
              checked={config.includeAttachments}
              onChange={(e) => updateConfig({ includeAttachments: e.target.checked })}
              disabled={disabled}
              className="rounded border-gray-300 text-cyan-600 focus:ring-cyan-500"
            />
            Include attachments
          </label>

          <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
            <input
              type="checkbox"
              checked={config.includeWorklogs}
              onChange={(e) => updateConfig({ includeWorklogs: e.target.checked })}
              disabled={disabled}
              className="rounded border-gray-300 text-cyan-600 focus:ring-cyan-500"
            />
            Include worklogs
          </label>

          <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
            <input
              type="checkbox"
              checked={config.includeChangelog}
              onChange={(e) => updateConfig({ includeChangelog: e.target.checked })}
              disabled={disabled}
              className="rounded border-gray-300 text-cyan-600 focus:ring-cyan-500"
            />
            Include change history
          </label>
        </div>
      </div>

      {/* Connection Summary */}
      {connectionResult?.connected && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4 space-y-2">
          <h4 className="text-sm font-semibold text-green-800">Connection Verified</h4>
          <div className="grid grid-cols-2 gap-2 text-xs text-green-700">
            <div>
              <span className="font-medium">DC Version:</span> {connectionResult.jiraVersion}
            </div>
            <div>
              <span className="font-medium">User:</span> {connectionResult.userName}
            </div>
            {connectionResult.projectCount !== undefined && (
              <div>
                <span className="font-medium">Accessible Projects:</span>{' '}
                {connectionResult.projectCount}
              </div>
            )}
            {connectionResult.issueCount !== undefined && (
              <div>
                <span className="font-medium">Issues to Import:</span>{' '}
                {connectionResult.issueCount.toLocaleString()}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
