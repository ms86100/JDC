import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';

interface Plugin {
  pluginId: string;
  name: string;
  version: string;
  description?: string;
  author?: string;
  vendor?: string;
  status: string;
  enabled: boolean;
  installedAt: string;
  updatedAt?: string;
}

interface MarketplacePlugin {
  pluginId: string;
  name: string;
  version: string;
  description: string;
  author: string;
  vendor: string;
  permissions: string[];
  compatible: boolean;
}

interface PluginStats {
  totalPlugins: number;
  enabledPlugins: number;
  errorPlugins: number;
  totalHookExecutions: number;
  totalHookFailures: number;
  successRate: number;
  applicationReady: boolean;
  lastStatusCheck: string;
  hookTypeStats: { hookType: string; totalPlugins: number; enabledPlugins: number }[];
}

interface HookTestResult {
  pluginId: string;
  hookType: string;
  success: boolean;
  message: string;
  data?: Record<string, unknown>;
  executionTimeMs: number;
}

const API_BASE = '/api/plugins';

export const PluginManagementPage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const [activeTab, setActiveTab] = useState<'installed' | 'marketplace' | 'stats'>('installed');
  const [plugins, setPlugins] = useState<Plugin[]>([]);
  const [marketplacePlugins, setMarketplacePlugins] = useState<MarketplacePlugin[]>([]);
  const [stats, setStats] = useState<PluginStats | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedPlugin, setSelectedPlugin] = useState<Plugin | null>(null);
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [showTestModal, setShowTestModal] = useState(false);
  const [testResult, setTestResult] = useState<HookTestResult | null>(null);
  const [selectedHookType, setSelectedHookType] = useState<string>('');

  const fetchPlugins = useCallback(async () => {
    if (!projectId) return;
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE}?projectId=${projectId}`);
      if (response.ok) {
        const data = await response.json();
        setPlugins(data.plugins || []);
      }
    } catch (err) {
      setError('Failed to load plugins');
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  const fetchMarketplace = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE}/available`);
      if (response.ok) {
        const data = await response.json();
        setMarketplacePlugins(data.plugins || []);
      }
    } catch (err) {
      setError('Failed to load marketplace');
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchStats = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE}/stats`);
      if (response.ok) {
        const data = await response.json();
        setStats(data);
      }
    } catch (err) {
      setError('Failed to load statistics');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchPlugins();
  }, [fetchPlugins]);

  useEffect(() => {
    if (activeTab === 'marketplace') {
      fetchMarketplace();
    } else if (activeTab === 'stats') {
      fetchStats();
    }
  }, [activeTab, fetchMarketplace, fetchStats]);

  const handleEnablePlugin = async (pluginId: string) => {
    try {
      const response = await fetch(`${API_BASE}/${pluginId}/enable?projectId=${projectId}`, {
        method: 'PUT',
      });
      if (response.ok) {
        fetchPlugins();
      } else {
        const data = await response.json();
        setError(data.message || 'Failed to enable plugin');
      }
    } catch (err) {
      setError('Failed to enable plugin');
    }
  };

  const handleDisablePlugin = async (pluginId: string) => {
    try {
      const response = await fetch(`${API_BASE}/${pluginId}/disable?projectId=${projectId}`, {
        method: 'PUT',
      });
      if (response.ok) {
        fetchPlugins();
      } else {
        const data = await response.json();
        setError(data.message || 'Failed to disable plugin');
      }
    } catch (err) {
      setError('Failed to disable plugin');
    }
  };

  const handleUninstallPlugin = async (pluginId: string) => {
    if (!confirm('Are you sure you want to uninstall this plugin?')) return;
    try {
      const response = await fetch(`${API_BASE}/${pluginId}?projectId=${projectId}`, {
        method: 'DELETE',
      });
      if (response.ok) {
        fetchPlugins();
        setSelectedPlugin(null);
      } else {
        setError('Failed to uninstall plugin');
      }
    } catch (err) {
      setError('Failed to uninstall plugin');
    }
  };

  const handleUploadPlugin = async (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('projectId', projectId!);

    try {
      const response = await fetch(`${API_BASE}/upload`, {
        method: 'POST',
        body: formData,
      });
      const data = await response.json();
      if (data.success) {
        setShowUploadModal(false);
        fetchPlugins();
      } else {
        setError(data.message || 'Upload failed');
      }
    } catch (err) {
      setError('Failed to upload plugin');
    }
  };

  const handleInstallFromMarketplace = async (plugin: MarketplacePlugin) => {
    try {
      const response = await fetch(
        `${API_BASE}/${plugin.pluginId}/install?projectId=${projectId}&version=${plugin.version}`,
        { method: 'POST' }
      );
      const data = await response.json();
      if (data.success) {
        fetchPlugins();
        setActiveTab('installed');
      } else {
        setError(data.message || 'Installation failed');
      }
    } catch (err) {
      setError('Failed to install plugin');
    }
  };

  const handleTestHook = async (pluginId: string, hookType?: string) => {
    try {
      const url = hookType
        ? `${API_BASE}/${pluginId}/hooks/test?hookType=${hookType}&projectId=${projectId}`
        : `${API_BASE}/${pluginId}/hooks/test?projectId=${projectId}`;
      const response = await fetch(url);
      if (response.ok) {
        const result = await response.json();
        setTestResult(result);
        setShowTestModal(true);
      } else {
        setError('Failed to test hook');
      }
    } catch (err) {
      setError('Failed to test hook');
    }
  };

  const getStatusBadge = (status: string, enabled: boolean) => {
    const baseClasses = 'px-2 py-1 rounded-full text-xs font-medium';
    if (enabled) {
      return <span className={`${baseClasses} bg-green-100 text-green-800`}>Enabled</span>;
    }
    switch (status) {
      case 'ERROR':
        return <span className={`${baseClasses} bg-red-100 text-red-800`}>Error</span>;
      case 'PENDING':
        return <span className={`${baseClasses} bg-yellow-100 text-yellow-800`}>Pending</span>;
      default:
        return <span className={`${baseClasses} bg-gray-100 text-gray-800`}>Disabled</span>;
    }
  };

  return (
    <div className="plugin-management-page p-6">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Plugin Management</h1>
          {projectId && (
            <p className="text-gray-500 text-sm mt-1">
              Project: {projectId.slice(0, 8)}...
            </p>
          )}
        </div>
        <div className="flex gap-3">
          <button onClick={() => setShowUploadModal(true)} className="btn btn-primary">
            + Upload Plugin
          </button>
        </div>
      </div>

      {/* Quick Stats */}
      {stats && (
        <div className="stats-bar grid grid-cols-5 gap-4 mb-6">
          <div className="card border rounded-lg p-4">
            <div className="text-sm text-gray-500">Total Plugins</div>
            <div className="text-2xl font-bold">{stats.totalPlugins}</div>
          </div>
          <div className="card border rounded-lg p-4">
            <div className="text-sm text-gray-500">Enabled</div>
            <div className="text-2xl font-bold text-green-600">{stats.enabledPlugins}</div>
          </div>
          <div className="card border rounded-lg p-4">
            <div className="text-sm text-gray-500">Errors</div>
            <div className="text-2xl font-bold text-red-600">{stats.errorPlugins}</div>
          </div>
          <div className="card border rounded-lg p-4">
            <div className="text-sm text-gray-500">Hook Executions</div>
            <div className="text-2xl font-bold">{stats.totalHookExecutions}</div>
          </div>
          <div className="card border rounded-lg p-4">
            <div className="text-sm text-gray-500">Success Rate</div>
            <div className="text-2xl font-bold text-blue-600">
              {(stats.successRate * 100).toFixed(1)}%
            </div>
          </div>
        </div>
      )}

      {/* Navigation Tabs */}
      <div className="tabs mb-6 flex gap-2 border-b">
        <button
          onClick={() => setActiveTab('installed')}
          className={`px-4 py-2 font-medium ${
            activeTab === 'installed'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Installed Plugins
        </button>
        <button
          onClick={() => setActiveTab('marketplace')}
          className={`px-4 py-2 font-medium ${
            activeTab === 'marketplace'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Marketplace
        </button>
        <button
          onClick={() => setActiveTab('stats')}
          className={`px-4 py-2 font-medium ${
            activeTab === 'stats'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Statistics
        </button>
      </div>

      {/* Error Display */}
      {error && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700 flex justify-between">
          <span>{error}</span>
          <button onClick={() => setError(null)} className="text-red-500 hover:text-red-700">
            x
          </button>
        </div>
      )}

      {/* Content */}
      <div className="content">
        {activeTab === 'installed' && (
          <div className="grid grid-cols-3 gap-6">
            {/* Plugin List */}
            <div className="col-span-2">
              <div className="card border rounded-lg">
                <div className="p-4 border-b">
                  <h2 className="font-semibold">Installed Plugins ({plugins.length})</h2>
                </div>
                {loading ? (
                  <div className="p-8 text-center text-gray-500">Loading...</div>
                ) : plugins.length === 0 ? (
                  <div className="p-8 text-center text-gray-500">
                    No plugins installed. Upload a plugin or browse the marketplace.
                  </div>
                ) : (
                  <div className="divide-y">
                    {plugins.map((plugin) => (
                      <div
                        key={plugin.pluginId}
                        className={`p-4 hover:bg-gray-50 cursor-pointer ${
                          selectedPlugin?.pluginId === plugin.pluginId ? 'bg-blue-50' : ''
                        }`}
                        onClick={() => setSelectedPlugin(plugin)}
                      >
                        <div className="flex justify-between items-start">
                          <div>
                            <h3 className="font-medium">{plugin.name}</h3>
                            <p className="text-sm text-gray-500">{plugin.pluginId}</p>
                            {plugin.description && (
                              <p className="text-sm text-gray-600 mt-1 line-clamp-1">
                                {plugin.description}
                              </p>
                            )}
                          </div>
                          <div className="flex items-center gap-2">
                            {getStatusBadge(plugin.status, plugin.enabled)}
                            <span className="text-xs text-gray-400">v{plugin.version}</span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* Plugin Details */}
            <div className="col-span-1">
              {selectedPlugin ? (
                <div className="card border rounded-lg p-4">
                  <h3 className="font-semibold mb-4">{selectedPlugin.name}</h3>
                  <div className="space-y-3 text-sm">
                    <div>
                      <span className="text-gray-500">Plugin ID:</span>
                      <p className="font-mono">{selectedPlugin.pluginId}</p>
                    </div>
                    <div>
                      <span className="text-gray-500">Version:</span>
                      <p>{selectedPlugin.version}</p>
                    </div>
                    {selectedPlugin.author && (
                      <div>
                        <span className="text-gray-500">Author:</span>
                        <p>{selectedPlugin.author}</p>
                      </div>
                    )}
                    {selectedPlugin.vendor && (
                      <div>
                        <span className="text-gray-500">Vendor:</span>
                        <p>{selectedPlugin.vendor}</p>
                      </div>
                    )}
                    <div>
                      <span className="text-gray-500">Installed:</span>
                      <p>{new Date(selectedPlugin.installedAt).toLocaleDateString()}</p>
                    </div>
                    {selectedPlugin.updatedAt && (
                      <div>
                        <span className="text-gray-500">Updated:</span>
                        <p>{new Date(selectedPlugin.updatedAt).toLocaleDateString()}</p>
                      </div>
                    )}
                  </div>

                  <div className="mt-6 flex flex-col gap-2">
                    {selectedPlugin.enabled ? (
                      <button
                        onClick={() => handleDisablePlugin(selectedPlugin.pluginId)}
                        className="btn btn-secondary w-full"
                      >
                        Disable Plugin
                      </button>
                    ) : (
                      <button
                        onClick={() => handleEnablePlugin(selectedPlugin.pluginId)}
                        className="btn btn-primary w-full"
                      >
                        Enable Plugin
                      </button>
                    )}
                    <button
                      onClick={() => handleTestHook(selectedPlugin.pluginId)}
                      className="btn btn-secondary w-full"
                    >
                      Test Hook
                    </button>
                    <button
                      onClick={() => handleUninstallPlugin(selectedPlugin.pluginId)}
                      className="btn btn-danger w-full"
                    >
                      Uninstall
                    </button>
                  </div>
                </div>
              ) : (
                <div className="card border rounded-lg p-8 text-center text-gray-500">
                  Select a plugin to view details
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'marketplace' && (
          <div className="grid grid-cols-2 gap-6">
            {loading ? (
              <div className="col-span-2 p-8 text-center text-gray-500">Loading marketplace...</div>
            ) : marketplacePlugins.length === 0 ? (
              <div className="col-span-2 p-8 text-center text-gray-500">
                No marketplace plugins available.
              </div>
            ) : (
              marketplacePlugins.map((plugin) => (
                <div key={plugin.pluginId} className="card border rounded-lg p-4">
                  <div className="flex justify-between items-start mb-3">
                    <div>
                      <h3 className="font-semibold">{plugin.name}</h3>
                      <p className="text-sm text-gray-500">by {plugin.author}</p>
                    </div>
                    <span className="text-xs bg-blue-100 text-blue-800 px-2 py-1 rounded">
                      v{plugin.version}
                    </span>
                  </div>
                  <p className="text-sm text-gray-600 mb-4">{plugin.description}</p>
                  <div className="mb-4">
                    <span className="text-xs text-gray-500">Permissions:</span>
                    <div className="flex flex-wrap gap-1 mt-1">
                      {plugin.permissions.map((perm) => (
                        <span
                          key={perm}
                          className="text-xs bg-gray-100 text-gray-700 px-2 py-0.5 rounded"
                        >
                          {perm}
                        </span>
                      ))}
                    </div>
                  </div>
                  <div className="flex justify-between items-center">
                    <span
                      className={`text-xs ${
                        plugin.compatible ? 'text-green-600' : 'text-red-600'
                      }`}
                    >
                      {plugin.compatible ? 'Compatible' : 'Not Compatible'}
                    </span>
                    <button
                      onClick={() => handleInstallFromMarketplace(plugin)}
                      disabled={!plugin.compatible}
                      className={`btn btn-primary btn-sm ${
                        !plugin.compatible ? 'opacity-50 cursor-not-allowed' : ''
                      }`}
                    >
                      Install
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        )}

        {activeTab === 'stats' && stats && (
          <div className="space-y-6">
            <div className="card border rounded-lg p-4">
              <h2 className="font-semibold mb-4">Hook Statistics</h2>
              <div className="grid grid-cols-4 gap-4">
                {stats.hookTypeStats.map((hook) => (
                  <div key={hook.hookType} className="border rounded-lg p-3">
                    <div className="text-sm text-gray-500">{hook.hookType}</div>
                    <div className="flex justify-between mt-2">
                      <span className="text-lg font-bold">{hook.totalPlugins}</span>
                      <span className="text-sm text-green-600">
                        {hook.enabledPlugins} active
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="grid grid-cols-2 gap-6">
              <div className="card border rounded-lg p-4">
                <h2 className="font-semibold mb-4">Execution Metrics</h2>
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-gray-500">Total Executions</span>
                    <span className="font-bold">{stats.totalHookExecutions}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">Failed Executions</span>
                    <span className="font-bold text-red-600">{stats.totalHookFailures}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">Success Rate</span>
                    <span className="font-bold text-green-600">
                      {(stats.successRate * 100).toFixed(2)}%
                    </span>
                  </div>
                </div>
              </div>

              <div className="card border rounded-lg p-4">
                <h2 className="font-semibold mb-4">System Status</h2>
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-gray-500">Application Ready</span>
                    <span
                      className={`font-bold ${
                        stats.applicationReady ? 'text-green-600' : 'text-red-600'
                      }`}
                    >
                      {stats.applicationReady ? 'Yes' : 'No'}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">Last Status Check</span>
                    <span className="font-mono text-sm">{stats.lastStatusCheck}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Upload Modal */}
      {showUploadModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-xl font-semibold mb-4">Upload Plugin</h2>
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Select Plugin JAR file
              </label>
              <input
                type="file"
                accept=".jar"
                id="plugin-file"
                className="w-full border rounded-lg p-2"
              />
            </div>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setShowUploadModal(false)}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  const input = document.getElementById('plugin-file') as HTMLInputElement;
                  if (input.files && input.files[0]) {
                    handleUploadPlugin(input.files[0]);
                  }
                }}
                className="btn btn-primary"
              >
                Upload
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Test Modal */}
      {showTestModal && testResult && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-lg">
            <h2 className="text-xl font-semibold mb-4">Hook Test Result</h2>
            <div className="space-y-4">
              <div className="flex justify-between">
                <span className="text-gray-500">Plugin:</span>
                <span className="font-mono">{testResult.pluginId}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Hook Type:</span>
                <span>{testResult.hookType}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Result:</span>
                <span className={testResult.success ? 'text-green-600' : 'text-red-600'}>
                  {testResult.success ? 'SUCCESS' : 'FAILED'}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500">Execution Time:</span>
                <span>{testResult.executionTimeMs}ms</span>
              </div>
              <div className="p-3 bg-gray-50 rounded">
                <span className="text-gray-500">Message:</span>
                <p className="mt-1">{testResult.message}</p>
              </div>
              {testResult.data && Object.keys(testResult.data).length > 0 && (
                <div className="p-3 bg-gray-50 rounded">
                  <span className="text-gray-500">Data:</span>
                  <pre className="mt-1 text-sm font-mono overflow-x-auto">
                    {JSON.stringify(testResult.data, null, 2)}
                  </pre>
                </div>
              )}
            </div>
            <div className="mt-6 flex justify-end">
              <button
                onClick={() => {
                  setShowTestModal(false);
                  setTestResult(null);
                }}
                className="btn btn-primary"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PluginManagementPage;