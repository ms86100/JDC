import React, { useState, useEffect } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import combinedApi from '../../../api/testApi';
import TestList from '../components/TestList';
import TestCreateModal from '../components/TestCreateModal';
import TestReportsDashboard from '../components/TestReportsDashboard';
import ImportPanel from '../components/ImportPanel';
import TestSetsList from '../components/TestSetsList';
import TestPlansList from '../components/TestPlansList';
import XrayTestHub from '../components/XrayTestHub';
import { XRAY_PLUGIN_LABEL } from '../xrayNavRegistry';
import '../styles/xray-hub.css';

const VIEW_PARAM_MAP: Record<string, 'tests' | 'sets' | 'plans' | 'reports' | 'import'> = {
  tests: 'tests',
  sets: 'sets',
  plans: 'plans',
  reports: 'reports',
  import: 'import',
};

export const TestManagementPage: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const [searchParams] = useSearchParams();
  const initialView = VIEW_PARAM_MAP[searchParams.get('view') || ''] || 'tests';
  const [activeView, setActiveView] = useState<'tests' | 'sets' | 'plans' | 'reports' | 'import'>(initialView);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [filter, setFilter] = useState<{ testType?: string; testStatus?: string; search?: string }>({});
  const [stats, setStats] = useState<{ tests: number; sets: number; plans: number; executions: number } | null>(null);
  const [showHub, setShowHub] = useState(false);

  useEffect(() => {
    if (projectId) {
      loadStats();
    }
  }, [projectId]);

  const loadStats = async () => {
    try {
      const response = await combinedApi.getTestSummary(projectId!);
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

  if (!projectId) {
    return <XrayTestHub showProjectPicker />;
  }

  return (
    <div className="test-management-page xray-project-shell">
      <div className="page-header mb-6 flex items-center justify-between flex-wrap gap-3">
        <div>
          <span className="xray-hub-badge">Xray plugin</span>
          <h1 className="text-2xl font-bold" style={{ margin: '4px 0 0' }}>
            {XRAY_PLUGIN_LABEL}
          </h1>
          <p className="jdc-muted text-sm mt-1">
            Project scope · <Link to="/tests" className="jdc-link">Change project</Link>
            {' · '}
            <button type="button" className="jdc-link jdc-link-btn" onClick={() => setShowHub(!showHub)}>
              {showHub ? 'Hide' : 'All Xray modules'}
            </button>
          </p>
        </div>
        <div className="flex gap-3">
          <Link to={`/tests/create/${projectId}`} className="jdc-btn jdc-btn-secondary">
            Create test (wizard)
          </Link>
          <button type="button" className="jdc-btn jdc-btn-primary" onClick={() => setShowCreateModal(true)}>
            + Create test
          </button>
        </div>
      </div>

      {showHub && <XrayTestHub projectId={projectId} showProjectPicker={false} />}

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

      <div className="tabs mb-6 flex gap-2 border-b flex-wrap">
        {(['tests', 'sets', 'plans', 'reports', 'import'] as const).map((view) => (
          <button
            key={view}
            type="button"
            onClick={() => setActiveView(view)}
            className={`px-4 py-2 font-medium ${activeView === view ? 'text-blue-600 border-b-2 border-blue-600' : 'text-gray-500'}`}
          >
            {view === 'tests' && '📝 Tests'}
            {view === 'sets' && '📁 Test Sets'}
            {view === 'plans' && '📋 Test Plans'}
            {view === 'reports' && '📊 Reports'}
            {view === 'import' && '📥 Import'}
          </button>
        ))}
      </div>

      <div className="content">
        {activeView === 'tests' && (
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
                className="px-3 py-2 border rounded"
                value={filter.testType || ''}
                onChange={(e) => setFilter({ ...filter, testType: e.target.value || undefined })}
              >
                <option value="">All Types</option>
                <option value="MANUAL">Manual</option>
                <option value="AUTOMATED">Automated</option>
                <option value="BDD">BDD</option>
              </select>
              <select
                className="px-3 py-2 border rounded"
                value={filter.testStatus || ''}
                onChange={(e) => setFilter({ ...filter, testStatus: e.target.value || undefined })}
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
        {activeView === 'sets' && <TestSetsList projectId={projectId} />}
        {activeView === 'plans' && <TestPlansList projectId={projectId} />}
        {activeView === 'reports' && <TestReportsDashboard projectId={projectId} />}
        {activeView === 'import' && (
          <div className="grid grid-cols-3 gap-6">
            <div className="col-span-2">
              <div className="card border rounded-lg p-4">
                <ImportPanel projectId={projectId} onImportComplete={loadStats} />
              </div>
            </div>
            <div>
              <div className="card border rounded-lg p-4">
                <h3 className="font-semibold mb-3">Import Help</h3>
                <p className="text-sm text-gray-600">Cucumber/Gherkin and JUnit XML supported (Xray import).</p>
              </div>
            </div>
          </div>
        )}
      </div>

      {showCreateModal && (
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
