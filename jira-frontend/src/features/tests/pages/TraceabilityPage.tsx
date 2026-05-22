import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import {
  Grid3x3, Link2, Plus, Trash2, Search, Filter, Download,
  CheckCircle2, XCircle, AlertTriangle, ExternalLink, RefreshCw
} from 'lucide-react';
import apiClient from '../../../api/axiosClient';

interface TraceabilityLink {
  id: string;
  requirementKey: string;
  requirementName?: string;
  testId: string;
  testName?: string;
  testKey?: string;
  createdAt: string;
  createdBy?: string;
}

interface TraceabilityMatrixItem {
  requirementKey: string;
  requirementName: string;
  requirementStatus: string;
  tests: Array<{
    testId: string;
    testName: string;
    testKey: string;
    status: string;
    lastRunDate?: string;
  }>;
  coverageStatus: 'COVERED' | 'PARTIAL' | 'UNCOVERED';
  coveragePercent: number;
}

interface DefectLink {
  id: string;
  testId: string;
  testName: string;
  executionId?: string;
  defectKey: string;
  severity?: string;
  linkedAt: string;
  linkedBy?: string;
}

interface TraceabilityMatrixResponse {
  projectId: string;
  matrix: TraceabilityMatrixItem[];
  summary: {
    totalRequirements: number;
    coveredRequirements: number;
    partialCoverage: number;
    uncoveredRequirements: number;
    overallCoveragePercent: number;
  };
}

interface DefectLinkResponse {
  testId: string;
  testName: string;
  defects: DefectLink[];
}

interface RequirementLinkRequest {
  projectId: string;
  requirementKey: string;
  testIds: string[];
}

interface DefectLinkRequest {
  executionId?: string;
  stepResultId?: string;
  defectKey: string;
  severity?: string;
}

const getSeverityColor = (severity?: string): string => {
  switch (severity?.toLowerCase()) {
    case 'critical': return 'bg-red-100 text-red-800';
    case 'high': return 'bg-orange-100 text-orange-800';
    case 'medium': return 'bg-yellow-100 text-yellow-800';
    case 'low': return 'bg-blue-100 text-blue-800';
    default: return 'bg-gray-100 text-gray-800';
  }
};

const getCoverageColor = (percent: number): string => {
  if (percent >= 80) return 'bg-green-500';
  if (percent >= 50) return 'bg-yellow-500';
  return 'bg-red-500';
};

export default function TraceabilityPage() {
  const { projectId } = useParams<{ projectId?: string }>();
  const queryClient = useQueryClient();

  // State
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedRequirement, setSelectedRequirement] = useState<string | null>(null);
  const [showLinkModal, setShowLinkModal] = useState(false);
  const [showDefectModal, setShowDefectModal] = useState(false);
  const [selectedTestId, setSelectedTestId] = useState<string | null>(null);
  const [requirementFilter, setRequirementFilter] = useState<string>('');
  const [coverageFilter, setCoverageFilter] = useState<string>('ALL');

  // Queries
  const { data: matrixData, isLoading, error, refetch } = useQuery<TraceabilityMatrixResponse>({
    queryKey: ['traceability-matrix', projectId],
    queryFn: async () => {
      const params = new URLSearchParams();
      if (projectId) params.append('projectId', projectId);
      const response = await apiClient.get(`/api/traceability/matrix?${params}`);
      return response.data;
    },
    enabled: true,
  });

  const { data: defectLinks = [] } = useQuery<DefectLinkResponse[]>({
    queryKey: ['traceability-defects', projectId],
    queryFn: async () => {
      const response = await apiClient.get('/api/traceability/defects', {
        params: projectId ? { executionId: projectId } : undefined,
      });
      return response.data;
    },
    enabled: true,
  });

  const { data: coverageData } = useQuery({
    queryKey: ['traceability-coverage', selectedRequirement, projectId],
    queryFn: async () => {
      if (!selectedRequirement) return null;
      const params = new URLSearchParams({
        requirementKey: selectedRequirement,
        projectId: projectId || '',
      });
      const response = await apiClient.get(`/api/traceability/coverage?${params}`);
      return response.data;
    },
    enabled: !!selectedRequirement && !!projectId,
  });

  // Mutations
  const linkRequirementMutation = useMutation({
    mutationFn: async (request: RequirementLinkRequest) => {
      const response = await apiClient.post('/api/traceability/requirements', request);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['traceability-matrix'] });
      setShowLinkModal(false);
    },
  });

  const linkDefectMutation = useMutation({
    mutationFn: async (data: { projectId: string; request: DefectLinkRequest }) => {
      const response = await apiClient.post(`/defects/links?projectId=${data.projectId}`, data.request);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['traceability-defects'] });
      setShowDefectModal(false);
      setSelectedTestId(null);
    },
  });

  // Handlers
  const handleLinkRequirement = (testId: string) => {
    setSelectedTestId(testId);
    setShowLinkModal(true);
  };

  const handleLinkDefect = (testId: string) => {
    setSelectedTestId(testId);
    setShowDefectModal(true);
  };

  const submitRequirementLink = (requirementKey: string, testIds: string[]) => {
    if (!projectId) return;
    linkRequirementMutation.mutate({
      projectId,
      requirementKey,
      testIds,
    });
  };

  const submitDefectLink = (defectKey: string, severity?: string) => {
    if (!projectId || !selectedTestId) return;
    linkDefectMutation.mutate({
      projectId,
      request: {
        defectKey,
        severity,
        executionId: undefined,
        stepResultId: undefined,
      },
    });
  };

  // Export matrix
  const handleExportMatrix = () => {
    if (!matrixData) return;
    const csv = [
      'Requirement Key,Requirement Name,Status,Test Name,Test Status,Last Run',
      ...matrixData.matrix.flatMap((row) =>
        row.tests.map((test) =>
          [
            row.requirementKey,
            row.requirementName,
            row.requirementStatus,
            test.testName,
            test.status,
            test.lastRunDate || '',
          ].join(',')
        )
      ),
    ].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `traceability-matrix-${projectId || 'all'}.csv`;
    link.click();
  };

  // Filter logic
  const filteredMatrix = React.useMemo(() => {
    if (!matrixData?.matrix) return [];
    return matrixData.matrix.filter((item) => {
      const matchesSearch =
        !searchTerm ||
        item.requirementKey.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.requirementName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.tests.some((t) =>
          t.testName.toLowerCase().includes(searchTerm.toLowerCase())
        );
      const matchesRequirement = !requirementFilter || item.requirementKey.includes(requirementFilter);
      const matchesCoverage =
        coverageFilter === 'ALL' ||
        (coverageFilter === 'COVERED' && item.coverageStatus === 'COVERED') ||
        (coverageFilter === 'PARTIAL' && item.coverageStatus === 'PARTIAL') ||
        (coverageFilter === 'UNCOVERED' && item.coverageStatus === 'UNCOVERED');
      return matchesSearch && matchesRequirement && matchesCoverage;
    });
  }, [matrixData, searchTerm, requirementFilter, coverageFilter]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="w-8 h-8 border-4 border-jira-blue border-t-transparent rounded-full animate-spin" />
        <span className="ml-4 text-gray-600">Loading traceability matrix...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
        <AlertTriangle className="w-12 h-12 text-red-500 mx-auto mb-4" />
        <h3 className="text-lg font-semibold text-red-800 mb-2">Failed to Load Traceability Matrix</h3>
        <p className="text-red-600 mb-4">{error instanceof Error ? error.message : 'Unknown error'}</p>
        <button
          onClick={() => refetch()}
          className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 flex items-center gap-2 mx-auto"
        >
          <RefreshCw size={16} /> Retry
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="bg-white rounded-lg border p-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-semibold text-gray-900 flex items-center gap-2">
              <Grid3x3 size={24} />
              Traceability Matrix
            </h1>
            <p className="text-gray-600 mt-1">
              View requirement-test coverage and link defects to test executions
            </p>
          </div>
          <button
            onClick={handleExportMatrix}
            className="px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg flex items-center gap-2"
          >
            <Download size={16} /> Export CSV
          </button>
        </div>

        {/* Summary Cards */}
        {matrixData?.summary && (
          <div className="grid grid-cols-5 gap-4 mt-6">
            <div className="bg-blue-50 rounded-lg p-4">
              <div className="text-2xl font-bold text-blue-700">
                {matrixData.summary.totalRequirements}
              </div>
              <div className="text-sm text-blue-600">Total Requirements</div>
            </div>
            <div className="bg-green-50 rounded-lg p-4">
              <div className="text-2xl font-bold text-green-700">
                {matrixData.summary.coveredRequirements}
              </div>
              <div className="text-sm text-green-600">Fully Covered</div>
            </div>
            <div className="bg-yellow-50 rounded-lg p-4">
              <div className="text-2xl font-bold text-yellow-700">
                {matrixData.summary.partialCoverage}
              </div>
              <div className="text-sm text-yellow-600">Partial Coverage</div>
            </div>
            <div className="bg-red-50 rounded-lg p-4">
              <div className="text-2xl font-bold text-red-700">
                {matrixData.summary.uncoveredRequirements}
              </div>
              <div className="text-sm text-red-600">Uncovered</div>
            </div>
            <div className="bg-purple-50 rounded-lg p-4">
              <div className="text-2xl font-bold text-purple-700">
                {matrixData.summary.overallCoveragePercent}%
              </div>
              <div className="text-sm text-purple-600">Overall Coverage</div>
            </div>
          </div>
        )}
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg border p-4">
        <div className="flex items-center gap-4 flex-wrap">
          <div className="relative flex-1 min-w-[300px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
            <input
              type="text"
              placeholder="Search requirements or tests..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-jira-blue"
            />
          </div>
          <div className="flex items-center gap-2">
            <Filter size={18} className="text-gray-500" />
            <select
              value={coverageFilter}
              onChange={(e) => setCoverageFilter(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-jira-blue"
            >
              <option value="ALL">All Coverage</option>
              <option value="COVERED">Fully Covered</option>
              <option value="PARTIAL">Partial Coverage</option>
              <option value="UNCOVERED">Uncovered</option>
            </select>
            <input
              type="text"
              placeholder="Filter by requirement key..."
              value={requirementFilter}
              onChange={(e) => setRequirementFilter(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-jira-blue"
            />
          </div>
          <button
            onClick={() => refetch()}
            className="px-3 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 flex items-center gap-2"
          >
            <RefreshCw size={16} /> Refresh
          </button>
        </div>
      </div>

      {/* Traceability Matrix Table */}
      <div className="bg-white rounded-lg border overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-600">
                  Requirement
                </th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-600">
                  Status
                </th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-600">
                  Tests
                </th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-600">
                  Coverage
                </th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-600">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {filteredMatrix.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-4 py-12 text-center text-gray-500">
                    {matrixData?.matrix.length === 0
                      ? 'No traceability data found. Link requirements to tests to see coverage.'
                      : 'No requirements match your filters.'}
                  </td>
                </tr>
              ) : (
                filteredMatrix.map((item) => (
                  <tr key={item.requirementKey} className="hover:bg-gray-50">
                    <td className="px-4 py-3">
                      <div>
                        <div className="font-medium text-gray-900">{item.requirementKey}</div>
                        <div className="text-sm text-gray-500">{item.requirementName}</div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`px-2 py-1 rounded text-xs font-medium ${
                          item.requirementStatus === 'ACTIVE'
                            ? 'bg-green-100 text-green-800'
                            : item.requirementStatus === 'DEPRECATED'
                            ? 'bg-gray-100 text-gray-800'
                            : 'bg-yellow-100 text-yellow-800'
                        }`}
                      >
                        {item.requirementStatus}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-2">
                        {item.tests.length === 0 ? (
                          <span className="text-sm text-gray-400 italic">No tests linked</span>
                        ) : (
                          item.tests.map((test) => (
                            <span
                              key={test.testId}
                              className="inline-flex items-center gap-1 px-2 py-1 bg-blue-50 text-blue-700 rounded text-xs"
                              title={`Last run: ${test.lastRunDate || 'Never'}`}
                            >
                              {test.testKey || test.testName}
                              {test.status === 'PASSED' && (
                                <CheckCircle2 size={12} className="text-green-600" />
                              )}
                              {test.status === 'FAILED' && (
                                <XCircle size={12} className="text-red-600" />
                              )}
                            </span>
                          ))
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div className="w-24 h-2 bg-gray-200 rounded-full overflow-hidden">
                          <div
                            className={`h-full ${getCoverageColor(item.coveragePercent)}`}
                            style={{ width: `${item.coveragePercent}%` }}
                          />
                        </div>
                        <span className="text-sm font-medium">{item.coveragePercent}%</span>
                        <span
                          className={`text-xs px-2 py-0.5 rounded ${
                            item.coverageStatus === 'COVERED'
                              ? 'bg-green-100 text-green-700'
                              : item.coverageStatus === 'PARTIAL'
                              ? 'bg-yellow-100 text-yellow-700'
                              : 'bg-red-100 text-red-700'
                          }`}
                        >
                          {item.coverageStatus}
                        </span>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => setSelectedRequirement(item.requirementKey)}
                          className="p-1 text-blue-600 hover:bg-blue-50 rounded"
                          title="View coverage details"
                        >
                          <ExternalLink size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Defect Links Section */}
      <div className="bg-white rounded-lg border overflow-hidden">
        <div className="px-4 py-3 bg-red-50 border-b border-red-100">
          <h2 className="text-lg font-semibold text-gray-900 flex items-center gap-2">
            <XCircle size={20} className="text-red-600" />
            Defect Links
          </h2>
          <p className="text-sm text-gray-600 mt-1">
            Track defects linked to test executions
          </p>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-600">
                  Test
                </th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-600">
                  Execution
                </th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-600">
                  Defect Key
                </th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-600">
                  Severity
                </th>
                <th className="px-4 py-3 text-left text-xs font-semibold uppercase text-gray-600">
                  Linked
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {defectLinks.flatMap((link) =>
                link.defects.map((defect) => (
                  <tr key={defect.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-medium text-gray-900">{link.testName}</td>
                    <td className="px-4 py-3 text-gray-500">{defect.executionId || '—'}</td>
                    <td className="px-4 py-3">
                      <a
                        href={`/browse/${defect.defectKey}`}
                        className="text-blue-600 hover:underline"
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        {defect.defectKey}
                      </a>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-1 rounded text-xs font-medium ${getSeverityColor(defect.severity)}`}>
                        {defect.severity || 'Unknown'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500">
                      {new Date(defect.linkedAt).toLocaleDateString()}
                    </td>
                  </tr>
                ))
              )}
              {defectLinks.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-gray-500">
                    No defect links found
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Link Requirement Modal */}
      {showLinkModal && (
        <LinkRequirementModal
          onClose={() => setShowLinkModal(false)}
          onSubmit={submitRequirementLink}
          isLoading={linkRequirementMutation.isPending}
          projectId={projectId || ''}
        />
      )}

      {/* Link Defect Modal */}
      {showDefectModal && selectedTestId && (
        <LinkDefectModal
          testId={selectedTestId}
          onClose={() => {
            setShowDefectModal(false);
            setSelectedTestId(null);
          }}
          onSubmit={submitDefectLink}
          isLoading={linkDefectMutation.isPending}
        />
      )}

      {/* Coverage Detail Panel */}
      {selectedRequirement && (
        <CoverageDetailPanel
          requirementKey={selectedRequirement}
          coverageData={coverageData}
          onClose={() => setSelectedRequirement(null)}
        />
      )}
    </div>
  );
}

// Link Requirement Modal
interface LinkRequirementModalProps {
  onClose: () => void;
  onSubmit: (requirementKey: string, testIds: string[]) => void;
  isLoading: boolean;
  projectId: string;
}

function LinkRequirementModal({ onClose, onSubmit, isLoading, projectId }: LinkRequirementModalProps) {
  const [requirementKey, setRequirementKey] = useState('');
  const [testIds, setTestIds] = useState<string[]>([]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (requirementKey && testIds.length > 0) {
      onSubmit(requirementKey, testIds);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onClose} />
        <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h3 className="text-lg font-semibold mb-4">Link Requirement to Tests</h3>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Requirement Key *
              </label>
              <input
                type="text"
                value={requirementKey}
                onChange={(e) => setRequirementKey(e.target.value)}
                placeholder="e.g., PROJ-123"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-jira-blue"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Test IDs (comma-separated) *
              </label>
              <textarea
                value={testIds.join(', ')}
                onChange={(e) => setTestIds(e.target.value.split(',').map((id) => id.trim()).filter(Boolean))}
                placeholder="e.g., test-uuid-1, test-uuid-2"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-jira-blue"
                rows={3}
                required
              />
            </div>
            <div className="flex justify-end gap-3 pt-4">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 text-gray-600 hover:text-gray-900"
                disabled={isLoading}
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isLoading || !requirementKey || testIds.length === 0}
                className="px-4 py-2 bg-jira-blue text-white rounded-lg hover:bg-blue-600 disabled:opacity-50 flex items-center gap-2"
              >
                {isLoading && <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />}
                Link Requirement
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

// Link Defect Modal
interface LinkDefectModalProps {
  testId: string;
  onClose: () => void;
  onSubmit: (defectKey: string, severity?: string) => void;
  isLoading: boolean;
}

function LinkDefectModal({ testId, onClose, onSubmit, isLoading }: LinkDefectModalProps) {
  const [defectKey, setDefectKey] = useState('');
  const [severity, setSeverity] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (defectKey) {
      onSubmit(defectKey, severity || undefined);
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onClose} />
        <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h3 className="text-lg font-semibold mb-4">Link Defect to Test</h3>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Defect Key *
              </label>
              <input
                type="text"
                value={defectKey}
                onChange={(e) => setDefectKey(e.target.value)}
                placeholder="e.g., BUG-456"
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-jira-blue"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Severity
              </label>
              <select
                value={severity}
                onChange={(e) => setSeverity(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-jira-blue"
              >
                <option value="">Select severity...</option>
                <option value="CRITICAL">Critical</option>
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
              </select>
            </div>
            <div className="flex justify-end gap-3 pt-4">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 text-gray-600 hover:text-gray-900"
                disabled={isLoading}
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isLoading || !defectKey}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 flex items-center gap-2"
              >
                {isLoading && <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />}
                Link Defect
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

// Coverage Detail Panel
interface CoverageDetailPanelProps {
  requirementKey: string;
  coverageData: any;
  onClose: () => void;
}

function CoverageDetailPanel({ requirementKey, coverageData, onClose }: CoverageDetailPanelProps) {
  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onClose} />
        <div className="relative bg-white rounded-lg shadow-xl max-w-2xl w-full p-6 max-h-[80vh] overflow-y-auto">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold">
              Coverage for {requirementKey}
            </h3>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600"
            >
              ×
            </button>
          </div>
          {coverageData ? (
            <div className="space-y-4">
              <div className="bg-blue-50 rounded-lg p-4">
                <div className="text-sm text-blue-600 mb-2">Tests covering this requirement</div>
                <div className="space-y-2">
                  {coverageData.map((test: any) => (
                    <div key={test.testId} className="flex items-center justify-between bg-white rounded p-2">
                      <span className="font-medium">{test.testName}</span>
                      <span className={`px-2 py-1 rounded text-xs ${
                        test.status === 'PASSED' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                      }`}>
                        {test.status}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <div className="text-center text-gray-500 py-8">
              Loading coverage details...
            </div>
          )}
        </div>
      </div>
    </div>
  );
}