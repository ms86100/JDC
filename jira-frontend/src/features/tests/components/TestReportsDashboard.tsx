import React, { useState, useEffect } from 'react';
import testApi, { TestSummaryReport } from '../../../api/testApi';

interface TestReportsDashboardProps {
  projectId: string;
}

export const TestReportsDashboard: React.FC<TestReportsDashboardProps> = ({ projectId }) => {
  const [report, setReport] = useState<TestSummaryReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [timeRange, setTimeRange] = useState<'7d' | '30d' | '90d'>('30d');

  useEffect(() => {
    loadReport();
  }, [projectId, timeRange]);

  const loadReport = async () => {
    setLoading(true);
    try {
      const response = await testApi.getTestSummary(projectId);
      setReport(response);
    } catch (error) {
      console.error('Failed to load test summary:', error);
    } finally {
      setLoading(false);
    }
  };

  const getPassRateColor = (rate: number) => {
    if (rate >= 90) return 'text-green-600';
    if (rate >= 70) return 'text-yellow-600';
    return 'text-red-600';
  };

  const getPassRateBg = (rate: number) => {
    if (rate >= 90) return 'bg-green-100';
    if (rate >= 70) return 'bg-yellow-100';
    return 'bg-red-100';
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!report) {
    return (
      <div className="text-center py-12 text-gray-500">
        <p>Failed to load test summary</p>
      </div>
    );
  }

  return (
    <div className="test-reports-dashboard">
      <div className="dashboard-header mb-6 flex items-center justify-between">
        <h2 className="text-xl font-semibold">Test Summary Dashboard</h2>
        <div className="time-filter flex gap-2">
          <button
            onClick={() => setTimeRange('7d')}
            className={`btn btn-sm ${timeRange === '7d' ? 'btn-primary' : 'btn-secondary'}`}
          >
            7 Days
          </button>
          <button
            onClick={() => setTimeRange('30d')}
            className={`btn btn-sm ${timeRange === '30d' ? 'btn-primary' : 'btn-secondary'}`}
          >
            30 Days
          </button>
          <button
            onClick={() => setTimeRange('90d')}
            className={`btn btn-sm ${timeRange === '90d' ? 'btn-primary' : 'btn-secondary'}`}
          >
            90 Days
          </button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="summary-cards grid grid-cols-4 gap-4 mb-6">
        <div className="card p-4 border rounded-lg">
          <div className="text-sm text-gray-500 mb-1">Total Tests</div>
          <div className="text-2xl font-bold">{report.totalTests}</div>
        </div>
        <div className="card p-4 border rounded-lg">
          <div className="text-sm text-gray-500 mb-1">Test Sets</div>
          <div className="text-2xl font-bold">{report.totalTestSets}</div>
        </div>
        <div className="card p-4 border rounded-lg">
          <div className="text-sm text-gray-500 mb-1">Test Plans</div>
          <div className="text-2xl font-bold">{report.totalTestPlans}</div>
        </div>
        <div className="card p-4 border rounded-lg">
          <div className="text-sm text-gray-500 mb-1">Total Executions</div>
          <div className="text-2xl font-bold">{report.totalExecutions}</div>
        </div>
      </div>

      {/* Pass Rate */}
      <div className="pass-rate-section mb-6">
        <div className={`p-6 rounded-lg ${getPassRateBg(report.passRate)}`}>
          <div className="flex items-center justify-between">
            <div>
              <div className="text-lg font-medium mb-2">Overall Pass Rate</div>
              <div className={`text-4xl font-bold ${getPassRateColor(report.passRate)}`}>
                {report.passRate.toFixed(1)}%
              </div>
            </div>
            <div className="text-right">
              <div className="text-6xl font-bold text-gray-300">
                {Math.round(report.passRate)}%
              </div>
            </div>
          </div>

          {/* Rate breakdown */}
          <div className="mt-4 grid grid-cols-4 gap-4">
            <div>
              <div className="text-sm text-gray-600">Pass Rate</div>
              <div className="text-lg font-semibold text-green-600">{report.passRate.toFixed(1)}%</div>
            </div>
            <div>
              <div className="text-sm text-gray-600">Fail Rate</div>
              <div className="text-lg font-semibold text-red-600">{report.failRate.toFixed(1)}%</div>
            </div>
            <div>
              <div className="text-sm text-gray-600">Blocked</div>
              <div className="text-lg font-semibold text-orange-600">{report.blockedRate.toFixed(1)}%</div>
            </div>
            <div>
              <div className="text-sm text-gray-600">Skipped</div>
              <div className="text-lg font-semibold text-yellow-600">{report.skippedRate.toFixed(1)}%</div>
            </div>
          </div>
        </div>
      </div>

      {/* Execution Stats */}
      <div className="execution-stats mb-6">
        <h3 className="text-lg font-semibold mb-3">Execution Status Breakdown</h3>
        <div className="grid grid-cols-2 gap-4">
          {Object.entries(report.executionsByStatus).map(([status, count]) => (
            <div key={status} className="flex items-center justify-between p-3 bg-gray-50 rounded">
              <span className="font-medium">{status}</span>
              <span className="text-lg font-semibold">{count}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Top Failing Tests */}
      {report.topFailingTests && report.topFailingTests.length > 0 && (
        <div className="top-failing-tests mb-6">
          <h3 className="text-lg font-semibold mb-3">Top Failing Tests</h3>
          <div className="border rounded-lg overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Test</th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
                  <th className="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase">Failures</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {report.topFailingTests.map((test, index) => (
                  <tr key={index}>
                    <td className="px-4 py-2 font-mono text-sm text-blue-600">{test.issueKey}</td>
                    <td className="px-4 py-2 text-sm">{test.name}</td>
                    <td className="px-4 py-2 text-right">
                      <span className="px-2 py-1 bg-red-100 text-red-800 rounded-full text-sm font-medium">
                        {test.failureCount}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Recent Executions */}
      {report.recentExecutions && report.recentExecutions.length > 0 && (
        <div className="recent-executions">
          <h3 className="text-lg font-semibold mb-3">Recent Executions</h3>
          <div className="space-y-2">
            {report.recentExecutions.slice(0, 10).map((exec) => (
              <div
                key={exec.id}
                className="flex items-center justify-between p-3 bg-gray-50 rounded hover:bg-gray-100"
              >
                <div>
                  <span className="font-mono text-sm text-blue-600">{exec.issueKey}</span>
                  <span className="ml-3 text-sm text-gray-600">
                    {exec.startedAt && new Date(exec.startedAt).toLocaleDateString()}
                  </span>
                </div>
                <span
                  className={`px-2 py-1 rounded-full text-xs font-medium ${
                    exec.status === 'PASSED'
                      ? 'bg-green-100 text-green-800'
                      : exec.status === 'FAILED'
                      ? 'bg-red-100 text-red-800'
                      : 'bg-gray-100 text-gray-800'
                  }`}
                >
                  {exec.status}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default TestReportsDashboard;