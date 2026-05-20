import React, { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import testApi from '../../../api/testApi';
import TestList from '../components/TestList';
import TestCreateModal from '../components/TestCreateModal';
import TestReportsDashboard from '../components/TestReportsDashboard';
import TraceabilityMatrix from '../components/TraceabilityMatrix';
import ImportPanel from '../components/ImportPanel';

export const TestManagementPage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const [activeView, setActiveView] = useState<'tests' | 'sets' | 'plans' | 'reports' | 'import'>('tests');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [filter, setFilter] = useState<{ testType?: string; testStatus?: string; search?: string }>({});
  const [stats, setStats] = useState<{ tests: number; sets: number; plans: number; executions: number } | null>(null);

  useEffect(() => {
    if (projectId) {
      loadStats();
    }
  }, [projectId]);

  const loadStats = async () => {
    try {
      const response = await testApi.getTestSummary(projectId!);
      setStats({
        tests: response.totalTests,
        sets: response.totalTestSets,
        plans: response.totalTestPlans,
        executions: response.totalExecutions,
      });
    } catch (error) {
      console.error('Failed to load stats:', error);
    }
  };

  return (
    <div className="test-management-page">
      {/* Header */}
      <div className="page-header mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Test Management</h1>
          {projectId && (
            <p className="text-gray-500 text-sm mt-1">
              Project: {projectId.slice(0, 8)}...
            </p>
          )}
        </div>
        <div className="flex gap-3">
          <button
            onClick={() => setShowCreateModal(true)}
            className="btn btn-primary"
          >
            + Create Test
          </button>
        </div>
      </div>

      {/* Quick Stats */}
      {stats && (
        <div className="stats-bar grid grid-cols-4 gap-4 mb-6">
          <div className="card border rounded-lg p-4 cursor-pointer hover:bg-gray-50" onClick={() => setActiveView('tests')}>
            <div className="text-sm text-gray-500">Total Tests</div>
            <div className="text-2xl font-bold">{stats.tests}</div>
          </div>
          <div className="card border rounded-lg p-4 cursor-pointer hover:bg-gray-50" onClick={() => setActiveView('sets')}>
            <div className="text-sm text-gray-500">Test Sets</div>
            <div className="text-2xl font-bold">{stats.sets}</div>
          </div>
          <div className="card border rounded-lg p-4 cursor-pointer hover:bg-gray-50" onClick={() => setActiveView('plans')}>
            <div className="text-sm text-gray-500">Test Plans</div>
            <div className="text-2xl font-bold">{stats.plans}</div>
          </div>
          <div className="card border rounded-lg p-4 cursor-pointer hover:bg-gray-50" onClick={() => setActiveView('reports')}>
            <div className="text-sm text-gray-500">Executions</div>
            <div className="text-2xl font-bold">{stats.executions}</div>
          </div>
        </div>
      )}

      {/* Navigation Tabs */}
      <div className="tabs mb-6 flex gap-2 border-b">
        <button
          onClick={() => setActiveView('tests')}
          className={`px-4 py-2 font-medium ${
            activeView === 'tests'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          📝 Tests
        </button>
        <button
          onClick={() => setActiveView('sets')}
          className={`px-4 py-2 font-medium ${
            activeView === 'sets'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          📁 Test Sets
        </button>
        <button
          onClick={() => setActiveView('plans')}
          className={`px-4 py-2 font-medium ${
            activeView === 'plans'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          📋 Test Plans
        </button>
        <button
          onClick={() => setActiveView('reports')}
          className={`px-4 py-2 font-medium ${
            activeView === 'reports'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          📊 Reports
        </button>
        <button
          onClick={() => setActiveView('import')}
          className={`px-4 py-2 font-medium ${
            activeView === 'import'
              ? 'text-blue-600 border-b-2 border-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          📥 Import
        </button>
      </div>

      {/* Content */}
      <div className="content">
        {activeView === 'tests' && projectId && (
          <>
            <div className="filter-bar mb-4 flex gap-4 items-center">
              <input
                type="text"
                placeholder="Search tests..."
                value={filter.search || ''}
                onChange={(e) => setFilter({ ...filter, search: e.target.value })}
                className="px-3 py-2 border rounded w-64"
              />
              <select
                value={filter.testType || ''}
                onChange={(e) => setFilter({ ...filter, testType: e.target.value || undefined })}
                className="px-3 py-2 border rounded"
              >
                <option value="">All Types</option>
                <option value="MANUAL">Manual</option>
                <option value="AUTOMATED">Automated</option>
                <option value="BDD">BDD</option>
              </select>
              <select
                value={filter.testStatus || ''}
                onChange={(e) => setFilter({ ...filter, testStatus: e.target.value || undefined })}
                className="px-3 py-2 border rounded"
              >
                <option value="">All Statuses</option>
                <option value="DRAFT">Draft</option>
                <option value="READY">Ready</option>
                <option value="APPROVED">Approved</option>
                <option value="DEPRECATED">Deprecated</option>
              </select>
            </div>
            <TestList projectId={projectId} filter={filter} />
          </>
        )}

        {activeView === 'sets' && (
          <div className="test-sets-view">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg font-semibold">Test Sets</h2>
              <button className="btn btn-secondary">+ Create Test Set</button>
            </div>
            <p className="text-gray-500">Create and manage test sets to group related tests.</p>
          </div>
        )}

        {activeView === 'plans' && (
          <div className="test-plans-view">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg font-semibold">Test Plans</h2>
              <button className="btn btn-secondary">+ Create Test Plan</button>
            </div>
            <p className="text-gray-500">Create test plans to organize test execution cycles.</p>
          </div>
        )}

        {activeView === 'reports' && projectId && (
          <TestReportsDashboard projectId={projectId} />
        )}

        {activeView === 'import' && projectId && (
          <div className="grid grid-cols-3 gap-6">
            <div className="col-span-2">
              <div className="card border rounded-lg p-4">
                <ImportPanel projectId={projectId} onImportComplete={loadStats} />
              </div>
            </div>
            <div>
              <div className="card border rounded-lg p-4">
                <h3 className="font-semibold mb-3">Import Help</h3>
                <div className="space-y-3 text-sm">
                  <div>
                    <h4 className="font-medium">🥒 Cucumber / Gherkin</h4>
                    <p className="text-gray-600">
                      Upload .feature files containing BDD scenarios. Each scenario becomes a test with Given/When/Then steps.
                    </p>
                  </div>
                  <div>
                    <h4 className="font-medium">📄 JUnit XML</h4>
                    <p className="text-gray-600">
                      Import CI/CD test results from Jenkins, GitHub Actions, GitLab CI, or Azure DevOps.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Create Modal */}
      {showCreateModal && projectId && (
        <TestCreateModal
          projectId={projectId}
          isOpen={showCreateModal}
          onClose={() => setShowCreateModal(false)}
          onSuccess={() => {
            setShowCreateModal(false);
            loadStats();
          }}
        />
      )}
    </div>
  );
};

export default TestManagementPage;