import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';
import {
  GitBranch, GitCommit, History, TrendingUp, TrendingDown, AlertTriangle,
  ChevronDown, ChevronRight, Plus, Search, Filter, Clock, ArrowRight,
  Eye, EyeOff, RefreshCw, FileText, Link2, Unlink, Calendar, Download
} from 'lucide-react';

// Types
interface RequirementVersion {
  id: string;
  requirementKey: string;
  versionNumber: number;
  name: string;
  description?: string;
  content?: string;
  changeSummary?: string;
  isPublished: boolean;
  isCurrent: boolean;
  createdBy?: string;
  createdAt: string;
  publishedAt?: string;
}

interface VersionDiff {
  versionId: string;
  previousVersionId?: string;
  changes: {
    field: string;
    oldValue?: string;
    newValue?: string;
    changeType: 'ADDED' | 'MODIFIED' | 'REMOVED';
  }[];
}

interface DriftRecord {
  requirementKey: string;
  previousVersion: number;
  currentVersion: number;
  driftType: 'CONTENT' | 'STATUS' | 'COVERAGE';
  driftPercent: number;
  affectedTests: string[];
  detectedAt: string;
}

interface RequirementChangeEvent {
  id: string;
  requirementKey: string;
  versionId: string;
  eventType: 'CREATED' | 'UPDATED' | 'PUBLISHED' | 'ARCHIVED';
  changedBy?: string;
  changedAt: string;
  details?: string;
}

// Stats Card
interface StatsCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  color?: string;
}

const StatsCard: React.FC<StatsCardProps> = ({ title, value, subtitle, icon, color = 'blue' }) => {
  const colorClasses: Record<string, { bg: string; icon: string }> = {
    blue: { bg: 'bg-blue-50', icon: 'text-blue-500' },
    green: { bg: 'bg-green-50', icon: 'text-green-500' },
    red: { bg: 'bg-red-50', icon: 'text-red-500' },
    yellow: { bg: 'bg-yellow-50', icon: 'text-yellow-500' },
    purple: { bg: 'bg-purple-50', icon: 'text-purple-500' },
  };

  const colors = colorClasses[color] || colorClasses.blue;

  return (
    <div className="bg-white rounded-lg border p-4">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-gray-500">{title}</p>
          <p className="text-2xl font-bold mt-1">{value}</p>
          {subtitle && <p className="text-xs text-gray-400 mt-1">{subtitle}</p>}
        </div>
        <div className={`${colors.bg} p-3 rounded-lg`}>
          <div className={colors.icon}>{icon}</div>
        </div>
      </div>
    </div>
  );
};

// Change Type Badge
const ChangeTypeBadge: React.FC<{ type: string }> = ({ type }) => {
  const typeConfig: Record<string, { bg: string; text: string }> = {
    ADDED: { bg: 'bg-green-100', text: 'text-green-700' },
    MODIFIED: { bg: 'bg-yellow-100', text: 'text-yellow-700' },
    REMOVED: { bg: 'bg-red-100', text: 'text-red-700' },
  };

  const config = typeConfig[type] || typeConfig.MODIFIED;

  return (
    <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${config.bg} ${config.text}`}>
      {type}
    </span>
  );
};

// Event Type Badge
const EventTypeBadge: React.FC<{ type: string }> = ({ type }) => {
  const typeConfig: Record<string, { bg: string; text: string }> = {
    CREATED: { bg: 'bg-blue-100', text: 'text-blue-700' },
    UPDATED: { bg: 'bg-yellow-100', text: 'text-yellow-700' },
    PUBLISHED: { bg: 'bg-green-100', text: 'text-green-700' },
    ARCHIVED: { bg: 'bg-gray-100', text: 'text-gray-700' },
  };

  const config = typeConfig[type] || typeConfig.UPDATED;

  return (
    <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${config.bg} ${config.text}`}>
      {type}
    </span>
  );
};

// Version History Item
interface VersionItemProps {
  version: RequirementVersion;
  isSelected: boolean;
  isCurrent: boolean;
  onSelect: () => void;
}

const VersionItem: React.FC<VersionItemProps> = ({ version, isSelected, isCurrent, onSelect }) => {
  return (
    <div
      onClick={onSelect}
      className={`p-4 border-b cursor-pointer transition-colors ${
        isSelected ? 'bg-blue-50 border-l-4 border-l-blue-500' : 'hover:bg-gray-50'
      }`}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <GitCommit size={16} className="text-gray-400" />
          <span className="font-medium">v{version.versionNumber}</span>
          {isCurrent && (
            <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded">Current</span>
          )}
        </div>
        <span className="text-xs text-gray-500">
          {new Date(version.createdAt).toLocaleDateString()}
        </span>
      </div>
      {version.changeSummary && (
        <p className="text-sm text-gray-600 mt-2">{version.changeSummary}</p>
      )}
      <div className="flex items-center gap-4 mt-2">
        {version.createdBy && (
          <span className="text-xs text-gray-400">by {version.createdBy}</span>
        )}
        {version.isPublished && (
          <span className="text-xs text-green-600">Published</span>
        )}
      </div>
    </div>
  );
};

// Diff View Component
interface DiffViewProps {
  diff: VersionDiff;
}

const DiffView: React.FC<DiffViewProps> = ({ diff }) => {
  return (
    <div className="space-y-3">
      {diff.changes.map((change, index) => (
        <div key={index} className="border rounded-lg p-3">
          <div className="flex items-center gap-2 mb-2">
            <span className="font-medium text-sm">{change.field}</span>
            <ChangeTypeBadge type={change.changeType} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            {change.oldValue && (
              <div className="bg-red-50 p-2 rounded">
                <p className="text-xs text-red-600 mb-1">Removed</p>
                <p className="text-sm text-gray-700 font-mono">{change.oldValue}</p>
              </div>
            )}
            {change.newValue && (
              <div className="bg-green-50 p-2 rounded">
                <p className="text-xs text-green-600 mb-1">Added</p>
                <p className="text-sm text-gray-700 font-mono">{change.newValue}</p>
              </div>
            )}
          </div>
        </div>
      ))}
    </div>
  );
};

// Create Version Modal
interface CreateVersionModalProps {
  open: boolean;
  requirementKey: string;
  currentVersion: number;
  onClose: () => void;
  onSuccess: () => void;
}

const CreateVersionModal: React.FC<CreateVersionModalProps> = ({
  open,
  requirementKey,
  currentVersion,
  onClose,
  onSuccess,
}) => {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    changeSummary: '',
    content: '',
  });

  const createMutation = useMutation({
    mutationFn: async (data: typeof formData) => {
      const response = await apiClient.post(
        `/api/requirements/${requirementKey}/versions`,
        { ...data, versionNumber: currentVersion + 1 }
      );
      return response.data;
    },
    onSuccess: () => {
      onSuccess();
      onClose();
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate(formData);
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onClose}></div>
        <div className="relative bg-white rounded-lg shadow-xl max-w-lg w-full p-6">
          <h3 className="text-lg font-semibold mb-4">Create New Version</h3>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Version Name *
              </label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="w-full px-3 py-2 border rounded-lg"
                placeholder={`Version ${currentVersion + 1}`}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <textarea
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                className="w-full px-3 py-2 border rounded-lg"
                rows={2}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Change Summary *
              </label>
              <textarea
                value={formData.changeSummary}
                onChange={(e) => setFormData({ ...formData, changeSummary: e.target.value })}
                className="w-full px-3 py-2 border rounded-lg"
                rows={3}
                placeholder="Describe what changed in this version"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Content
              </label>
              <textarea
                value={formData.content}
                onChange={(e) => setFormData({ ...formData, content: e.target.value })}
                className="w-full px-3 py-2 border rounded-lg font-mono text-sm"
                rows={5}
                placeholder="Requirement content or specification"
              />
            </div>
            <div className="flex justify-end gap-3 pt-4">
              <button type="button" onClick={onClose} className="btn btn-secondary">
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={createMutation.isPending}>
                {createMutation.isPending ? 'Creating...' : 'Create Version'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

// Main Component
const RequirementVersionPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [selectedRequirement, setSelectedRequirement] = useState<string | null>(null);
  const [selectedVersion, setSelectedVersion] = useState<RequirementVersion | null>(null);
  const [compareVersion, setCompareVersion] = useState<RequirementVersion | null>(null);
  const [showDiff, setShowDiff] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState<'ALL' | 'PUBLISHED' | 'DRAFT'>('ALL');
  const [showCreateVersion, setShowCreateVersion] = useState(false);
  const [showChangeHistory, setShowChangeHistory] = useState(false);

  // Fetch requirements with versions
  const { data: requirements = [], isLoading } = useQuery({
    queryKey: ['requirement-versions'],
    queryFn: async () => {
      const response = await apiClient.get('/requirements');
      return response.data;
    },
  });

  // Fetch version history for selected requirement
  const { data: versionHistory = [], isLoading: historyLoading } = useQuery({
    queryKey: ['version-history', selectedRequirement],
    queryFn: async () => {
      if (!selectedRequirement) return [];
      const response = await apiClient.get(`/api/requirements/${selectedRequirement}/versions`);
      return response.data as RequirementVersion[];
    },
    enabled: !!selectedRequirement,
  });

  // Fetch version diff
  const { data: versionDiff } = useQuery({
    queryKey: ['version-diff', selectedVersion?.id, compareVersion?.id],
    queryFn: async () => {
      if (!selectedVersion) return null;
      const params = compareVersion ? { compareVersionId: compareVersion.id } : {};
      const response = await apiClient.get(`/api/requirements/${selectedRequirement}/versions/${selectedVersion.id}/diff`, { params });
      return response.data as VersionDiff;
    },
    enabled: !!selectedVersion && showDiff,
  });

  // Fetch drift analysis
  const { data: driftRecords = [] } = useQuery({
    queryKey: ['drift-records', selectedRequirement],
    queryFn: async () => {
      if (!selectedRequirement) return [];
      const response = await apiClient.get(`/api/requirements/${selectedRequirement}/drift`);
      return response.data as DriftRecord[];
    },
    enabled: !!selectedRequirement,
  });

  // Fetch change events
  const { data: changeEvents = [] } = useQuery({
    queryKey: ['change-events', selectedRequirement],
    queryFn: async () => {
      if (!selectedRequirement) return [];
      const response = await apiClient.get(`/api/requirements/${selectedRequirement}/events`);
      return response.data as RequirementChangeEvent[];
    },
    enabled: !!selectedRequirement && showChangeHistory,
  });

  // Publish version mutation
  const publishMutation = useMutation({
    mutationFn: async (versionId: string) => {
      const response = await apiClient.put(`/api/requirements/${selectedRequirement}/versions/${versionId}/publish`);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['version-history'] });
    },
  });

  // Trigger drift analysis mutation
  const analyzeDriftMutation = useMutation({
    mutationFn: async () => {
      const response = await apiClient.post(`/api/requirements/${selectedRequirement}/coverage-drift`);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['drift-records'] });
    },
  });

  // Filter requirements
  const filteredRequirements = (requirements as any[]).filter((req) => {
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      if (!req.key?.toLowerCase().includes(query) && !req.name?.toLowerCase().includes(query)) {
        return false;
      }
    }
    return true;
  });

  // Stats
  const stats = {
    totalVersions: versionHistory.length,
    publishedVersions: versionHistory.filter((v) => v.isPublished).length,
    currentVersion: versionHistory.find((v) => v.isCurrent)?.versionNumber || 0,
    driftCount: driftRecords.length,
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Requirement Version & Drift</h1>
            <p className="text-sm text-gray-500 mt-1">
              Track requirement changes and detect coverage drift
            </p>
          </div>
          <div className="flex gap-2">
            {selectedRequirement && (
              <>
                <button
                  onClick={() => analyzeDriftMutation.mutate()}
                  className="btn btn-secondary"
                  disabled={analyzeDriftMutation.isPending}
                >
                  <RefreshCw size={16} className="mr-1" />
                  Analyze Drift
                </button>
                <button
                  onClick={() => setShowCreateVersion(true)}
                  className="btn btn-primary"
                >
                  <Plus size={16} className="mr-1" />
                  New Version
                </button>
              </>
            )}
          </div>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <StatsCard
            title="Total Versions"
            value={stats.totalVersions}
            subtitle="across requirements"
            icon={<History size={20} />}
            color="blue"
          />
          <StatsCard
            title="Published"
            value={stats.publishedVersions}
            subtitle="versions available"
            icon={<GitBranch size={20} />}
            color="green"
          />
          <StatsCard
            title="Current Version"
            value={`v${stats.currentVersion}`}
            subtitle="selected requirement"
            icon={<FileText size={20} />}
            color="purple"
          />
          <StatsCard
            title="Drift Detected"
            value={stats.driftCount}
            subtitle="coverage changes"
            icon={<AlertTriangle size={20} />}
            color={stats.driftCount > 0 ? 'red' : 'green'}
          />
        </div>

        {/* Main Content */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Requirements List */}
          <div className="lg:col-span-1 bg-white rounded-lg border">
            <div className="p-4 border-b">
              <h2 className="font-semibold text-gray-900">Requirements</h2>
              <div className="mt-3">
                <div className="relative">
                  <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                  <input
                    type="text"
                    placeholder="Search requirements..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full pl-9 pr-3 py-2 border rounded-lg text-sm"
                  />
                </div>
              </div>
            </div>
            <div className="max-h-[500px] overflow-y-auto">
              {isLoading ? (
                <div className="p-4 text-center text-gray-500">Loading...</div>
              ) : filteredRequirements.length === 0 ? (
                <div className="p-4 text-center text-gray-500">No requirements found</div>
              ) : (
                filteredRequirements.map((req: any) => (
                  <div
                    key={req.key}
                    onClick={() => {
                      setSelectedRequirement(req.key);
                      setSelectedVersion(null);
                      setCompareVersion(null);
                    }}
                    className={`p-4 border-b cursor-pointer transition-colors ${
                      selectedRequirement === req.key ? 'bg-blue-50' : 'hover:bg-gray-50'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-medium">{req.key}</span>
                      <GitBranch size={14} className="text-gray-400" />
                    </div>
                    {req.name && (
                      <p className="text-sm text-gray-500 mt-1">{req.name}</p>
                    )}
                    <div className="flex items-center gap-2 mt-2">
                      <span className="text-xs text-gray-400">
                        {req.versionCount || 0} versions
                      </span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Version History */}
          <div className="lg:col-span-2 space-y-4">
            {selectedRequirement ? (
              <>
                {/* Version List */}
                <div className="bg-white rounded-lg border">
                  <div className="p-4 border-b flex items-center justify-between">
                    <div>
                      <h2 className="font-semibold">Version History</h2>
                      <p className="text-sm text-gray-500">{selectedRequirement}</p>
                    </div>
                    <div className="flex gap-2">
                      <button
                        onClick={() => setShowChangeHistory(!showChangeHistory)}
                        className={`btn btn-sm ${showChangeHistory ? 'btn-primary' : 'btn-secondary'}`}
                      >
                        <History size={14} className="mr-1" />
                        Change Events
                      </button>
                    </div>
                  </div>
                  <div className="max-h-[300px] overflow-y-auto">
                    {historyLoading ? (
                      <div className="p-4 text-center text-gray-500">Loading...</div>
                    ) : versionHistory.length === 0 ? (
                      <div className="p-4 text-center text-gray-500">
                        No versions available. Create the first version.
                      </div>
                    ) : (
                      versionHistory.map((version) => (
                        <VersionItem
                          key={version.id}
                          version={version}
                          isSelected={selectedVersion?.id === version.id}
                          isCurrent={version.isCurrent}
                          onSelect={() => setSelectedVersion(version)}
                        />
                      ))
                    )}
                  </div>
                </div>

                {/* Version Detail */}
                {selectedVersion && (
                  <div className="bg-white rounded-lg border">
                    <div className="p-4 border-b flex items-center justify-between">
                      <div>
                        <h3 className="font-semibold">Version {selectedVersion.versionNumber} Details</h3>
                        {selectedVersion.changeSummary && (
                          <p className="text-sm text-gray-500 mt-1">{selectedVersion.changeSummary}</p>
                        )}
                      </div>
                      <div className="flex gap-2">
                        <select
                          value={compareVersion?.id || ''}
                          onChange={(e) => {
                            const v = versionHistory.find((v) => v.id === e.target.value);
                            setCompareVersion(v || null);
                          }}
                          className="px-3 py-1 border rounded text-sm"
                        >
                          <option value="">Compare with...</option>
                          {versionHistory
                            .filter((v) => v.id !== selectedVersion.id)
                            .map((v) => (
                              <option key={v.id} value={v.id}>
                                v{v.versionNumber} ({(v as any).changeSummary?.slice(0, 20) || 'No summary'}...)
                              </option>
                            ))}
                        </select>
                        <button
                          onClick={() => setShowDiff(!showDiff)}
                          className={`btn btn-sm ${showDiff ? 'btn-primary' : 'btn-secondary'}`}
                          disabled={!compareVersion}
                        >
                          Compare
                        </button>
                        {!selectedVersion.isPublished && (
                          <button
                            onClick={() => publishMutation.mutate(selectedVersion.id)}
                            className="btn btn-sm btn-primary"
                            disabled={publishMutation.isPending}
                          >
                            {publishMutation.isPending ? 'Publishing...' : 'Publish'}
                          </button>
                        )}
                      </div>
                    </div>
                    <div className="p-4">
                      {showDiff && versionDiff ? (
                        <DiffView diff={versionDiff} />
                      ) : (
                        <div className="space-y-4">
                          <div className="grid grid-cols-2 gap-4">
                            <div>
                              <p className="text-sm font-medium text-gray-500">Name</p>
                              <p className="text-gray-900">{selectedVersion.name}</p>
                            </div>
                            <div>
                              <p className="text-sm font-medium text-gray-500">Status</p>
                              <p className="text-gray-900">
                                {selectedVersion.isCurrent ? 'Current Version' : selectedVersion.isPublished ? 'Published' : 'Draft'}
                              </p>
                            </div>
                          </div>
                          {selectedVersion.description && (
                            <div>
                              <p className="text-sm font-medium text-gray-500">Description</p>
                              <p className="text-gray-700">{selectedVersion.description}</p>
                            </div>
                          )}
                          {selectedVersion.content && (
                            <div>
                              <p className="text-sm font-medium text-gray-500">Content</p>
                              <pre className="bg-gray-50 p-3 rounded text-sm font-mono overflow-x-auto">
                                {selectedVersion.content}
                              </pre>
                            </div>
                          )}
                          <div className="grid grid-cols-2 gap-4">
                            <div>
                              <p className="text-sm font-medium text-gray-500">Created</p>
                              <p className="text-gray-700">{new Date(selectedVersion.createdAt).toLocaleString()}</p>
                            </div>
                            {selectedVersion.publishedAt && (
                              <div>
                                <p className="text-sm font-medium text-gray-500">Published</p>
                                <p className="text-gray-700">{new Date(selectedVersion.publishedAt).toLocaleString()}</p>
                              </div>
                            )}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {/* Drift Records */}
                {driftRecords.length > 0 && (
                  <div className="bg-white rounded-lg border">
                    <div className="p-4 border-b">
                      <h3 className="font-semibold">Coverage Drift Detected</h3>
                    </div>
                    <div className="divide-y">
                      {driftRecords.map((drift) => (
                        <div key={drift.requirementKey} className="p-4">
                          <div className="flex items-center justify-between">
                            <div>
                              <p className="font-medium text-sm">
                                {drift.driftType} Drift
                              </p>
                              <p className="text-xs text-gray-500">
                                Version {drift.previousVersion} → {drift.currentVersion}
                              </p>
                            </div>
                            <span className={`text-sm ${drift.driftPercent > 0 ? 'text-green-600' : 'text-red-600'}`}>
                              {drift.driftPercent > 0 ? '+' : ''}{drift.driftPercent.toFixed(1)}%
                            </span>
                          </div>
                          <p className="text-xs text-gray-400 mt-2">
                            {drift.affectedTests.length} tests affected
                          </p>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Change Events */}
                {showChangeHistory && changeEvents.length > 0 && (
                  <div className="bg-white rounded-lg border">
                    <div className="p-4 border-b">
                      <h3 className="font-semibold">Change Events</h3>
                    </div>
                    <div className="divide-y max-h-[300px] overflow-y-auto">
                      {changeEvents.map((event) => (
                        <div key={event.id} className="p-4">
                          <div className="flex items-center gap-2">
                            <EventTypeBadge type={event.eventType} />
                            <span className="text-sm font-medium">
                              {event.eventType === 'CREATED' ? 'Created' :
                               event.eventType === 'UPDATED' ? 'Updated' :
                               event.eventType === 'PUBLISHED' ? 'Published' : 'Archived'}
                            </span>
                          </div>
                          {event.details && (
                            <p className="text-sm text-gray-600 mt-1">{event.details}</p>
                          )}
                          <div className="flex items-center gap-2 mt-2 text-xs text-gray-400">
                            <span>{new Date(event.changedAt).toLocaleString()}</span>
                            {event.changedBy && <span>by {event.changedBy}</span>}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </>
            ) : (
              <div className="bg-white rounded-lg border p-8 text-center">
                <GitBranch size={48} className="mx-auto text-gray-300 mb-4" />
                <h3 className="text-lg font-semibold text-gray-900">No Requirement Selected</h3>
                <p className="text-gray-500 mt-2">
                  Select a requirement from the list to view its version history
                </p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Create Version Modal */}
      {selectedRequirement && versionHistory.length > 0 && (
        <CreateVersionModal
          open={showCreateVersion}
          requirementKey={selectedRequirement}
          currentVersion={versionHistory[0]?.versionNumber || 0}
          onClose={() => setShowCreateVersion(false)}
          onSuccess={() => {
            queryClient.invalidateQueries({ queryKey: ['version-history'] });
          }}
        />
      )}
    </div>
  );
};

export default RequirementVersionPage;