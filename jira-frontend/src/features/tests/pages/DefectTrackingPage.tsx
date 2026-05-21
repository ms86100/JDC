import React, { useState, useMemo } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axiosClient from '../../../api/axiosClient';
import {
  Search,
  Filter,
  Plus,
  ChevronDown,
  ChevronRight,
  ExternalLink,
  Bug,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Clock,
  User,
  Calendar,
  Tag,
  ArrowUpRight,
  ArrowDownRight,
  MoreVertical,
  RefreshCw,
  Trash2,
  Edit2,
  Link2,
  X,
  Loader2,
  AlertCircle,
  BarChart3,
} from 'lucide-react';

// Types
interface Defect {
  id: string;
  key: string;
  summary: string;
  status: string;
  priority: string;
  severity: string;
  assignee?: {
    id: string;
    displayName: string;
  };
  reporter?: {
    id: string;
    displayName: string;
  };
  testExecutionId?: string;
  testIssueKey?: string;
  environment?: string;
  createdAt: string;
  updatedAt: string;
  labels?: string[];
  description?: string;
}

interface DefectStats {
  total: number;
  open: number;
  inProgress: number;
  resolved: number;
  closed: number;
  byPriority: Record<string, number>;
  bySeverity: Record<string, number>;
}

interface CreateDefectRequest {
  projectId: string;
  summary: string;
  description?: string;
  priority?: string;
  severity?: string;
  labels?: string[];
  testExecutionId?: string;
  testIssueKey?: string;
  environment?: string;
}

interface DefectFilters {
  status: string;
  priority: string;
  severity: string;
  search: string;
  assignee: string;
}

const initialFilters: DefectFilters = {
  status: '',
  priority: '',
  severity: '',
  search: '',
  assignee: '',
};

// Status configuration
const StatusConfig: Record<string, { color: string; bgColor: string; icon: React.ReactNode }> = {
  OPEN: { color: 'text-red-600', bgColor: 'bg-red-100', icon: <Bug className="w-4 h-4" /> },
  IN_PROGRESS: { color: 'text-blue-600', bgColor: 'bg-blue-100', icon: <Clock className="w-4 h-4" /> },
  RESOLVED: { color: 'text-green-600', bgColor: 'bg-green-100', icon: <CheckCircle2 className="w-4 h-4" /> },
  CLOSED: { color: 'text-gray-600', bgColor: 'bg-gray-100', icon: <CheckCircle2 className="w-4 h-4" /> },
};

// Priority configuration
const PriorityConfig: Record<string, { label: string; color: string }> = {
  CRITICAL: { label: 'Critical', color: 'bg-red-100 text-red-800' },
  HIGH: { label: 'High', color: 'bg-orange-100 text-orange-800' },
  MEDIUM: { label: 'Medium', color: 'bg-yellow-100 text-yellow-800' },
  LOW: { label: 'Low', color: 'bg-green-100 text-green-800' },
};

// Severity configuration
const SeverityConfig: Record<string, { label: string; color: string }> = {
  BLOCKER: { label: 'Blocker', color: 'bg-red-100 text-red-800' },
  MAJOR: { label: 'Major', color: 'bg-orange-100 text-orange-800' },
  MINOR: { label: 'Minor', color: 'bg-yellow-100 text-yellow-800' },
  TRIVIAL: { label: 'Trivial', color: 'bg-blue-100 text-blue-800' },
};

// Mock data for demonstration
const mockDefects: Defect[] = [
  {
    id: '1',
    key: 'DEF-123',
    summary: 'Login button not responding on Safari browser',
    status: 'OPEN',
    priority: 'HIGH',
    severity: 'MAJOR',
    assignee: { id: '1', displayName: 'John Doe' },
    reporter: { id: '2', displayName: 'Jane Smith' },
    testExecutionId: 'exec-1',
    testIssueKey: 'TEST-456',
    environment: 'Safari 16',
    createdAt: '2024-01-15T10:30:00Z',
    updatedAt: '2024-01-15T10:30:00Z',
    labels: ['browser', 'safari', 'login'],
  },
  {
    id: '2',
    key: 'DEF-124',
    summary: 'Payment validation fails for international cards',
    status: 'IN_PROGRESS',
    priority: 'CRITICAL',
    severity: 'BLOCKER',
    assignee: { id: '3', displayName: 'Bob Wilson' },
    testExecutionId: 'exec-2',
    testIssueKey: 'TEST-789',
    environment: 'Production',
    createdAt: '2024-01-14T09:00:00Z',
    updatedAt: '2024-01-15T08:00:00Z',
    labels: ['payment', 'international'],
  },
  {
    id: '3',
    key: 'DEF-125',
    summary: 'Chart rendering issue in Firefox',
    status: 'RESOLVED',
    priority: 'MEDIUM',
    severity: 'MINOR',
    assignee: { id: '1', displayName: 'John Doe' },
    testExecutionId: 'exec-3',
    environment: 'Firefox 120',
    createdAt: '2024-01-10T14:00:00Z',
    updatedAt: '2024-01-12T16:00:00Z',
  },
  {
    id: '4',
    key: 'DEF-126',
    summary: 'Export to PDF produces blank pages',
    status: 'OPEN',
    priority: 'HIGH',
    severity: 'MAJOR',
    testIssueKey: 'TEST-101',
    environment: 'Chrome 119',
    createdAt: '2024-01-13T11:00:00Z',
    updatedAt: '2024-01-13T11:00:00Z',
  },
];

// API Functions (mock for demo)
const defectApi = {
  getDefects: async (filters?: DefectFilters): Promise<Defect[]> => {
    // In production, replace with actual API call
    // return axiosClient.get('/api/defects', { params: filters }).then(r => r.data);
    return mockDefects;
  },
  getDefect: async (defectId: string): Promise<Defect> => {
    return mockDefects.find(d => d.id === defectId) || mockDefects[0];
  },
  createDefect: async (data: CreateDefectRequest): Promise<Defect> => {
    return {
      id: Math.random().toString(36).substr(2, 9),
      key: `DEF-${Math.floor(Math.random() * 1000) + 127}`,
      summary: data.summary,
      status: 'OPEN',
      priority: data.priority || 'MEDIUM',
      severity: data.severity || 'MINOR',
      testExecutionId: data.testExecutionId,
      testIssueKey: data.testIssueKey,
      environment: data.environment,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
  },
  updateDefect: async (defectId: string, data: Partial<Defect>): Promise<Defect> => {
    return { ...mockDefects[0], ...data };
  },
  deleteDefect: async (defectId: string): Promise<void> => {
    // Implementation
  },
};

// Confirmation Dialog
const ConfirmDialog: React.FC<{
  open: boolean;
  title: string;
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
}> = ({ open, title, message, onConfirm, onCancel }) => {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onCancel}></div>
      <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6 z-10">
        <h3 className="text-lg font-semibold mb-2">{title}</h3>
        <p className="text-gray-600 mb-6">{message}</p>
        <div className="flex justify-end gap-3">
          <button onClick={onCancel} className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50">
            Cancel
          </button>
          <button onClick={onConfirm} className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700">
            Delete
          </button>
        </div>
      </div>
    </div>
  );
};

export const DefectTrackingPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const [filters, setFilters] = useState<DefectFilters>(initialFilters);
  const [expandedDefect, setExpandedDefect] = useState<string | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState<{ open: boolean; defectId: string | null }>({ open: false, defectId: null });

  // Get execution ID from URL params
  const executionIdFromUrl = searchParams.get('executionId');

  // Fetch defects
  const { data: defects = [], isLoading, refetch } = useQuery({
    queryKey: ['defects', filters],
    queryFn: () => defectApi.getDefects(filters),
  });

  // Calculate stats
  const stats = useMemo((): DefectStats => {
    return defects.reduce((acc, defect) => {
      acc.total++;
      if (defect.status === 'OPEN') acc.open++;
      else if (defect.status === 'IN_PROGRESS') acc.inProgress++;
      else if (defect.status === 'RESOLVED') acc.resolved++;
      else if (defect.status === 'CLOSED') acc.closed++;
      acc.byPriority[defect.priority] = (acc.byPriority[defect.priority] || 0) + 1;
      acc.bySeverity[defect.severity] = (acc.bySeverity[defect.severity] || 0) + 1;
      return acc;
    }, { total: 0, open: 0, inProgress: 0, resolved: 0, closed: 0, byPriority: {}, bySeverity: {} } as DefectStats);
  }, [defects]);

  // Filter defects
  const filteredDefects = useMemo(() => {
    return defects.filter(defect => {
      if (filters.status && defect.status !== filters.status) return false;
      if (filters.priority && defect.priority !== filters.priority) return false;
      if (filters.severity && defect.severity !== filters.severity) return false;
      if (filters.assignee && defect.assignee?.id !== filters.assignee) return false;
      if (filters.search) {
        const searchLower = filters.search.toLowerCase();
        const matchesKey = defect.key.toLowerCase().includes(searchLower);
        const matchesSummary = defect.summary.toLowerCase().includes(searchLower);
        if (!matchesKey && !matchesSummary) return false;
      }
      return true;
    });
  }, [defects, filters]);

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: (defectId: string) => defectApi.deleteDefect(defectId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['defects'] });
      setDeleteConfirm({ open: false, defectId: null });
    },
  });

  const handleDelete = () => {
    if (deleteConfirm.defectId) {
      deleteMutation.mutate(deleteConfirm.defectId);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  };

  const getStatusConfig = (status: string) => {
    return StatusConfig[status] || StatusConfig.OPEN;
  };

  const getPriorityConfig = (priority: string) => {
    return PriorityConfig[priority] || PriorityConfig.MEDIUM;
  };

  const getSeverityConfig = (severity: string) => {
    return SeverityConfig[severity] || SeverityConfig.MINOR;
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Defect Tracking</h1>
              <p className="text-sm text-gray-500 mt-1">
                Track and manage test execution defects
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
              <button
                onClick={() => setShowCreateModal(true)}
                className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
              >
                <Plus className="w-4 h-4" />
                Create Defect
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="max-w-7xl mx-auto px-6 py-4">
        <div className="grid grid-cols-5 gap-4">
          <div className="bg-white rounded-lg border p-4 cursor-pointer hover:bg-gray-50" onClick={() => setFilters({ ...filters, status: '' })}>
            <div className="text-sm text-gray-500">Total Defects</div>
            <div className="text-2xl font-bold mt-1">{stats.total}</div>
          </div>
          <div className="bg-white rounded-lg border p-4 cursor-pointer hover:bg-gray-50" onClick={() => setFilters({ ...filters, status: 'OPEN' })}>
            <div className="text-sm text-gray-500 flex items-center gap-1">
              <ArrowUpRight className="w-3 h-3 text-red-500" />
              Open
            </div>
            <div className="text-2xl font-bold mt-1 text-red-600">{stats.open}</div>
          </div>
          <div className="bg-white rounded-lg border p-4 cursor-pointer hover:bg-gray-50" onClick={() => setFilters({ ...filters, status: 'IN_PROGRESS' })}>
            <div className="text-sm text-gray-500">In Progress</div>
            <div className="text-2xl font-bold mt-1 text-blue-600">{stats.inProgress}</div>
          </div>
          <div className="bg-white rounded-lg border p-4 cursor-pointer hover:bg-gray-50" onClick={() => setFilters({ ...filters, status: 'RESOLVED' })}>
            <div className="text-sm text-gray-500">Resolved</div>
            <div className="text-2xl font-bold mt-1 text-green-600">{stats.resolved}</div>
          </div>
          <div className="bg-white rounded-lg border p-4">
            <div className="text-sm text-gray-500">Closed</div>
            <div className="text-2xl font-bold mt-1 text-gray-600">{stats.closed}</div>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="max-w-7xl mx-auto px-6 py-2">
        <div className="bg-white rounded-lg border">
          <div className="p-4 flex items-center gap-4">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                placeholder="Search by key or summary..."
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
              <option value="OPEN">Open</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="RESOLVED">Resolved</option>
              <option value="CLOSED">Closed</option>
            </select>
            <select
              value={filters.priority}
              onChange={(e) => setFilters({ ...filters, priority: e.target.value })}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">All Priorities</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>
            <select
              value={filters.severity}
              onChange={(e) => setFilters({ ...filters, severity: e.target.value })}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">All Severities</option>
              <option value="BLOCKER">Blocker</option>
              <option value="MAJOR">Major</option>
              <option value="MINOR">Minor</option>
              <option value="TRIVIAL">Trivial</option>
            </select>
            <button
              onClick={() => setFilters(initialFilters)}
              className="px-4 py-2 text-gray-600 hover:bg-gray-100 rounded-lg"
            >
              Clear Filters
            </button>
          </div>
        </div>
      </div>

      {/* Defect List */}
      <div className="max-w-7xl mx-auto px-6 py-4">
        {isLoading ? (
          <div className="flex items-center justify-center h-64">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          </div>
        ) : filteredDefects.length === 0 ? (
          <div className="bg-white rounded-lg border p-12 text-center">
            <Bug className="w-12 h-12 mx-auto text-gray-300 mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">No defects found</h3>
            <p className="text-gray-500">
              {defects.length === 0
                ? 'No defects have been created yet'
                : 'No defects match your filter criteria'}
            </p>
          </div>
        ) : (
          <div className="bg-white rounded-lg border overflow-hidden">
            <table className="w-full">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200">
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-8"></th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Key</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Summary</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Priority</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Severity</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Assignee</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Created</th>
                  <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {filteredDefects.map((defect) => {
                  const statusConfig = getStatusConfig(defect.status);
                  const priorityConfig = getPriorityConfig(defect.priority);
                  const severityConfig = getSeverityConfig(defect.severity);
                  const isExpanded = expandedDefect === defect.id;

                  return (
                    <React.Fragment key={defect.id}>
                      <tr
                        className="hover:bg-gray-50 cursor-pointer"
                        onClick={() => setExpandedDefect(isExpanded ? null : defect.id)}
                      >
                        <td className="px-4 py-3">
                          {isExpanded ? (
                            <ChevronDown className="w-4 h-4 text-gray-400" />
                          ) : (
                            <ChevronRight className="w-4 h-4 text-gray-400" />
                          )}
                        </td>
                        <td className="px-4 py-3">
                          <span className="font-mono text-sm text-blue-600 hover:underline cursor-pointer">
                            {defect.key}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <span className="text-sm text-gray-900 line-clamp-1">{defect.summary}</span>
                        </td>
                        <td className="px-4 py-3">
                          <span className={`inline-flex items-center gap-1 px-2 py-1 rounded ${statusConfig.bgColor} ${statusConfig.color}`}>
                            {statusConfig.icon}
                            <span className="text-xs font-medium">{defect.status.replace('_', ' ')}</span>
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <span className={`px-2 py-1 rounded text-xs font-medium ${priorityConfig.color}`}>
                            {priorityConfig.label}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <span className={`px-2 py-1 rounded text-xs font-medium ${severityConfig.color}`}>
                            {severityConfig.label}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2">
                            <div className="w-6 h-6 rounded-full bg-blue-100 flex items-center justify-center text-xs text-blue-600 font-medium">
                              {defect.assignee?.displayName.charAt(0) || '?'}
                            </div>
                            <span className="text-sm text-gray-600">{defect.assignee?.displayName || 'Unassigned'}</span>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-500">
                          {formatDate(defect.createdAt)}
                        </td>
                        <td className="px-4 py-3 text-right" onClick={(e) => e.stopPropagation()}>
                          <div className="flex items-center justify-end gap-1">
                            <Link
                              to={`/tests/evidence?executionId=${defect.testExecutionId}`}
                              className="p-2 hover:bg-gray-100 rounded"
                              title="View Evidence"
                            >
                              <ExternalLink className="w-4 h-4 text-gray-400" />
                            </Link>
                            <button
                              onClick={() => setDeleteConfirm({ open: true, defectId: defect.id })}
                              className="p-2 hover:bg-red-50 rounded"
                              title="Delete"
                            >
                              <Trash2 className="w-4 h-4 text-gray-400 hover:text-red-600" />
                            </button>
                          </div>
                        </td>
                      </tr>

                      {/* Expanded Row */}
                      {isExpanded && (
                        <tr>
                          <td colSpan={9} className="px-4 py-4 bg-gray-50">
                            <div className="grid grid-cols-3 gap-6">
                              {/* Left: Details */}
                              <div className="col-span-2 space-y-4">
                                <div>
                                  <h4 className="text-sm font-medium text-gray-700 mb-2">Description</h4>
                                  <p className="text-sm text-gray-600 bg-white p-3 rounded border">
                                    {defect.description || 'No description provided.'}
                                  </p>
                                </div>

                                {defect.labels && defect.labels.length > 0 && (
                                  <div>
                                    <h4 className="text-sm font-medium text-gray-700 mb-2">Labels</h4>
                                    <div className="flex flex-wrap gap-2">
                                      {defect.labels.map((label) => (
                                        <span
                                          key={label}
                                          className="inline-flex items-center gap-1 px-2 py-1 bg-gray-100 text-gray-600 rounded text-xs"
                                        >
                                          <Tag className="w-3 h-3" />
                                          {label}
                                        </span>
                                      ))}
                                    </div>
                                  </div>
                                )}

                                <div className="grid grid-cols-2 gap-4">
                                  {defect.testIssueKey && (
                                    <div>
                                      <h4 className="text-sm font-medium text-gray-700 mb-1">Test</h4>
                                      <Link
                                        to={`/tests/detail/${defect.testIssueKey}`}
                                        className="text-sm text-blue-600 hover:underline"
                                      >
                                        {defect.testIssueKey}
                                      </Link>
                                    </div>
                                  )}
                                  {defect.environment && (
                                    <div>
                                      <h4 className="text-sm font-medium text-gray-700 mb-1">Environment</h4>
                                      <span className="text-sm text-gray-600">{defect.environment}</span>
                                    </div>
                                  )}
                                </div>
                              </div>

                              {/* Right: Metadata */}
                              <div className="space-y-4">
                                <div className="bg-white p-4 rounded border">
                                  <h4 className="text-sm font-medium text-gray-700 mb-3">Details</h4>
                                  <dl className="space-y-2 text-sm">
                                    <div>
                                      <dt className="text-gray-500">Reporter</dt>
                                      <dd className="font-medium">{defect.reporter?.displayName || 'Unknown'}</dd>
                                    </div>
                                    <div>
                                      <dt className="text-gray-500">Created</dt>
                                      <dd className="font-medium">{formatDate(defect.createdAt)}</dd>
                                    </div>
                                    <div>
                                      <dt className="text-gray-500">Updated</dt>
                                      <dd className="font-medium">{formatDate(defect.updatedAt)}</dd>
                                    </div>
                                  </dl>
                                </div>
                              </div>
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Delete Confirmation */}
      <ConfirmDialog
        open={deleteConfirm.open}
        title="Delete Defect"
        message="Are you sure you want to delete this defect? This action cannot be undone."
        onConfirm={handleDelete}
        onCancel={() => setDeleteConfirm({ open: false, defectId: null })}
      />

      {/* Create Modal Placeholder */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="fixed inset-0 bg-black bg-opacity-50" onClick={() => setShowCreateModal(false)}></div>
          <div className="relative bg-white rounded-lg shadow-xl max-w-lg w-full p-6 z-10">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Create Defect</h3>
              <button onClick={() => setShowCreateModal(false)} className="text-gray-400 hover:text-gray-600">
                <X className="w-5 h-5" />
              </button>
            </div>
            <p className="text-gray-500">This is a placeholder for the defect creation form.</p>
            {executionIdFromUrl && (
              <p className="mt-2 text-sm text-blue-600">Linked to execution: {executionIdFromUrl}</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default DefectTrackingPage;