import React, { useState, useMemo } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import advancedApi, {
  FlakyTestResponse,
  FlakyPatternResponse,
  FlakyDashboardResponse,
  ExecutionRecordResponse
} from '../../../api/testApi';
import {
  AlertTriangle, TrendingUp, TrendingDown, Minus, Search, Filter,
  MoreVertical, Eye, Pause, Play, X, ChevronDown, ChevronRight,
  RefreshCw, ShieldAlert, Activity, Clock, AlertCircle
} from 'lucide-react';

// Confirmation Dialog
const ConfirmDialog: React.FC<{
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  variant?: 'default' | 'danger';
}> = ({ open, title, message, confirmLabel = 'Confirm', onConfirm, onCancel, variant = 'default' }) => {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onCancel}></div>
        <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h3 className="text-lg font-semibold mb-2">{title}</h3>
          <p className="text-gray-600 mb-6">{message}</p>
          <div className="flex justify-end gap-3">
            <button onClick={onCancel} className="btn btn-secondary">Cancel</button>
            <button onClick={onConfirm} className={`btn ${variant === 'danger' ? 'bg-red-600 hover:bg-red-700 text-white' : 'btn-primary'}`}>
              {confirmLabel}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// Stats Card Component
interface StatsCardProps {
  title: string;
  value: number | string;
  subtitle?: string;
  trend?: 'up' | 'down' | 'stable';
  trendValue?: string;
  icon: React.ReactNode;
  color?: string;
}

const StatsCard: React.FC<StatsCardProps> = ({ title, value, subtitle, trend, trendValue, icon, color = 'blue' }) => {
  const colorClasses: Record<string, { bg: string; text: string; icon: string }> = {
    blue: { bg: 'bg-blue-50', text: 'text-blue-600', icon: 'text-blue-500' },
    red: { bg: 'bg-red-50', text: 'text-red-600', icon: 'text-red-500' },
    green: { bg: 'bg-green-50', text: 'text-green-600', icon: 'text-green-500' },
    yellow: { bg: 'bg-yellow-50', text: 'text-yellow-600', icon: 'text-yellow-500' },
    purple: { bg: 'bg-purple-50', text: 'text-purple-600', icon: 'text-purple-500' },
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
      {trend && trendValue && (
        <div className={`flex items-center gap-1 mt-2 text-xs ${
          trend === 'up' ? 'text-green-600' : trend === 'down' ? 'text-red-600' : 'text-gray-600'
        }`}>
          {trend === 'up' ? <TrendingUp className="w-3 h-3" /> :
           trend === 'down' ? <TrendingDown className="w-3 h-3" /> :
           <Minus className="w-3 h-3" />}
          <span>{trendValue}</span>
        </div>
      )}
    </div>
  );
};

// Flaky Test Detail Panel
interface FlakyTestDetailPanelProps {
  test: FlakyTestResponse;
  onClose: () => void;
  onQuarantine: () => void;
}

const FlakyTestDetailPanel: React.FC<FlakyTestDetailPanelProps> = ({ test, onClose, onQuarantine }) => {
  const { data: patterns } = useQuery({
    queryKey: ['flaky-test-patterns', test.testId],
    queryFn: async () => test.patterns || [],
  });

  return (
    <div className="w-[480px] border-l border-gray-200 bg-white flex flex-col">
      <div className="flex items-center justify-between p-4 border-b">
        <div>
          <h3 className="font-semibold">Flaky Test Details</h3>
          <p className="text-sm text-gray-500">{test.testIssueKey || test.testId}</p>
        </div>
        <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
          <X className="w-5 h-5" />
        </button>
      </div>
      <div className="flex-1 overflow-auto p-4">
        {/* Test Info */}
        <div className="mb-4">
          <h4 className="text-lg font-medium">{test.testName}</h4>
          <div className="flex items-center gap-2 mt-2">
            <span className={`px-2 py-0.5 rounded text-xs font-medium ${
              test.currentStatus === 'stable' ? 'bg-green-100 text-green-800' :
              test.currentStatus === 'flaky' ? 'bg-yellow-100 text-yellow-800' :
              'bg-red-100 text-red-800'
            }`}>
              {test.currentStatus?.replace('_', ' ').toUpperCase() || 'UNKNOWN'}
            </span>
            <span className="text-sm text-gray-500">Flaky Score: {test.flakyScore}%</span>
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-3 mb-4">
          <div className="p-3 bg-gray-50 rounded text-center">
            <p className="text-2xl font-bold text-gray-900">{test.totalExecutions}</p>
            <p className="text-xs text-gray-500">Total Runs</p>
          </div>
          <div className="p-3 bg-green-50 rounded text-center">
            <p className="text-2xl font-bold text-green-600">{test.totalPasses}</p>
            <p className="text-xs text-gray-500">Passed</p>
          </div>
          <div className="p-3 bg-red-50 rounded text-center">
            <p className="text-2xl font-bold text-red-600">{test.totalFailures}</p>
            <p className="text-xs text-gray-500">Failed</p>
          </div>
        </div>

        {/* Trend */}
        <div className="mb-4 p-3 bg-gray-50 rounded">
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-600">Pass Rate Trend</span>
            <span className={`flex items-center gap-1 text-xs ${
              test.passRateTrend === 'improving' ? 'text-green-600' :
              test.passRateTrend === 'degrading' ? 'text-red-600' :
              'text-gray-600'
            }`}>
              {test.passRateTrend === 'improving' ? <TrendingUp className="w-3 h-3" /> :
               test.passRateTrend === 'degrading' ? <TrendingDown className="w-3 h-3" /> :
               <Minus className="w-3 h-3" />}
              {test.passRateTrend || 'stable'}
            </span>
          </div>
          <div className="mt-2 h-2 bg-gray-200 rounded-full overflow-hidden">
            <div
              className="h-full bg-blue-500"
              style={{ width: `${test.flakyScore}%` }}
            />
          </div>
        </div>

        {/* Execution History */}
        <div className="mb-4">
          <h5 className="text-sm font-medium mb-2">Recent Execution Pattern</h5>
          <div className="flex gap-1 flex-wrap">
            {test.recentExecutions?.slice(0, 20).map((exec, i) => (
              <div
                key={i}
                className={`w-6 h-6 rounded text-xs flex items-center justify-center text-white ${
                  exec.isFlakyExecution ? 'bg-yellow-500' :
                  exec.lastStatus === 'PASSED' ? 'bg-green-500' :
                  'bg-red-500'
                }`}
                title={`${exec.analyzedAt}: ${exec.lastStatus || 'UNKNOWN'}`}
              >
                {exec.lastStatus === 'PASSED' ? 'P' : 'F'}
              </div>
            ))}
          </div>
        </div>

        {/* Patterns */}
        {patterns && patterns.length > 0 && (
          <div className="mb-4">
            <h5 className="text-sm font-medium mb-2">Detected Patterns</h5>
            <div className="space-y-2">
              {patterns.map((pattern) => (
                <div key={pattern.id} className="p-3 bg-gray-50 rounded">
                  <div className="flex items-center justify-between">
                    <span className="font-medium text-sm">{pattern.patternType}</span>
                    <span className="text-xs text-gray-500">{pattern.rootCauseCategory}</span>
                  </div>
                  {pattern.patternDescription && (
                    <p className="text-sm text-gray-600 mt-1">{pattern.patternDescription}</p>
                  )}
                  {pattern.suggestedFix && (
                    <p className="text-sm text-blue-600 mt-1">Fix: {pattern.suggestedFix}</p>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Dates */}
        <div className="text-xs text-gray-500 space-y-1">
          <p>First Flaky: {test.firstFlakyOccurrence ? new Date(test.firstFlakyOccurrence).toLocaleString() : 'N/A'}</p>
          <p>Last Flaky: {test.lastFlakyOccurrence ? new Date(test.lastFlakyOccurrence).toLocaleString() : 'N/A'}</p>
        </div>

        {/* Actions */}
        <div className="mt-4 pt-4 border-t">
          <button
            onClick={onQuarantine}
            className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-yellow-500 hover:bg-yellow-600 text-white rounded-lg"
          >
            <ShieldAlert className="w-4 h-4" />
            Quarantine Test
          </button>
        </div>
      </div>
    </div>
  );
};

// Main Page Component
export const FlakyTestsPage: React.FC<{ projectId?: string }> = ({ projectId: propProjectId }) => {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [filterSeverity, setFilterSeverity] = useState<string>('');
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [expandedTestId, setExpandedTestId] = useState<string | null>(null);
  const [selectedTest, setSelectedTest] = useState<FlakyTestResponse | null>(null);
  const [quarantineConfirm, setQuarantineConfirm] = useState<{ open: boolean; testId: string | null }>({ open: false, testId: null });
  const [showDetailPanel, setShowDetailPanel] = useState(false);

  // Fetch dashboard data
  const { data: dashboard, isLoading: dashboardLoading } = useQuery({
    queryKey: ['flaky-dashboard', propProjectId],
    queryFn: () => advancedApi.flakyTests.getDashboard(propProjectId || ''),
    enabled: !!propProjectId,
  });

  // Fetch all flaky tests
  const { data: flakyTests = [], isLoading, refetch } = useQuery({
    queryKey: ['flaky-tests'],
    queryFn: () => advancedApi.flakyTests.getAll(100),
  });

  // Quarantine mutation
  const quarantineMutation = useMutation({
    mutationFn: (testId: string) => advancedApi.quarantine.quarantine({
      testId,
      status: 'quarantined',
      quarantineReason: 'Flaky test detected - auto-quarantine by system',
      triggerType: 'auto_flaky',
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['flaky-tests'] });
      queryClient.invalidateQueries({ queryKey: ['quarantine'] });
      setQuarantineConfirm({ open: false, testId: null });
    },
  });

  const filteredTests = useMemo(() => {
    return flakyTests
      .filter(test => {
        const matchesSearch = !searchQuery ||
          test.testName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          test.testIssueKey?.toLowerCase().includes(searchQuery.toLowerCase());
        const matchesStatus = !filterStatus || test.currentStatus === filterStatus;
        return matchesSearch && matchesStatus;
      })
      .sort((a, b) => b.flakyScore - a.flakyScore);
  }, [flakyTests, searchQuery, filterStatus]);

  const handleQuarantine = () => {
    if (quarantineConfirm.testId) {
      quarantineMutation.mutate(quarantineConfirm.testId);
    }
  };

  const handleViewDetails = (test: FlakyTestResponse) => {
    setSelectedTest(test);
    setShowDetailPanel(true);
  };

  const getFlakyScoreColor = (score: number) => {
    if (score >= 70) return 'text-red-600 bg-red-100';
    if (score >= 40) return 'text-yellow-600 bg-yellow-100';
    return 'text-blue-600 bg-blue-100';
  };

  const getStatusIcon = (status?: string) => {
    switch (status) {
      case 'stable': return <span className="w-2 h-2 rounded-full bg-green-500"></span>;
      case 'flaky': return <AlertTriangle className="w-4 h-4 text-yellow-500" />;
      case 'quarantine_candidate': return <ShieldAlert className="w-4 h-4 text-red-500" />;
      default: return <span className="w-2 h-2 rounded-full bg-gray-400"></span>;
    }
  };

  return (
    <div className="h-full flex">
      {/* Main Content */}
      <div className="flex-1 flex flex-col bg-gray-50">
        {/* Header */}
        <div className="bg-white px-6 py-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Flaky Tests</h1>
              <p className="text-sm text-gray-500 mt-1">Identify and manage unstable test executions</p>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={() => refetch()}
                className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                <RefreshCw className="w-4 h-4" />
                Refresh
              </button>
            </div>
          </div>
        </div>

        {/* Stats Cards */}
        {dashboard && (
          <div className="px-6 py-4 grid grid-cols-4 gap-4">
            <StatsCard
              title="Total Analyzed"
              value={dashboard.totalTestsAnalyzed}
              subtitle="Tests monitored"
              icon={<Activity className="w-5 h-5" />}
              color="blue"
            />
            <StatsCard
              title="Stable"
              value={dashboard.stableCount}
              subtitle={`${dashboard.totalTestsAnalyzed > 0 ? Math.round(dashboard.stableCount / dashboard.totalTestsAnalyzed * 100) : 0}%`}
              trend="stable"
              icon={<span className="w-5 h-5 rounded-full bg-green-500"></span>}
              color="green"
            />
            <StatsCard
              title="Flaky"
              value={dashboard.flakyCount}
              subtitle={`${dashboard.totalTestsAnalyzed > 0 ? Math.round(dashboard.flakyCount / dashboard.totalTestsAnalyzed * 100) : 0}%`}
              icon={<AlertTriangle className="w-5 h-5" />}
              color="yellow"
            />
            <StatsCard
              title="Quarantine Candidates"
              value={dashboard.quarantineCandidateCount}
              subtitle="Need attention"
              icon={<ShieldAlert className="w-5 h-5" />}
              color="red"
            />
          </div>
        )}

        {/* Filters */}
        <div className="px-6 pb-4 flex items-center gap-4">
          <div className="flex-1 relative max-w-md">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search flaky tests..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">All Statuses</option>
            <option value="stable">Stable</option>
            <option value="flaky">Flaky</option>
            <option value="quarantine_candidate">Quarantine Candidate</option>
          </select>
        </div>

        {/* Table */}
        <div className="flex-1 overflow-auto px-6 pb-6">
          {isLoading || dashboardLoading ? (
            <div className="flex items-center justify-center h-64">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
            </div>
          ) : filteredTests.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-64 text-center bg-white rounded-lg border">
              <Activity className="w-12 h-12 text-gray-400 mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No flaky tests detected</h3>
              <p className="text-gray-500">All your tests are running consistently</p>
            </div>
          ) : (
            <div className="bg-white rounded-lg border overflow-hidden">
              <table className="w-full">
                <thead>
                  <tr className="text-left text-gray-500 bg-gray-50 border-b">
                    <th className="p-4 w-10"></th>
                    <th className="p-4">Test</th>
                    <th className="p-4">Flaky Score</th>
                    <th className="p-4">Status</th>
                    <th className="p-4">Executions</th>
                    <th className="p-4">Pass Rate</th>
                    <th className="p-4">Trend</th>
                    <th className="p-4">Last Failure</th>
                    <th className="p-4 w-12"></th>
                  </tr>
                </thead>
                <tbody>
                  {filteredTests.map((test) => (
                    <React.Fragment key={test.testId}>
                      <tr
                        className="border-b hover:bg-gray-50 cursor-pointer"
                        onClick={() => setExpandedTestId(expandedTestId === test.testId ? null : test.testId)}
                      >
                        <td className="p-4">
                          {expandedTestId === test.testId ? (
                            <ChevronDown className="w-4 h-4 text-gray-400" />
                          ) : (
                            <ChevronRight className="w-4 h-4 text-gray-400" />
                          )}
                        </td>
                        <td className="p-4">
                          <div>
                            <p className="font-medium text-gray-900">{test.testName}</p>
                            {test.testIssueKey && (
                              <p className="text-sm text-gray-500">{test.testIssueKey}</p>
                            )}
                          </div>
                        </td>
                        <td className="p-4">
                          <span className={`px-3 py-1 rounded-full text-sm font-medium ${getFlakyScoreColor(test.flakyScore)}`}>
                            {test.flakyScore}%
                          </span>
                        </td>
                        <td className="p-4">
                          <div className="flex items-center gap-2">
                            {getStatusIcon(test.currentStatus)}
                            <span className="text-sm capitalize">
                              {test.currentStatus?.replace('_', ' ') || 'Unknown'}
                            </span>
                          </div>
                        </td>
                        <td className="p-4 text-gray-600">{test.totalExecutions}</td>
                        <td className="p-4">
                          <div className="flex items-center gap-2">
                            <div className="w-16 h-2 bg-gray-200 rounded-full overflow-hidden">
                              <div
                                className="h-full bg-green-500"
                                style={{ width: `${Math.round((test.totalPasses / test.totalExecutions) * 100)}%` }}
                              />
                            </div>
                            <span className="text-sm">{Math.round((test.totalPasses / test.totalExecutions) * 100)}%</span>
                          </div>
                        </td>
                        <td className="p-4">
                          <span className={`flex items-center gap-1 text-xs ${
                            test.passRateTrend === 'improving' ? 'text-green-600' :
                            test.passRateTrend === 'degrading' ? 'text-red-600' :
                            'text-gray-600'
                          }`}>
                            {test.passRateTrend === 'improving' ? <TrendingUp className="w-3 h-3" /> :
                             test.passRateTrend === 'degrading' ? <TrendingDown className="w-3 h-3" /> :
                             <Minus className="w-3 h-3" />}
                            {test.passRateTrend || 'stable'}
                          </span>
                        </td>
                        <td className="p-4 text-gray-500 text-sm">
                          {test.lastFlakyOccurrence ? new Date(test.lastFlakyOccurrence).toLocaleDateString() : 'N/A'}
                        </td>
                        <td className="p-4" onClick={(e) => e.stopPropagation()}>
                          <FlakyTestActionsMenu
                            test={test}
                            onView={() => handleViewDetails(test)}
                            onQuarantine={() => setQuarantineConfirm({ open: true, testId: test.testId })}
                          />
                        </td>
                      </tr>
                      {expandedTestId === test.testId && test.recentExecutions && (
                        <tr>
                          <td colSpan={9} className="bg-gray-50 p-4">
                            <div className="mb-2">
                              <h5 className="text-sm font-medium">Execution Pattern (Last 20 runs)</h5>
                              <div className="flex gap-1 mt-2">
                                {test.recentExecutions.slice(0, 20).map((exec, i) => (
                                  <div
                                    key={i}
                                    className={`w-8 h-8 rounded flex items-center justify-center text-xs text-white ${
                                      exec.isFlakyExecution ? 'bg-yellow-500' :
                                      exec.lastStatus === 'PASSED' ? 'bg-green-500' :
                                      'bg-red-500'
                                    }`}
                                    title={new Date(exec.analyzedAt).toLocaleString()}
                                  >
                                    {exec.lastStatus === 'PASSED' ? 'P' : 'F'}
                                  </div>
                                ))}
                              </div>
                            </div>
                            {test.patterns && test.patterns.length > 0 && (
                              <div>
                                <h5 className="text-sm font-medium">Detected Patterns</h5>
                                <div className="flex gap-2 mt-2 flex-wrap">
                                  {test.patterns.map((pattern) => (
                                    <span key={pattern.id} className="px-2 py-1 bg-orange-100 text-orange-800 rounded text-xs">
                                      {pattern.patternType}
                                    </span>
                                  ))}
                                </div>
                              </div>
                            )}
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Detail Panel */}
      {showDetailPanel && selectedTest && (
        <FlakyTestDetailPanel
          test={selectedTest}
          onClose={() => setShowDetailPanel(false)}
          onQuarantine={() => {
            setQuarantineConfirm({ open: true, testId: selectedTest.testId });
            setShowDetailPanel(false);
          }}
        />
      )}

      {/* Quarantine Confirmation */}
      <ConfirmDialog
        open={quarantineConfirm.open}
        title="Quarantine Test"
        message="This test will be moved to quarantine and excluded from test execution cycles. You can restore it later from the Quarantine Management page."
        confirmLabel="Quarantine"
        onConfirm={handleQuarantine}
        onCancel={() => setQuarantineConfirm({ open: false, testId: null })}
        variant="danger"
      />
    </div>
  );
};

// Flaky Test Actions Menu
const FlakyTestActionsMenu: React.FC<{
  test: FlakyTestResponse;
  onView: () => void;
  onQuarantine: () => void;
}> = ({ onView, onQuarantine }) => {
  const [open, setOpen] = useState(false);

  return (
    <div className="relative">
      <button onClick={() => setOpen(!open)} className="p-1 hover:bg-gray-200 rounded">
        <MoreVertical className="w-4 h-4 text-gray-400" />
      </button>
      {open && (
        <div className="absolute right-0 top-8 bg-white border border-gray-200 rounded-lg shadow-lg z-10 min-w-[160px]">
          <button onClick={() => { onView(); setOpen(false); }} className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 flex items-center gap-2">
            <Eye className="w-4 h-4" /> View Details
          </button>
          <div className="border-t border-gray-200 my-1" />
          <button onClick={() => { onQuarantine(); setOpen(false); }} className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 text-yellow-600 flex items-center gap-2">
            <ShieldAlert className="w-4 h-4" /> Quarantine
          </button>
        </div>
      )}
    </div>
  );
};

export default FlakyTestsPage;