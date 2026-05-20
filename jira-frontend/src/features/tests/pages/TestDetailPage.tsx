import React, { useState, useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import testApi, { TestResponse } from '../../../api/testApi';
import { TestStepsEditor, TestStatusBadge, TestTypeBadge, RequirementTag, ExecutionStatusBadge } from '../components/TestComponents';
import TestExecutionPanel from '../components/TestExecutionPanel';

export const TestDetailPage: React.FC = () => {
  const { testId } = useParams<{ testId: string }>();
  const [test, setTest] = useState<TestResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'details' | 'execute' | 'history' | 'traceability'>('details');
  const [editingSteps, setEditingSteps] = useState(false);
  const [stepData, setStepData] = useState<any[]>([]);

  useEffect(() => {
    if (testId) {
      loadTest(testId);
    }
  }, [testId]);

  const loadTest = async (id: string) => {
    setLoading(true);
    try {
      const response = await testApi.getTest(id);
      setTest(response);
      setStepData(response.testSteps || []);
    } catch (error) {
      console.error('Failed to load test:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSaveSteps = async () => {
    if (!testId) return;
    try {
      await testApi.updateTest(testId, { testSteps: stepData } as any);
      setEditingSteps(false);
      loadTest(testId);
    } catch (error) {
      console.error('Failed to save steps:', error);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!test) {
    return (
      <div className="text-center py-12 text-gray-500">
        <p>Test not found</p>
      </div>
    );
  }

  return (
    <div className="test-detail-page">
      {/* Header */}
      <div className="page-header mb-6">
        <div className="flex items-center gap-2 text-sm text-gray-500 mb-2">
          <Link to="/tests" className="hover:text-blue-600">Tests</Link>
          <span>/</span>
          <span className="font-mono">{test.issueKey}</span>
        </div>
        <div className="flex items-start justify-between">
          <div>
            <h1 className="text-2xl font-bold mb-2">{test.name}</h1>
            <div className="flex items-center gap-3">
              <TestTypeBadge type={test.testType} />
              <TestStatusBadge status={test.testStatus} />
              {test.testPriority && (
                <span className="px-2 py-1 bg-gray-100 rounded text-sm">
                  Priority: {test.testPriority}
                </span>
              )}
            </div>
          </div>
          <div className="flex gap-2">
            <Link
              to={`/tests/${testId}/execute`}
              className="btn btn-primary"
            >
              ▶ Execute
            </Link>
            <button className="btn btn-secondary">✏️ Edit</button>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="tabs mb-4 border-b">
        <button
          onClick={() => setActiveTab('details')}
          className={`px-4 py-2 border-b-2 ${
            activeTab === 'details' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500'
          }`}
        >
          Details
        </button>
        <button
          onClick={() => setActiveTab('execute')}
          className={`px-4 py-2 border-b-2 ${
            activeTab === 'execute' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500'
          }`}
        >
          Execute
        </button>
        <button
          onClick={() => setActiveTab('history')}
          className={`px-4 py-2 border-b-2 ${
            activeTab === 'history' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500'
          }`}
        >
          History
        </button>
        <button
          onClick={() => setActiveTab('traceability')}
          className={`px-4 py-2 border-b-2 ${
            activeTab === 'traceability' ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500'
          }`}
        >
          Traceability
        </button>
      </div>

      {/* Tab Content */}
      <div className="tab-content">
        {activeTab === 'details' && (
          <div className="grid grid-cols-3 gap-6">
            {/* Main Content */}
            <div className="col-span-2 space-y-6">
              {/* Description */}
              <div className="card border rounded-lg p-4">
                <h3 className="font-semibold mb-2">Description</h3>
                <p className="text-gray-600 whitespace-pre-wrap">
                  {test.description || 'No description provided.'}
                </p>
              </div>

              {/* Test Steps */}
              <div className="card border rounded-lg p-4">
                <div className="flex items-center justify-between mb-3">
                  <h3 className="font-semibold">Test Steps</h3>
                  {!editingSteps && (
                    <button
                      onClick={() => setEditingSteps(true)}
                      className="btn btn-sm btn-secondary"
                    >
                      Edit Steps
                    </button>
                  )}
                </div>
                {editingSteps ? (
                  <div>
                    <TestStepsEditor
                      steps={stepData}
                      onChange={setStepData}
                    />
                    <div className="flex justify-end gap-2 mt-4">
                      <button
                        onClick={() => {
                          setEditingSteps(false);
                          setStepData(test.testSteps || []);
                        }}
                        className="btn btn-secondary"
                      >
                        Cancel
                      </button>
                      <button
                        onClick={handleSaveSteps}
                        className="btn btn-primary"
                      >
                        Save Steps
                      </button>
                    </div>
                  </div>
                ) : (
                  <TestStepsEditor
                    steps={test.testSteps || []}
                    onChange={() => {}}
                    readOnly
                  />
                )}
              </div>

              {/* Requirement Links */}
              {test.requirementKeys && test.requirementKeys.length > 0 && (
                <div className="card border rounded-lg p-4">
                  <h3 className="font-semibold mb-3">Linked Requirements</h3>
                  <div className="flex flex-wrap">
                    {test.requirementKeys.map((key) => (
                      <RequirementTag key={key} requirementKey={key} />
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Sidebar */}
            <div className="space-y-4">
              {/* Metadata */}
              <div className="card border rounded-lg p-4">
                <h3 className="font-semibold mb-3">Details</h3>
                <dl className="space-y-2 text-sm">
                  <div className="flex justify-between">
                    <dt className="text-gray-500">Key</dt>
                    <dd className="font-mono">{test.issueKey}</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-gray-500">Project</dt>
                    <dd>{test.projectId?.slice(0, 8)}...</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-gray-500">Test Type</dt>
                    <dd>{test.testType}</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-gray-500">Status</dt>
                    <dd><TestStatusBadge status={test.testStatus} /></dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-gray-500">Created</dt>
                    <dd>{new Date(test.createdAt).toLocaleDateString()}</dd>
                  </div>
                  <div className="flex justify-between">
                    <dt className="text-gray-500">Updated</dt>
                    <dd>{new Date(test.updatedAt).toLocaleDateString()}</dd>
                  </div>
                </dl>
              </div>

              {/* Labels */}
              {test.labels && test.labels.length > 0 && (
                <div className="card border rounded-lg p-4">
                  <h3 className="font-semibold mb-3">Labels</h3>
                  <div className="flex flex-wrap gap-1">
                    {test.labels.map((label) => (
                      <span
                        key={label}
                        className="px-2 py-1 bg-gray-100 rounded text-xs"
                      >
                        {label}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* BDD Info */}
              {test.gherkinFeatureKey && (
                <div className="card border rounded-lg p-4">
                  <h3 className="font-semibold mb-3">BDD Information</h3>
                  <dl className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <dt className="text-gray-500">Feature</dt>
                      <dd className="font-mono text-xs">{test.gherkinFeatureKey}</dd>
                    </div>
                    {test.gherkinScenarioId && (
                      <div className="flex justify-between">
                        <dt className="text-gray-500">Scenario ID</dt>
                        <dd className="font-mono text-xs">{test.gherkinScenarioId}</dd>
                      </div>
                    )}
                  </dl>
                </div>
              )}
            </div>
          </div>
        )}

        {activeTab === 'execute' && (
          <TestExecutionPanel
            testId={test.id}
            issueKey={test.issueKey}
            testName={test.name}
            testSteps={test.testSteps || []}
          />
        )}

        {activeTab === 'history' && (
          <div className="execution-history">
            <h3 className="text-lg font-semibold mb-4">Test Execution History</h3>
            <p className="text-gray-500">View past executions on the Execute tab.</p>
          </div>
        )}

        {activeTab === 'traceability' && (
          <div className="traceability">
            <h3 className="text-lg font-semibold mb-4">Traceability</h3>
            <p className="text-gray-500">View this test's requirement links in the full matrix.</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default TestDetailPage;