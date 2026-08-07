import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../../api/axiosClient';
import {
  TrendingUp, TrendingDown, AlertTriangle, CheckCircle, XCircle,
  Target, BarChart3, PieChart, Activity, Filter, Download,
  Settings, Bell, BellOff, Eye, RefreshCw, Calendar, ChevronDown, ChevronRight
} from 'lucide-react';

// Types
interface CoverageRule {
  id: string;
  name: string;
  type: 'REQUIREMENT' | 'COMPONENT' | 'TAG';
  targetPercent: number;
  isActive: boolean;
  createdAt: string;
}

interface CoverageThreshold {
  id: string;
  name: string;
  warningPercent: number;
  criticalPercent: number;
  projectId?: string;
}

interface CoverageDrift {
  requirementKey: string;
  previousCoverage: number;
  currentCoverage: number;
  driftPercent: number;
  affectedTests: string[];
  detectedAt: string;
}

interface ProjectCoverage {
  projectId: string;
  projectName: string;
  totalRequirements: number;
  coveredRequirements: number;
  coveragePercent: number;
  trend: 'up' | 'down' | 'stable';
  lastUpdated: string;
}

interface CoverageAlert {
  id: string;
  type: 'WARNING' | 'CRITICAL' | 'INFO';
  message: string;
  requirementKey?: string;
  projectId?: string;
  createdAt: string;
  isRead: boolean;
}

// Stats Card
interface StatsCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  color?: string;
  trend?: 'up' | 'down' | 'stable';
}

const StatsCard: React.FC<StatsCardProps> = ({ title, value, subtitle, icon, color = 'blue', trend }) => {
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
          <p className="text-2xl font-bold mt-1 flex items-center gap-2">
            {value}
            {trend && (
              trend === 'up' ? <TrendingUp size={16} className="text-green-500" /> :
              trend === 'down' ? <TrendingDown size={16} className="text-red-500" /> :
              <Activity size={16} className="text-gray-400" />
            )}
          </p>
          {subtitle && <p className="text-xs text-gray-400 mt-1">{subtitle}</p>}
        </div>
        <div className={`${colors.bg} p-3 rounded-lg`}>
          <div className={colors.icon}>{icon}</div>
        </div>
      </div>
    </div>
  );
};

// Coverage Bar
interface CoverageBarProps {
  percent: number;
  showLabel?: boolean;
}

const CoverageBar: React.FC<CoverageBarProps> = ({ percent, showLabel = true }) => {
  const getColor = (p: number) => {
    if (p >= 80) return 'bg-green-500';
    if (p >= 50) return 'bg-yellow-500';
    return 'bg-red-500';
  };

  return (
    <div className="w-full">
      <div className="h-3 bg-gray-200 rounded-full overflow-hidden">
        <div
          className={`h-full ${getColor(percent)} transition-all duration-300`}
          style={{ width: `${percent}%` }}
        />
      </div>
      {showLabel && (
        <p className="text-xs text-gray-500 mt-1">{percent.toFixed(1)}%</p>
      )}
    </div>
  );
};

// Alert Badge
const AlertBadge: React.FC<{ type: string }> = ({ type }) => {
  const typeConfig: Record<string, { bg: string; text: string }> = {
    WARNING: { bg: 'bg-yellow-100', text: 'text-yellow-700' },
    CRITICAL: { bg: 'bg-red-100', text: 'text-red-700' },
    INFO: { bg: 'bg-blue-100', text: 'text-blue-700' },
  };

  const config = typeConfig[type] || typeConfig.INFO;

  return (
    <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${config.bg} ${config.text}`}>
      {type}
    </span>
  );
};

// Create Rule Modal
interface CreateRuleModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

const CreateRuleModal: React.FC<CreateRuleModalProps> = ({ open, onClose, onSuccess }) => {
  const [formData, setFormData] = useState({
    name: '',
    type: 'REQUIREMENT' as string,
    targetPercent: 80,
  });

  const createMutation = useMutation({
    mutationFn: async (data: typeof formData) => {
      const response = await apiClient.post('/coverage/rules', data);
      return response.data;
    },
    onSuccess: () => {
      onSuccess();
      onClose();
      setFormData({ name: '', type: 'REQUIREMENT', targetPercent: 80 });
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
        <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h3 className="text-lg font-semibold mb-4">Create Coverage Rule</h3>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Name *</label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="w-full px-3 py-2 border rounded-lg"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Type</label>
              <select
                value={formData.type}
                onChange={(e) => setFormData({ ...formData, type: e.target.value })}
                className="w-full px-3 py-2 border rounded-lg"
              >
                <option value="REQUIREMENT">Requirement</option>
                <option value="COMPONENT">Component</option>
                <option value="TAG">Tag</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Target Coverage %</label>
              <input
                type="number"
                min="0"
                max="100"
                value={formData.targetPercent}
                onChange={(e) => setFormData({ ...formData, targetPercent: parseInt(e.target.value) })}
                className="w-full px-3 py-2 border rounded-lg"
              />
            </div>
            <div className="flex justify-end gap-3 pt-4">
              <button type="button" onClick={onClose} className="btn btn-secondary">Cancel</button>
              <button type="submit" className="btn btn-primary" disabled={createMutation.isPending}>
                {createMutation.isPending ? 'Creating...' : 'Create'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

// Requirement Coverage Row
interface RequirementRowProps {
  requirementKey: string;
  requirementName: string;
  coverage: number;
  coveredTests: number;
  totalTests: number;
  lastExecuted?: string;
  onExpand: () => void;
  isExpanded: boolean;
}

const RequirementRow: React.FC<RequirementRowProps> = ({
  requirementKey,
  requirementName,
  coverage,
  coveredTests,
  totalTests,
  lastExecuted,
  onExpand,
  isExpanded,
}) => {
  return (
    <>
      <tr
        className="hover:bg-gray-50 cursor-pointer"
        onClick={onExpand}
      >
        <td className="px-4 py-3">
          <button className="p-1">
            {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
          </button>
        </td>
        <td className="px-4 py-3">
          <span className="font-medium">{requirementKey}</span>
          <p className="text-sm text-gray-500">{requirementName}</p>
        </td>
        <td className="px-4 py-3">
          <CoverageBar percent={coverage} showLabel={false} />
        </td>
        <td className="px-4 py-3 text-center">
          {coveredTests}/{totalTests}
        </td>
        <td className="px-4 py-3 text-center">
          {coverage.toFixed(1)}%
        </td>
        <td className="px-4 py-3 text-sm text-gray-500">
          {lastExecuted ? new Date(lastExecuted).toLocaleDateString() : 'Never'}
        </td>
      </tr>
      {isExpanded && (
        <tr>
          <td colSpan={6} className="bg-gray-50 px-8 py-4">
            <div className="text-sm">
              <p className="font-medium mb-2">Test Coverage Details:</p>
              <p>{coveredTests} tests covering this requirement out of {totalTests} total tests.</p>
            </div>
          </td>
        </tr>
      )}
    </>
  );
};

// Main Component
const CoveragePage: React.FC = () => {
  const queryClient = useQueryClient();
  const [selectedProject, setSelectedProject] = useState<string>('all');
  const [expandedRequirements, setExpandedRequirements] = useState<Set<string>>(new Set());
  const [showRules, setShowRules] = useState(false);
  const [showCreateRule, setShowCreateRule] = useState(false);
  const [filterStatus, setFilterStatus] = useState<'ALL' | 'COVERED' | 'PARTIAL' | 'UNCOVERED'>('ALL');
  const [timeRange, setTimeRange] = useState<'7d' | '30d' | '90d' | '1y'>('30d');

  // Fetch project coverage
  const { data: projectCoverage = [], isLoading: projectLoading } = useQuery({
    queryKey: ['coverage-projects', selectedProject],
    queryFn: async () => {
      const params = selectedProject !== 'all' ? { projectId: selectedProject } : {};
      const response = await apiClient.get('/coverage', { params });
      let data = response.data;
      // Handle single object vs array
      if (data && !Array.isArray(data)) {
        data = [data];
      }
      return (data || []) as ProjectCoverage[];
    },
  });

  // Fetch coverage trend
  const { data: coverageTrend = [] } = useQuery({
    queryKey: ['coverage-trend', selectedProject, timeRange],
    queryFn: async () => {
      const response = await apiClient.get(`/api/coverage/${selectedProject}/trend`, {
        params: { range: timeRange },
      });
      return response.data || [];
    },
    enabled: selectedProject !== 'all',
  });

  // Fetch requirements coverage
  const { data: requirementsCoverage = [], isLoading: requirementsLoading } = useQuery({
    queryKey: ['coverage-requirements', selectedProject],
    queryFn: async () => {
      if (selectedProject === 'all') return [];
      const response = await apiClient.get(`/api/coverage/${selectedProject}/requirements`);
      return response.data || [];
    },
    enabled: selectedProject !== 'all',
  });

  // Fetch coverage rules
  const { data: rules = [] } = useQuery({
    queryKey: ['coverage-rules'],
    queryFn: async () => {
      const response = await apiClient.get('/coverage/rules');
      return response.data || [];
    },
    enabled: showRules,
  });

  // Fetch alerts
  const { data: alerts = [] } = useQuery({
    queryKey: ['coverage-alerts'],
    queryFn: async () => {
      const response = await apiClient.get(`/api/coverage/${selectedProject}/alerts`);
      return response.data || [];
    },
    enabled: selectedProject !== 'all',
  });

  // Fetch drift records
  const { data: driftRecords = [] } = useQuery({
    queryKey: ['coverage-drift'],
    queryFn: async () => {
      if (selectedProject === 'all') return [];
      const response = await apiClient.get(`/api/coverage/${selectedProject}/drift`);
      return response.data || [];
    },
    enabled: selectedProject !== 'all',
  });

  // Generate report mutation
  const generateReportMutation = useMutation({
    mutationFn: async () => {
      const response = await apiClient.post('/coverage/reports/generate', {
        projectId: selectedProject,
      });
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['coverage-reports'] });
    },
  });

  // Mark alert as read mutation
  const markAlertMutation = useMutation({
    mutationFn: async (alertId: string) => {
      await apiClient.put(`/api/coverage/alerts/${alertId}/read`);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['coverage-alerts'] });
    },
  });

  // Toggle rule mutation
  const toggleRuleMutation = useMutation({
    mutationFn: async ({ ruleId, active }: { ruleId: string; active: boolean }) => {
      await apiClient.put(`/api/coverage/rules/${ruleId}`, { isActive: active });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['coverage-rules'] });
    },
  });

  // Filter requirements
  const filteredRequirements = requirementsCoverage.filter((req: any) => {
    if (filterStatus === 'ALL') return true;
    const coverage = req.coveragePercent || 0;
    if (filterStatus === 'COVERED' && coverage >= 80) return true;
    if (filterStatus === 'PARTIAL' && coverage >= 20 && coverage < 80) return true;
    if (filterStatus === 'UNCOVERED' && coverage < 20) return true;
    return false;
  });

  // Stats
  const totalCoverage = projectCoverage.reduce((sum, p) => sum + p.coveragePercent, 0) / (projectCoverage.length || 1);
  const totalRequirements = projectCoverage.reduce((sum, p) => sum + p.totalRequirements, 0);
  const coveredRequirements = projectCoverage.reduce((sum, p) => sum + p.coveredRequirements, 0);
  const criticalAlerts = alerts.filter((a) => a.type === 'CRITICAL' && !a.isRead).length;
  const warningsCount = alerts.filter((a) => a.type === 'WARNING' && !a.isRead).length;

  // Toggle requirement expansion
  const toggleExpansion = (requirementKey: string) => {
    const newExpanded = new Set(expandedRequirements);
    if (newExpanded.has(requirementKey)) {
      newExpanded.delete(requirementKey);
    } else {
      newExpanded.add(requirementKey);
    }
    setExpandedRequirements(newExpanded);
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Coverage Engine</h1>
            <p className="text-sm text-gray-500 mt-1">
              Track and manage test coverage across requirements
            </p>
          </div>
          <div className="flex gap-2">
            <select
              value={timeRange}
              onChange={(e) => setTimeRange(e.target.value as any)}
              className="px-3 py-2 border rounded-lg text-sm"
            >
              <option value="7d">Last 7 days</option>
              <option value="30d">Last 30 days</option>
              <option value="90d">Last 90 days</option>
              <option value="1y">Last year</option>
            </select>
            <button
              onClick={() => generateReportMutation.mutate()}
              className="btn btn-secondary"
              disabled={generateReportMutation.isPending || selectedProject === 'all'}
            >
              <Download size={16} className="mr-1" />
              Generate Report
            </button>
            <button
              onClick={() => setShowRules(!showRules)}
              className={`btn ${showRules ? 'btn-primary' : 'btn-secondary'}`}
            >
              <Settings size={16} className="mr-1" />
              Rules
            </button>
          </div>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <StatsCard
            title="Average Coverage"
            value={`${totalCoverage.toFixed(1)}%`}
            subtitle="across all projects"
            icon={<Target size={20} />}
            color={totalCoverage >= 80 ? 'green' : totalCoverage >= 50 ? 'yellow' : 'red'}
            trend={totalCoverage >= 80 ? 'up' : totalCoverage >= 50 ? 'stable' : 'down'}
          />
          <StatsCard
            title="Requirements Covered"
            value={`${coveredRequirements}/${totalRequirements}`}
            subtitle="total requirements"
            icon={<CheckCircle size={20} />}
            color="blue"
          />
          <StatsCard
            title="Critical Alerts"
            value={criticalAlerts}
            subtitle="requires attention"
            icon={<AlertTriangle size={20} />}
            color={criticalAlerts > 0 ? 'red' : 'green'}
          />
          <StatsCard
            title="Warnings"
            value={warningsCount}
            subtitle="below threshold"
            icon={<Bell size={20} />}
            color={warningsCount > 0 ? 'yellow' : 'green'}
          />
        </div>

        {/* Alerts Panel */}
        {alerts.length > 0 && (
          <div className="bg-white rounded-lg border mb-6">
            <div className="p-4 border-b flex items-center justify-between">
              <h2 className="font-semibold">Coverage Alerts</h2>
              <span className="text-sm text-gray-500">{alerts.length} alerts</span>
            </div>
            <div className="divide-y">
              {(Array.isArray(alerts) ? alerts : []).slice(0, 5).map((alert) => (
                <div
                  key={alert.id}
                  className={`p-4 flex items-center justify-between ${alert.isRead ? 'bg-gray-50' : 'bg-white'}`}
                >
                  <div className="flex items-center gap-3">
                    <AlertBadge type={alert.type} />
                    <span className="text-sm">{alert.message}</span>
                    {alert.requirementKey && (
                      <span className="text-xs bg-gray-100 px-2 py-1 rounded">{alert.requirementKey}</span>
                    )}
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-gray-400">
                      {new Date(alert.createdAt).toLocaleDateString()}
                    </span>
                    {!alert.isRead && (
                      <button
                        onClick={() => markAlertMutation.mutate(alert.id)}
                        className="text-xs text-blue-600 hover:underline"
                      >
                        Mark as read
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Main Content */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Project Coverage List */}
          <div className="lg:col-span-2 bg-white rounded-lg border">
            <div className="p-4 border-b flex items-center justify-between">
              <h2 className="font-semibold">Project Coverage</h2>
              <div className="flex gap-2">
                <select
                  value={selectedProject}
                  onChange={(e) => setSelectedProject(e.target.value)}
                  className="px-3 py-1 border rounded text-sm"
                >
                  <option value="all">All Projects</option>
                  {projectCoverage.map((project) => (
                    <option key={project.projectId} value={project.projectId}>
                      {project.projectName}
                    </option>
                  ))}
                </select>
                <select
                  value={filterStatus}
                  onChange={(e) => setFilterStatus(e.target.value as any)}
                  className="px-3 py-1 border rounded text-sm"
                >
                  <option value="ALL">All Status</option>
                  <option value="COVERED">Covered</option>
                  <option value="PARTIAL">Partial</option>
                  <option value="UNCOVERED">Uncovered</option>
                </select>
              </div>
            </div>

            {selectedProject === 'all' ? (
              /* Project Cards */
              <div className="p-4 grid grid-cols-1 md:grid-cols-2 gap-4">
                {projectLoading ? (
                  <div className="text-center py-8 text-gray-500">Loading...</div>
                ) : projectCoverage.length === 0 ? (
                  <div className="text-center py-8 text-gray-500">No projects found</div>
                ) : (
                  projectCoverage.map((project) => (
                    <div
                      key={project.projectId}
                      onClick={() => setSelectedProject(project.projectId)}
                      className="p-4 border rounded-lg hover:bg-gray-50 cursor-pointer"
                    >
                      <div className="flex items-center justify-between mb-3">
                        <h3 className="font-medium">{project.projectName}</h3>
                        <span className={`text-sm ${project.trend === 'up' ? 'text-green-600' : project.trend === 'down' ? 'text-red-600' : 'text-gray-500'}`}>
                          {project.trend === 'up' ? '↑' : project.trend === 'down' ? '↓' : '→'}
                        </span>
                      </div>
                      <CoverageBar percent={project.coveragePercent} />
                      <div className="flex items-center justify-between mt-2 text-xs text-gray-500">
                        <span>{project.coveredRequirements}/{project.totalRequirements} requirements</span>
                        <span>{project.lastUpdated}</span>
                      </div>
                    </div>
                  ))
                )}
              </div>
            ) : (
              /* Requirements Table */
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-gray-50 border-b">
                    <tr>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500"></th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Requirement</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 w-48">Coverage</th>
                      <th className="px-4 py-3 text-center text-xs font-medium text-gray-500">Tests</th>
                      <th className="px-4 py-3 text-center text-xs font-medium text-gray-500">%</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500">Last Run</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {requirementsLoading ? (
                      <tr>
                        <td colSpan={6} className="px-4 py-8 text-center text-gray-500">Loading...</td>
                      </tr>
                    ) : filteredRequirements.length === 0 ? (
                      <tr>
                        <td colSpan={6} className="px-4 py-8 text-center text-gray-500">No requirements found</td>
                      </tr>
                    ) : (
                      filteredRequirements.map((req: any) => (
                        <RequirementRow
                          key={req.requirementKey}
                          requirementKey={req.requirementKey}
                          requirementName={req.requirementName || req.requirementKey}
                          coverage={req.coveragePercent || 0}
                          coveredTests={req.coveredTests || 0}
                          totalTests={req.totalTests || 0}
                          lastExecuted={req.lastExecutedAt}
                          onExpand={() => toggleExpansion(req.requirementKey)}
                          isExpanded={expandedRequirements.has(req.requirementKey)}
                        />
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Sidebar */}
          <div className="space-y-4">
            {/* Coverage Trend */}
            {selectedProject !== 'all' && coverageTrend.length > 0 && (
              <div className="bg-white rounded-lg border p-4">
                <h3 className="font-semibold mb-4">Coverage Trend</h3>
                <div className="h-40 flex items-end gap-1">
                  {(Array.isArray(coverageTrend) ? coverageTrend : []).slice(-14).map((point: any, index) => (
                    <div
                      key={index}
                      className="flex-1 bg-blue-500 rounded-t"
                      style={{ height: `${point.coverage}%` }}
                      title={`${point.date}: ${point.coverage}%`}
                    />
                  ))}
                </div>
                <div className="flex justify-between text-xs text-gray-500 mt-2">
                  <span>14 days ago</span>
                  <span>Today</span>
                </div>
              </div>
            )}

            {/* Drift Records */}
            {selectedProject !== 'all' && driftRecords.length > 0 && (
              <div className="bg-white rounded-lg border">
                <div className="p-4 border-b">
                  <h3 className="font-semibold">Coverage Drift</h3>
                </div>
                <div className="divide-y max-h-[300px] overflow-y-auto">
                  {(Array.isArray(driftRecords) ? driftRecords : []).slice(0, 5).map((drift) => (
                    <div key={drift.requirementKey} className="p-4">
                      <div className="flex items-center justify-between">
                        <span className="font-medium text-sm">{drift.requirementKey}</span>
                        <span className={`text-sm ${drift.driftPercent > 0 ? 'text-green-600' : 'text-red-600'}`}>
                          {drift.driftPercent > 0 ? '+' : ''}{drift.driftPercent.toFixed(1)}%
                        </span>
                      </div>
                      <div className="flex items-center gap-4 mt-1 text-xs text-gray-500">
                        <span>Before: {drift.previousCoverage.toFixed(1)}%</span>
                        <span>Now: {drift.currentCoverage.toFixed(1)}%</span>
                      </div>
                      <p className="text-xs text-gray-400 mt-1">
                        {drift.affectedTests.length} tests affected
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Rules Panel */}
            {showRules && (
              <div className="bg-white rounded-lg border">
                <div className="p-4 border-b flex items-center justify-between">
                  <h3 className="font-semibold">Coverage Rules</h3>
                  <button
                    onClick={() => setShowCreateRule(true)}
                    className="btn btn-sm btn-primary"
                  >
                    Add Rule
                  </button>
                </div>
                <div className="divide-y max-h-[300px] overflow-y-auto">
                  {rules.length === 0 ? (
                    <div className="p-4 text-center text-gray-500">No rules configured</div>
                  ) : (
                    rules.map((rule) => (
                      <div key={rule.id} className="p-4 flex items-center justify-between">
                        <div>
                          <p className="font-medium text-sm">{rule.name}</p>
                          <p className="text-xs text-gray-500">
                            Target: {rule.targetPercent}% | Type: {rule.type}
                          </p>
                        </div>
                        <button
                          onClick={() => toggleRuleMutation.mutate({ ruleId: rule.id, active: !rule.isActive })}
                          className={`btn btn-sm ${rule.isActive ? 'btn-primary' : 'btn-secondary'}`}
                        >
                          {rule.isActive ? 'Active' : 'Inactive'}
                        </button>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Create Rule Modal */}
      <CreateRuleModal
        open={showCreateRule}
        onClose={() => setShowCreateRule(false)}
        onSuccess={() => {
          queryClient.invalidateQueries({ queryKey: ['coverage-rules'] });
        }}
      />
    </div>
  );
};

export default CoveragePage;