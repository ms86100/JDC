import React, { useState, useMemo } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import combinedApi, { TestExecutionResponse } from '../../../api/testApi';
import {
  ArrowLeft,
  Search,
  Filter,
  Calendar,
  Clock,
  User,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  SkipForward,
  Play,
  Download,
  ChevronDown,
  ChevronRight,
  RefreshCw,
  BarChart3,
  Hash,
  Monitor,
  Tag,
  ExternalLink,
} from 'lucide-react';

interface ExecutionFilters {
  status: string;
  dateRange: string;
  assignee: string;
  search: string;
}

const initialFilters: ExecutionFilters = {
  status: '',
  dateRange: '',
  assignee: '',
  search: '',
};

interface GroupedExecution {
  date: string;
  executions: TestExecutionResponse[];
}

const StatusConfig: Record<string, { icon: React.ReactNode; color: string; label: string }> = {
  PASSED: { icon: <CheckCircle2 className="w-4 h-4" />, color: 'text-green-600 bg-green-100', label: 'Passed' },
  FAILED: { icon: <XCircle className="w-4 h-4" />, color: 'text-red-600 bg-red-100', label: 'Failed' },
  BLOCKED: { icon: <AlertTriangle className="w-4 h-4" />, color: 'text-orange-600 bg-orange-100', label: 'Blocked' },
  SKIPPED: { icon: <SkipForward className="w-4 h-4" />, color: 'text-gray-600 bg-gray-100', label: 'Skipped' },
  IN_PROGRESS: { icon: <Play className="w-4 h-4" />, color: 'text-blue-600 bg-blue-100', label: 'In Progress' },
  TODO: { icon: <Clock className="w-4 h-4" />, color: 'text-gray-600 bg-gray-100', label: 'Todo' },
};

export const TestExecutionHistoryPage: React.FC = () => {
  const { testId } = useParams<{ testId: string }>();
  const navigate = useNavigate();
  const [filters, setFilters] = useState<ExecutionFilters>(initialFilters);
  const [expandedExecution, setExpandedExecution] = useState<string | null>(null);
  const [showFilters, setShowFilters] = useState(false);

  // Fetch test info
  const { data: test } = useQuery({
    queryKey: ['test', testId],
    queryFn: () => combinedApi.getTest(testId!),
    enabled: !!testId,
  });

  // Fetch execution history
  const { data: executionHistory = [], isLoading, refetch } = useQuery({
    queryKey: ['execution-history', testId],
    queryFn: async () => {
      if (!testId) return [];
      const response = await combinedApi.getExecutionHistory(testId);
      // Flatten the response to get all executions
      return response.flatMap(item => item.executions);
    },
    enabled: !!testId,
  });

  // Fetch executions by test directly
  const { data: directExecutions = [], isLoading: directLoading } = useQuery({
    queryKey: ['executions-direct', testId],
    queryFn: () => combinedApi.getExecutionsByTest(testId!),
    enabled: !!testId,
  });

  // Combine both data sources
  const allExecutions = useMemo(() => {
    const combined = [...directExecutions, ...executionHistory];
    // Remove duplicates based on id
    const unique = combined.reduce((acc, exec) => {
      if (!acc.find(e => e.id === exec.id)) {
        acc.push(exec);
      }
      return acc;
    }, [] as TestExecutionResponse[]);
    // Sort by date descending
    return unique.sort((a, b) =>
      new Date(b.startedAt || b.createdAt || 0).getTime() -
      new Date(a.startedAt || a.createdAt || 0).getTime()
    );
  }, [directExecutions, executionHistory]);

  // Calculate statistics
  const stats = useMemo(() => {
    return {
      total: allExecutions.length,
      passed: allExecutions.filter(e => e.status === 'PASSED').length,
      failed: allExecutions.filter(e => e.status === 'FAILED').length,
      blocked: allExecutions.filter(e => e.status === 'BLOCKED').length,
      skipped: allExecutions.filter(e => e.status === 'SKIPPED').length,
    };
  }, [allExecutions]);

  // Filter executions
  const filteredExecutions = useMemo(() => {
    return allExecutions.filter(exec => {
      if (filters.status && exec.status !== filters.status) return false;
      if (filters.assignee && exec.assigneeId !== filters.assignee) return false;
      if (filters.search) {
        const searchLower = filters.search.toLowerCase();
        const matchesKey = exec.issueKey?.toLowerCase().includes(searchLower);
        const matchesName = exec.name?.toLowerCase().includes(searchLower);
        if (!matchesKey && !matchesName) return false;
      }
      return true;
    });
  }, [allExecutions, filters]);

  // Group executions by date
  const groupedExecutions = useMemo(() => {
    const groups: GroupedExecution[] = [];
    const dateMap = new Map<string, TestExecutionResponse[]>();

    filteredExecutions.forEach(exec => {
      const date = new Date(exec.startedAt || exec.createdAt || Date.now()).toLocaleDateString();
      if (!dateMap.has(date)) {
        dateMap.set(date, []);
      }
      dateMap.get(date)!.push(exec);
    });

    dateMap.forEach((execs, date) => {
      groups.push({ date, executions: execs });
    });

    return groups;
  }, [filteredExecutions]);

  const getStatusConfig = (status: string) => {
    return StatusConfig[status] || { icon: <Clock className="w-4 h-4" />, color: 'text-gray-600 bg-gray-100', label: status };
  };

  const formatDuration = (ms?: number) => {
    if (!ms) return '-';
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    if (minutes > 0) {
      return `${minutes}m ${seconds % 60}s`;
    }
    return `${seconds}s`;
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString();
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-6 py-4">
          {/* Breadcrumb */}
          <div className="flex items-center gap-2 text-sm text-gray-500 mb-4">
            <button onClick={() => navigate(-1)} className="hover:text-blue-600 flex items-center gap-1">
              <ArrowLeft className="w-4 h-4" />
              Back
            </button>
            <span>/</span>
            <Link to="/tests" className="hover:text-blue-600">Tests</Link>
            <span>/</span>
            <span className="text-gray-900">{test?.issueKey || testId}</span>
            <span>/</span>
            <span>History</span>
          </div>

          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Test Execution History</h1>
              <p className="text-sm text-gray-500 mt-1">
                {test?.name || 'Loading...'} - {stats.total} executions
              </p>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={() => refetch()}
                className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                <RefreshCw className="w-4 h-4" />
                Refresh
              </button>
              <button className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700">
                <Play className="w-4 h-4" />
                Run Test
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="max-w-7xl mx-auto px-6 py-4">
        <div className="grid grid-cols-5 gap-4">
          <div className="bg-white rounded-lg border p-4">
            <div className="text-sm text-gray-500">Total Executions</div>
            <div className="text-2xl font-bold mt-1">{stats.total}</div>
          </div>
          <div className="bg-white rounded-lg border p-4">
            <div className="text-sm text-gray-500">Passed</div>
            <div className="text-2xl font-bold mt-1 text-green-600">{stats.passed}</div>
          </div>
          <div className="bg-white rounded-lg border p-4">
            <div className="text-sm text-gray-500">Failed</div>
            <div className="text-2xl font-bold mt-1 text-red-600">{stats.failed}</div>
          </div>
          <div className="bg-white rounded-lg border p-4">
            <div className="text-sm text-gray-500">Blocked</div>
            <div className="text-2xl font-bold mt-1 text-orange-600">{stats.blocked}</div>
          </div>
          <div className="bg-white rounded-lg border p-4">
            <div className="text-sm text-gray-500">Skipped</div>
            <div className="text-2xl font-bold mt-1 text-gray-600">{stats.skipped}</div>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="max-w-7xl mx-auto px-6 py-2">
        <div className="bg-white rounded-lg border">
          <div className="p-4 flex items-center gap-4 border-b border-gray-200">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                placeholder="Search executions..."
                value={filters.search}
                onChange={(e) => setFilters({ ...filters, search: e.target.value })}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <select
              value={filters.status}
              onChange={(e) => setFilters({ ...filters, status: e.target.value })}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">All Statuses</option>
              <option value="PASSED">Passed</option>
              <option value="FAILED">Failed</option>
              <option value="BLOCKED">Blocked</option>
              <option value="SKIPPED">Skipped</option>
              <option value="IN_PROGRESS">In Progress</option>
            </select>
            <button
              onClick={() => setShowFilters(!showFilters)}
              className={`flex items-center gap-2 px-4 py-2 border rounded-lg ${
                showFilters ? 'bg-blue-50 border-blue-500 text-blue-600' : 'hover:bg-gray-50'
              }`}
            >
              <Filter className="w-4 h-4" />
              Filters
              <ChevronDown className={`w-4 h-4 transition-transform ${showFilters ? 'rotate-180' : ''}`} />
            </button>
          </div>

          {showFilters && (
            <div className="p-4 bg-gray-50 border-t border-gray-200">
              <div className="grid grid-cols-4 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Date Range</label>
                  <select
                    value={filters.dateRange}
                    onChange={(e) => setFilters({ ...filters, dateRange: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">All Time</option>
                    <option value="7d">Last 7 days</option>
                    <option value="30d">Last 30 days</option>
                    <option value="90d">Last 90 days</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Assignee</label>
                  <select
                    value={filters.assignee}
                    onChange={(e) => setFilters({ ...filters, assignee: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">All Assignees</option>
                    <option value="current_user">Current User</option>
                  </select>
                </div>
                <div className="flex items-end">
                  <button
                    onClick={() => setFilters(initialFilters)}
                    className="px-4 py-2 text-gray-600 hover:bg-gray-200 rounded-lg"
                  >
                    Clear Filters
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Execution List */}
      <div className="max-w-7xl mx-auto px-6 py-4">
        {isLoading || directLoading ? (
          <div className="flex items-center justify-center h-64">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          </div>
        ) : filteredExecutions.length === 0 ? (
          <div className="bg-white rounded-lg border p-12 text-center">
            <BarChart3 className="w-12 h-12 mx-auto text-gray-300 mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">No executions found</h3>
            <p className="text-gray-500">
              {allExecutions.length === 0
                ? 'This test has not been executed yet'
                : 'No executions match your filter criteria'}
            </p>
          </div>
        ) : (
          <div className="space-y-6">
            {groupedExecutions.map((group) => (
              <div key={group.date} className="bg-white rounded-lg border overflow-hidden">
                <div className="px-4 py-3 bg-gray-50 border-b border-gray-200 flex items-center gap-2">
                  <Calendar className="w-4 h-4 text-gray-400" />
                  <span className="font-medium text-gray-700">{group.date}</span>
                  <span className="text-sm text-gray-500">({group.executions.length} executions)</span>
                </div>

                <div className="divide-y divide-gray-100">
                  {group.executions.map((execution) => {
                    const statusConfig = getStatusConfig(execution.status);
                    const isExpanded = expandedExecution === execution.id;

                    return (
                      <div key={execution.id}>
                        <div
                          className="px-4 py-3 hover:bg-gray-50 cursor-pointer"
                          onClick={() => setExpandedExecution(isExpanded ? null : execution.id)}
                        >
                          <div className="flex items-center gap-4">
                            {/* Expand Icon */}
                            <div className="text-gray-400">
                              {isExpanded ? (
                                <ChevronDown className="w-4 h-4" />
                              ) : (
                                <ChevronRight className="w-4 h-4" />
                              )}
                            </div>

                            {/* Status */}
                            <div className={`px-2 py-1 rounded ${statusConfig.color} flex items-center gap-1`}>
                              {statusConfig.icon}
                              <span className="text-sm font-medium">{statusConfig.label}</span>
                            </div>

                            {/* Execution Info */}
                            <div className="flex-1 grid grid-cols-4 gap-4">
                              <div>
                                <div className="text-xs text-gray-500">Started</div>
                                <div className="text-sm font-medium">{formatDate(execution.startedAt)}</div>
                              </div>
                              <div>
                                <div className="text-xs text-gray-500">Duration</div>
                                <div className="text-sm font-medium">{formatDuration(execution.duration)}</div>
                              </div>
                              <div>
                                <div className="text-xs text-gray-500">Environment</div>
                                <div className="text-sm font-medium flex items-center gap-1">
                                  <Monitor className="w-3 h-3 text-gray-400" />
                                  {execution.testEnv || 'Default'}
                                </div>
                              </div>
                              <div>
                                <div className="text-xs text-gray-500">Executed By</div>
                                <div className="text-sm font-medium flex items-center gap-1">
                                  <User className="w-3 h-3 text-gray-400" />
                                  {execution.executedBy || 'System'}
                                </div>
                              </div>
                            </div>

                            {/* Actions */}
                            <div className="flex items-center gap-2">
                              {execution.status === 'FAILED' && (
                                <Link
                                  to={`/tests/defects?executionId=${execution.id}`}
                                  className="px-3 py-1 text-sm border border-red-300 text-red-600 rounded hover:bg-red-50"
                                >
                                  Create Defect
                                </Link>
                              )}
                              <Link
                                to={`/tests/evidence?executionId=${execution.id}`}
                                className="p-2 hover:bg-gray-100 rounded"
                                title="View Evidence"
                              >
                                <ExternalLink className="w-4 h-4 text-gray-400" />
                              </Link>
                            </div>
                          </div>
                        </div>

                        {/* Expanded Details */}
                        {isExpanded && (
                          <div className="px-4 py-4 bg-gray-50 border-t border-gray-100">
                            {/* Step Results */}
                            {execution.stepResults && execution.stepResults.length > 0 ? (
                              <div>
                                <h4 className="font-medium text-gray-700 mb-3">Step Results</h4>
                                <div className="space-y-2">
                                  {execution.stepResults.map((step, index) => {
                                    const stepStatus = getStatusConfig(step.status);
                                    return (
                                      <div key={index} className="flex items-start gap-3 p-3 bg-white rounded border">
                                        <div className={`px-2 py-1 rounded ${stepStatus.color} flex items-center gap-1`}>
                                          {stepStatus.icon}
                                        </div>
                                        <div className="flex-1">
                                          <div className="font-medium text-sm">Step {step.stepIndex + 1}</div>
                                          <div className="text-sm text-gray-600 mt-1">{step.description}</div>
                                          {step.comment && (
                                            <div className="mt-2 p-2 bg-gray-50 rounded text-sm">
                                              <span className="font-medium">Comment:</span> {step.comment}
                                            </div>
                                          )}
                                        </div>
                                      </div>
                                    );
                                  })}
                                </div>
                              </div>
                            ) : (
                              <div className="text-center py-4 text-gray-500">
                                No step results available
                              </div>
                            )}

                            {/* Metadata */}
                            <div className="mt-4 pt-4 border-t border-gray-200">
                              <div className="grid grid-cols-4 gap-4 text-sm">
                                <div>
                                  <span className="text-gray-500">Execution ID:</span>
                                  <span className="ml-2 font-mono">{execution.id.slice(0, 8)}...</span>
                                </div>
                                <div>
                                  <span className="text-gray-500">Cycle:</span>
                                  <span className="ml-2">{execution.testCycle || 'N/A'}</span>
                                </div>
                                <div>
                                  <span className="text-gray-500">Finished:</span>
                                  <span className="ml-2">{formatDate(execution.finishedAt)}</span>
                                </div>
                                <div>
                                  <span className="text-gray-500">Assignee:</span>
                                  <span className="ml-2">{execution.assigneeId || 'Unassigned'}</span>
                                </div>
                              </div>
                            </div>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default TestExecutionHistoryPage;