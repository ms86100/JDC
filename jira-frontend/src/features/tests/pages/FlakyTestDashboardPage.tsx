import React, { useState, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import advancedApi, {
  FlakyTestResponse,
  FlakyDashboardResponse,
} from '../../../api/testApi';
import {
  Activity,
  AlertTriangle,
  TrendingUp,
  TrendingDown,
  Minus,
  RefreshCw,
  Search,
  Filter,
  Eye,
  ShieldAlert,
  Brain,
  Clock,
  GitBranch,
  Zap,
  BarChart3,
  PieChart,
  ArrowUpRight,
  ArrowDownRight,
  Target,
  Lightbulb,
  ChevronDown,
  ChevronRight,
  X,
  Loader2,
} from 'lucide-react';

// Stats Card Component
interface StatsCardProps {
  title: string;
  value: number | string;
  subtitle?: string;
  trend?: 'up' | 'down' | 'stable';
  trendValue?: string;
  icon: React.ReactNode;
  color?: string;
  onClick?: () => void;
}

const StatsCard: React.FC<StatsCardProps> = ({
  title,
  value,
  subtitle,
  trend,
  trendValue,
  icon,
  color = 'blue',
  onClick,
}) => {
  const colorClasses: Record<string, { bg: string; text: string; icon: string; gradient: string }> = {
    blue: { bg: 'bg-blue-50', text: 'text-blue-600', icon: 'text-blue-500', gradient: 'from-blue-500 to-blue-600' },
    red: { bg: 'bg-red-50', text: 'text-red-600', icon: 'text-red-500', gradient: 'from-red-500 to-red-600' },
    green: { bg: 'bg-green-50', text: 'text-green-600', icon: 'text-green-500', gradient: 'from-green-500 to-green-600' },
    yellow: { bg: 'bg-yellow-50', text: 'text-yellow-600', icon: 'text-yellow-500', gradient: 'from-yellow-500 to-yellow-600' },
    purple: { bg: 'bg-purple-50', text: 'text-purple-600', icon: 'text-purple-500', gradient: 'from-purple-500 to-purple-600' },
    indigo: { bg: 'bg-indigo-50', text: 'text-indigo-600', icon: 'text-indigo-500', gradient: 'from-indigo-500 to-indigo-600' },
  };

  const colors = colorClasses[color] || colorClasses.blue;

  return (
    <div
      className={`bg-white rounded-xl border border-gray-200 p-5 hover:shadow-lg transition-all cursor-pointer ${
        onClick ? 'hover:border-blue-300' : ''
      }`}
      onClick={onClick}
    >
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="text-sm font-medium text-gray-500">{title}</p>
          <p className="text-3xl font-bold mt-2 bg-gradient-to-r bg-clip-text text-transparent bg-gradient-to-r {colors.gradient}">
            {value}
          </p>
          {subtitle && <p className="text-xs text-gray-400 mt-1">{subtitle}</p>}
        </div>
        <div className={`${colors.bg} p-3 rounded-xl`}>
          <div className={colors.icon}>{icon}</div>
        </div>
      </div>
      {trend && trendValue && (
        <div
          className={`flex items-center gap-1 mt-3 text-xs font-medium ${
            trend === 'up' ? 'text-red-600' : trend === 'down' ? 'text-green-600' : 'text-gray-500'
          }`}
        >
          {trend === 'up' ? (
            <ArrowUpRight className="w-3 h-3" />
          ) : trend === 'down' ? (
            <ArrowDownRight className="w-3 h-3" />
          ) : (
            <Minus className="w-3 h-3" />
          )}
          <span>{trendValue}</span>
          <span className="text-gray-400 ml-1">vs last week</span>
        </div>
      )}
    </div>
  );
};

// Trend Chart Component
const TrendChart: React.FC<{ data: { date: string; value: number }[]; title: string }> = ({
  data,
  title,
}) => {
  const maxValue = Math.max(...data.map((d) => d.value), 1);

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-5">
      <h3 className="text-sm font-semibold text-gray-700 mb-4">{title}</h3>
      <div className="h-40 flex items-end gap-1">
        {data.map((item, index) => (
          <div key={index} className="flex-1 flex flex-col items-center">
            <div
              className="w-full bg-gradient-to-t from-blue-500 to-blue-300 rounded-t transition-all hover:from-blue-600 hover:to-blue-400"
              style={{ height: `${(item.value / maxValue) * 100}%` }}
              title={`${item.date}: ${item.value}`}
            />
            <span className="text-xs text-gray-400 mt-1 rotate-0">
              {item.date.slice(5)}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};

// Pattern Distribution Pie Chart
const PatternPieChart: React.FC<{
  data: Record<string, number>;
  title: string;
}> = ({ data, title }) => {
  const colors = ['bg-blue-500', 'bg-green-500', 'bg-yellow-500', 'bg-red-500', 'bg-purple-500'];
  const total = Object.values(data).reduce((a, b) => a + b, 0);

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-5">
      <h3 className="text-sm font-semibold text-gray-700 mb-4">{title}</h3>
      <div className="flex items-center gap-6">
        <div className="relative w-32 h-32">
          <svg className="w-full h-full transform -rotate-90" viewBox="0 0 100 100">
            {Object.entries(data).map(([key, value], index) => {
              const percentage = (value / total) * 100;
              const offset = Object.entries(data)
                .slice(0, index)
                .reduce((acc, [, v]) => acc + (v / total) * 360, 0);
              return (
                <circle
                  key={key}
                  cx="50"
                  cy="50"
                  r="40"
                  fill="none"
                  stroke={colors[index % colors.length].replace('bg-', 'rgb(').replace('-500', ')').replace('blue', '59, 130, 246').replace('green', '34, 197, 94').replace('yellow', '234, 179, 8').replace('red', '239, 68, 68').replace('purple', '168, 85, 247')}
                  strokeWidth="20"
                  strokeDasharray={`${percentage * 2.51} 251`}
                  strokeDashoffset={`-${offset * 2.51}`}
                  className="transition-all"
                />
              );
            })}
          </svg>
        </div>
        <div className="flex-1 space-y-2">
          {Object.entries(data).map(([key, value], index) => (
            <div key={key} className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className={`w-3 h-3 rounded ${colors[index % colors.length]}`} />
                <span className="text-sm text-gray-600 capitalize">{key.replace('-', ' ')}</span>
              </div>
              <span className="text-sm font-medium">{value}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

// Root Cause Analysis Card
interface RootCauseCardProps {
  test: FlakyTestResponse;
  onViewDetails: () => void;
}

const RootCauseCard: React.FC<RootCauseCardProps> = ({ test, onViewDetails }) => {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
      <div
        className="p-4 flex items-center justify-between cursor-pointer hover:bg-gray-50"
        onClick={() => setExpanded(!expanded)}
      >
        <div className="flex items-center gap-4">
          <div className={`p-2 rounded-lg ${
            test.flakyScore >= 70 ? 'bg-red-100' : test.flakyScore >= 40 ? 'bg-yellow-100' : 'bg-blue-100'
          }`}>
            {test.flakyScore >= 70 ? (
              <AlertTriangle className="w-5 h-5 text-red-600" />
            ) : test.flakyScore >= 40 ? (
              <AlertTriangle className="w-5 h-5 text-yellow-600" />
            ) : (
              <Activity className="w-5 h-5 text-blue-600" />
            )}
          </div>
          <div>
            <h4 className="font-medium text-gray-900">{test.testName}</h4>
            <p className="text-sm text-gray-500">{test.testIssueKey}</p>
          </div>
        </div>
        <div className="flex items-center gap-4">
          <div className="text-right">
            <div className="text-lg font-bold">{test.flakyScore}%</div>
            <div className="text-xs text-gray-500">Flaky Score</div>
          </div>
          {expanded ? (
            <ChevronDown className="w-5 h-5 text-gray-400" />
          ) : (
            <ChevronRight className="w-5 h-5 text-gray-400" />
          )}
        </div>
      </div>

      {expanded && (
        <div className="px-4 pb-4 border-t border-gray-100">
          <div className="pt-4 space-y-4">
            {/* Execution History */}
            <div>
              <h5 className="text-xs font-semibold text-gray-500 uppercase mb-2">Recent Execution Pattern</h5>
              <div className="flex gap-1 flex-wrap">
                {test.recentExecutions?.slice(0, 30).map((exec, i) => (
                  <div
                    key={i}
                    className={`w-6 h-6 rounded text-xs flex items-center justify-center text-white ${
                      exec.isFlakyExecution ? 'bg-yellow-500' : 'bg-green-500'
                    }`}
                    title={new Date(exec.analyzedAt).toLocaleDateString()}
                  >
                    {exec.isFlakyExecution ? 'F' : 'P'}
                  </div>
                ))}
              </div>
            </div>

            {/* Detected Patterns */}
            {test.patterns && test.patterns.length > 0 && (
              <div>
                <h5 className="text-xs font-semibold text-gray-500 uppercase mb-2">Detected Patterns</h5>
                <div className="space-y-2">
                  {test.patterns.map((pattern) => (
                    <div key={pattern.id} className="p-3 bg-gray-50 rounded-lg">
                      <div className="flex items-center justify-between">
                        <span className="font-medium text-sm capitalize">{pattern.patternType.replace('-', ' ')}</span>
                        <span className="text-xs text-gray-500">
                          {Number(pattern.frequencyScore) * 100}% frequency
                        </span>
                      </div>
                      {pattern.rootCauseCategory && (
                        <p className="text-xs text-gray-600 mt-1">{pattern.rootCauseCategory}</p>
                      )}
                      {pattern.suggestedFix && (
                        <div className="flex items-start gap-2 mt-2 text-xs text-blue-600">
                          <Lightbulb className="w-3 h-3 mt-0.5 flex-shrink-0" />
                          <span>{pattern.suggestedFix}</span>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Actions */}
            <div className="flex items-center gap-2 pt-2">
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  onViewDetails();
                }}
                className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 text-sm"
              >
                <Brain className="w-4 h-4" />
                Full Analysis
              </button>
              <button className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 text-sm">
                <GitBranch className="w-4 h-4" />
                Configure Retry
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

// CI/CD Integration Panel
interface CIDCPanelProps {
  recommendations: {
    testId: string;
    testName: string;
    currentFailureRate: number;
    recommendedStrategy: string;
    potentialImprovement: number;
  }[];
}

const CIDCPanel: React.FC<CIDCPanelProps> = ({ recommendations }) => {
  const [selectedTest, setSelectedTest] = useState<string | null>(null);

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
          <GitBranch className="w-4 h-4" />
          CI/CD Integration
        </h3>
        <button className="text-xs text-blue-600 hover:text-blue-700">View All</button>
      </div>

      <div className="space-y-3">
        {recommendations.slice(0, 5).map((rec) => (
          <div
            key={rec.testId}
            className="p-3 border border-gray-200 rounded-lg hover:border-blue-300 cursor-pointer"
            onClick={() => setSelectedTest(selectedTest === rec.testId ? null : rec.testId)}
          >
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium text-sm text-gray-900">{rec.testName}</p>
                <p className="text-xs text-gray-500">Strategy: {rec.recommendedStrategy}</p>
              </div>
              <div className="text-right">
                <div className="text-sm font-medium text-green-600">
                  +{rec.potentialImprovement}%
                </div>
                <p className="text-xs text-gray-500">improvement</p>
              </div>
            </div>

            {selectedTest === rec.testId && (
              <div className="mt-3 p-3 bg-gray-50 rounded border border-gray-200">
                <p className="text-xs text-gray-600 mb-2">Pipeline Snippet:</p>
                <pre className="text-xs bg-gray-800 text-green-400 p-2 rounded overflow-x-auto">
{`retry:
  maxAttempts: ${rec.recommendedStrategy === 'EXPONENTIAL_BACKOFF' ? 3 : rec.recommendedStrategy === 'FIXED_DELAY' ? 2 : 1}
  delay: 1000ms
  strategy: ${rec.recommendedStrategy.toLowerCase().replace('_', '-')}`}
                </pre>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

// Main Dashboard Component
export const FlakyTestDashboardPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const projectId = searchParams.get('projectId') || '';

  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [timeRange, setTimeRange] = useState<string>('7d');

  // Fetch dashboard data
  const { data: dashboard, isLoading: dashboardLoading, refetch } = useQuery({
    queryKey: ['flaky-dashboard', projectId],
    queryFn: () => advancedApi.flakyTests.getDashboard(projectId),
    enabled: !!projectId,
  });

  // Fetch all flaky tests
  const { data: flakyTests = [], isLoading } = useQuery({
    queryKey: ['flaky-tests', projectId],
    queryFn: () => advancedApi.flakyTests.getAll(100),
  });

  // Mock trend data
  const trendData = useMemo(() => {
    const days = timeRange === '7d' ? 7 : timeRange === '30d' ? 30 : 90;
    return Array.from({ length: days }, (_, i) => {
      const date = new Date();
      date.setDate(date.getDate() - (days - i - 1));
      return {
        date: date.toISOString().split('T')[0],
        value: Math.floor(Math.random() * 50) + 10,
      };
    });
  }, [timeRange]);

  // Mock pattern distribution
  const patternDistribution = useMemo(() => {
    if (!dashboard?.patternsByType) {
      return {
        intermittent: 12,
        environmental: 8,
        timing: 6,
        'data-dependent': 4,
      };
    }
    return dashboard.patternsByType;
  }, [dashboard]);

  // Filter tests
  const filteredTests = useMemo(() => {
    return flakyTests
      .filter((test) => {
        const matchesSearch =
          !searchQuery ||
          test.testName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          test.testIssueKey?.toLowerCase().includes(searchQuery.toLowerCase());
        const matchesStatus = !filterStatus || test.currentStatus === filterStatus;
        return matchesSearch && matchesStatus;
      })
      .sort((a, b) => b.flakyScore - a.flakyScore);
  }, [flakyTests, searchQuery, filterStatus]);

  // Mock CI/CD recommendations
  const ciCdRecommendations = useMemo(() => {
    return flakyTests.slice(0, 5).map((test) => ({
      testId: test.testId,
      testName: test.testName || test.testIssueKey || 'Unknown',
      currentFailureRate: Number(test.flakyScore),
      recommendedStrategy: test.patterns?.[0]?.patternType === 'timing'
        ? 'EXPONENTIAL_BACKOFF'
        : test.patterns?.[0]?.patternType === 'intermittent'
        ? 'FIXED_DELAY'
        : 'SEQUENTIAL',
      potentialImprovement: Math.floor(Math.random() * 20) + 5,
    }));
  }, [flakyTests]);

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-6 py-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-3">
                <Brain className="w-8 h-8 text-purple-600" />
                Flaky Test Intelligence
              </h1>
              <p className="text-gray-500 mt-1">
                ML-powered flaky test detection, prediction, and remediation
              </p>
            </div>
            <div className="flex items-center gap-3">
              <select
                value={timeRange}
                onChange={(e) => setTimeRange(e.target.value)}
                className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="7d">Last 7 days</option>
                <option value="30d">Last 30 days</option>
                <option value="90d">Last 90 days</option>
              </select>
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
      </div>

      <div className="max-w-7xl mx-auto px-6 py-6">
        {/* Stats Overview */}
        {dashboard && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <StatsCard
              title="Total Analyzed"
              value={dashboard.totalTestsAnalyzed}
              subtitle="Tests monitored"
              trend="stable"
              trendValue="0%"
              icon={<Activity className="w-5 h-5" />}
              color="blue"
            />
            <StatsCard
              title="Stable Tests"
              value={dashboard.stableCount}
              subtitle={`${dashboard.totalTestsAnalyzed > 0 ? Math.round((dashboard.stableCount / dashboard.totalTestsAnalyzed) * 100) : 0}%`}
              trend="up"
              trendValue="+5%"
              icon={<Target className="w-5 h-5" />}
              color="green"
            />
            <StatsCard
              title="Flaky Tests"
              value={dashboard.flakyCount}
              subtitle={`${dashboard.totalTestsAnalyzed > 0 ? Math.round((dashboard.flakyCount / dashboard.totalTestsAnalyzed) * 100) : 0}%`}
              trend="down"
              trendValue="-3%"
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

        {/* Charts Row */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
          <div className="lg:col-span-2">
            <TrendChart data={trendData} title="Flaky Test Trend Over Time" />
          </div>
          <PatternPieChart data={patternDistribution} title="Pattern Distribution" />
        </div>

        {/* Main Content Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left Column - Test List */}
          <div className="lg:col-span-2 space-y-4">
            <div className="bg-white rounded-xl border border-gray-200 p-5">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
                  <BarChart3 className="w-4 h-4" />
                  Flaky Tests Analysis
                </h3>
                <div className="flex items-center gap-2">
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <input
                      type="text"
                      placeholder="Search tests..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm w-64"
                    />
                  </div>
                  <select
                    value={filterStatus}
                    onChange={(e) => setFilterStatus(e.target.value)}
                    className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                  >
                    <option value="">All Statuses</option>
                    <option value="stable">Stable</option>
                    <option value="flaky">Flaky</option>
                    <option value="quarantine_candidate">Quarantine Candidate</option>
                  </select>
                </div>
              </div>

              {isLoading || dashboardLoading ? (
                <div className="flex items-center justify-center h-64">
                  <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
                </div>
              ) : filteredTests.length === 0 ? (
                <div className="text-center py-12">
                  <Activity className="w-12 h-12 text-gray-300 mx-auto mb-4" />
                  <h3 className="text-lg font-medium text-gray-900 mb-2">No flaky tests found</h3>
                  <p className="text-gray-500">All your tests are running consistently</p>
                </div>
              ) : (
                <div className="space-y-3 max-h-[600px] overflow-y-auto">
                  {filteredTests.map((test) => (
                    <RootCauseCard
                      key={test.testId}
                      test={test}
                      onViewDetails={() => console.log('View details for:', test.testId)}
                    />
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Right Column - CI/CD & Insights */}
          <div className="space-y-6">
            <CIDCPanel recommendations={ciCdRecommendations} />

            {/* ML Model Insights */}
            <div className="bg-white rounded-xl border border-gray-200 p-5">
              <h3 className="text-sm font-semibold text-gray-700 flex items-center gap-2 mb-4">
                <Zap className="w-4 h-4 text-purple-500" />
                ML Model Insights
              </h3>
              <div className="space-y-4">
                <div className="p-3 bg-purple-50 rounded-lg">
                  <p className="text-xs font-medium text-purple-800">Model Accuracy</p>
                  <p className="text-2xl font-bold text-purple-600 mt-1">94.2%</p>
                  <p className="text-xs text-purple-600 mt-1">Based on 30-day validation</p>
                </div>
                <div className="p-3 bg-blue-50 rounded-lg">
                  <p className="text-xs font-medium text-blue-800">Predictions Generated</p>
                  <p className="text-2xl font-bold text-blue-600 mt-1">
                    {dashboard?.totalTestsAnalyzed || 0}
                  </p>
                  <p className="text-xs text-blue-600 mt-1">This period</p>
                </div>
                <div className="p-3 bg-green-50 rounded-lg">
                  <p className="text-xs font-medium text-green-800">Remediation Success</p>
                  <p className="text-2xl font-bold text-green-600 mt-1">87%</p>
                  <p className="text-xs text-green-600 mt-1">Tests improved after retry config</p>
                </div>
              </div>
            </div>

            {/* Quick Actions */}
            <div className="bg-white rounded-xl border border-gray-200 p-5">
              <h3 className="text-sm font-semibold text-gray-700 mb-4">Quick Actions</h3>
              <div className="space-y-2">
                <button className="w-full flex items-center gap-3 px-4 py-3 bg-gray-50 hover:bg-gray-100 rounded-lg text-left">
                  <ShieldAlert className="w-5 h-5 text-red-500" />
                  <div>
                    <p className="font-medium text-gray-900">Auto-Quarantine</p>
                    <p className="text-xs text-gray-500">Move high-risk flaky tests</p>
                  </div>
                </button>
                <button className="w-full flex items-center gap-3 px-4 py-3 bg-gray-50 hover:bg-gray-100 rounded-lg text-left">
                  <GitBranch className="w-5 h-5 text-blue-500" />
                  <div>
                    <p className="font-medium text-gray-900">Export CI Config</p>
                    <p className="text-xs text-gray-500">Download retry configurations</p>
                  </div>
                </button>
                <button className="w-full flex items-center gap-3 px-4 py-3 bg-gray-50 hover:bg-gray-100 rounded-lg text-left">
                  <BarChart3 className="w-5 h-5 text-green-500" />
                  <div>
                    <p className="font-medium text-gray-900">Generate Report</p>
                    <p className="text-xs text-gray-500">Weekly flaky test summary</p>
                  </div>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default FlakyTestDashboardPage;
