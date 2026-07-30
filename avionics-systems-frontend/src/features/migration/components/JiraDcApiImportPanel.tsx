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

  const handleTestConnection = async () => {
    setTesting(true);
    onConnectionResult(null);
    try {
      const result = await migrationApi.testJiraDcConnection({
        jiraBaseUrl: config.jiraBaseUrl,
        pat: config.pat,
        trustAllCertificates: config.trustAllCertificates,
      });
      onConnectionResult(result.data);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Connection failed';
      onConnectionResult({ connected: false, error: message });
    } finally {
      setTesting(false);
    }
  };

  const updateConfig = (partial: Partial<JiraDcApiConfig>) => {
    onConfigChange({ ...config, ...partial });
    if ('jiraBaseUrl' in partial || 'pat' in partial) {
      onConnectionResult(null);
    }
  };

  const toggleProject = (key: string) => {
    const current = config.projectKeys;
    const next = current.includes(key)
      ? current.filter((k) => k !== key)
      : [...current, key];
    onConfigChange({ ...config, projectKeys: next });
  };

  const selectAllProjects = () => {
    const allKeys = (connectionResult?.projects ?? []).map((p) => p.key);
    onConfigChange({ ...config, projectKeys: allKeys });
  };

  const clearAllProjects = () => {
    onConfigChange({ ...config, projectKeys: [] });
  };

  const isConnectionReady = config.jiraBaseUrl.trim() !== '' && config.pat.trim() !== '';
  const availableProjects = connectionResult?.projects ?? [];

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
            placeholder="https://dc-instance.example.com"
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
            ) : connectionResult?.connected ? (
              'Re-test Connection'
            ) : (
              'Test Connection'
            )}
          </button>

          {connectionResult && !connectionResult.connected && (
            <div className="flex items-center gap-2 text-sm text-red-600">
              <span>✗</span>
              <span>{connectionResult.error || 'Connection failed'}</span>
            </div>
          )}
        </div>
      </div>

      {/* Connection Success + Project Selection */}
      {connectionResult?.connected && (
        <>
          <div className="bg-green-50 border border-green-200 rounded-lg p-4 space-y-2">
            <h4 className="text-sm font-semibold text-green-800">Connection Verified</h4>
            <div className="grid grid-cols-3 gap-2 text-xs text-green-700">
              <div>
                <span className="font-medium">DC Version:</span> {connectionResult.jiraVersion}
              </div>
              <div>
                <span className="font-medium">User:</span> {connectionResult.userName}
              </div>
              <div>
                <span className="font-medium">Total Issues:</span>{' '}
                {connectionResult.issueCount?.toLocaleString()}
              </div>
            </div>
          </div>

          {/* Project Selection */}
          {availableProjects.length > 0 && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <h4 className="text-sm font-semibold text-gray-700 uppercase tracking-wider">
                  Select Projects to Import
                </h4>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={selectAllProjects}
                    className="text-xs text-cyan-600 hover:text-cyan-800"
                  >
                    Select all
                  </button>
                  <span className="text-xs text-gray-300">|</span>
                  <button
                    type="button"
                    onClick={clearAllProjects}
                    className="text-xs text-gray-500 hover:text-gray-700"
                  >
                    Clear
                  </button>
                </div>
              </div>
              <div className="border border-gray-200 rounded-lg max-h-48 overflow-y-auto">
                {availableProjects.map((project) => (
                  <label
                    key={project.key}
                    className={`flex items-center gap-3 px-3 py-2 cursor-pointer transition-colors
                      border-b border-gray-100 last:border-b-0 hover:bg-gray-50
                      ${config.projectKeys.includes(project.key) ? 'bg-cyan-50' : ''}`}
                  >
                    <input
                      type="checkbox"
                      checked={config.projectKeys.includes(project.key)}
                      onChange={() => toggleProject(project.key)}
                      disabled={disabled}
                      className="rounded border-gray-300 text-cyan-600 focus:ring-cyan-500"
                    />
                    <span className="font-mono text-xs font-semibold text-gray-700 w-16">
                      {project.key}
                    </span>
                    <span className="text-sm text-gray-600 truncate">{project.name}</span>
                  </label>
                ))}
              </div>
              <p className="text-xs text-gray-500">
                {config.projectKeys.length === 0
                  ? 'No projects selected — all accessible projects will be imported'
                  : `${config.projectKeys.length} project${config.projectKeys.length > 1 ? 's' : ''} selected`}
              </p>
            </div>
          )}

          {/* JQL Filter */}
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
                  onChange={(e) => onConfigChange({ ...config, includeComments: e.target.checked })}
                  disabled={disabled}
                  className="rounded border-gray-300 text-cyan-600 focus:ring-cyan-500"
                />
                Include comments
              </label>
              <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                <input
                  type="checkbox"
                  checked={config.includeAttachments}
                  onChange={(e) => onConfigChange({ ...config, includeAttachments: e.target.checked })}
                  disabled={disabled}
                  className="rounded border-gray-300 text-cyan-600 focus:ring-cyan-500"
                />
                Include attachments
              </label>
              <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                <input
                  type="checkbox"
                  checked={config.includeWorklogs}
                  onChange={(e) => onConfigChange({ ...config, includeWorklogs: e.target.checked })}
                  disabled={disabled}
                  className="rounded border-gray-300 text-cyan-600 focus:ring-cyan-500"
                />
                Include worklogs
              </label>
              <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                <input
                  type="checkbox"
                  checked={config.includeChangelog}
                  onChange={(e) => onConfigChange({ ...config, includeChangelog: e.target.checked })}
                  disabled={disabled}
                  className="rounded border-gray-300 text-cyan-600 focus:ring-cyan-500"
                />
                Include change history
              </label>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
