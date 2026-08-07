import React, { useState, useEffect } from 'react';
import combinedApi, {
  TestExecutionResponse,
  StepResultRequest,
} from '../../../api/testApi';
import { ExecutionStatusBadge } from './TestComponents';

interface TestExecutionPanelProps {
  testId: string;
  issueKey: string;
  testName: string;
  testSteps: { index: number; description: string; expectedResult: string }[];
}

export const TestExecutionPanel: React.FC<TestExecutionPanelProps> = ({
  testId,
  issueKey,
  testName,
  testSteps,
}) => {
  const [executions, setExecutions] = useState<TestExecutionResponse[]>([]);
  const [currentExecution, setCurrentExecution] = useState<TestExecutionResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [starting, setStarting] = useState(false);
  const [completing, setCompleting] = useState(false);
  const [stepResults, setStepResults] = useState<Map<number, StepResultRequest>>(new Map());

  useEffect(() => {
    loadExecutions();
  }, [testId]);

  const loadExecutions = async () => {
    setLoading(true);
    try {
      const response = await combinedApi.getExecutionsByTest(testId);
      setExecutions(response);
      if (response.length > 0) {
        const latest = response.find(e => e.status === 'RUNNING') || response[0];
        setCurrentExecution(latest);
        if (latest.stepResults) {
          const results = new Map<number, StepResultRequest>();
          latest.stepResults.forEach((sr) => {
            results.set(sr.stepIndex, {
              stepIndex: sr.stepIndex,
              status: sr.status as any,
              comment: sr.comment,
              evidenceUrls: sr.evidenceUrls,
              defectKey: sr.defectKey,
            });
          });
          setStepResults(results);
        }
      }
    } catch (error) {
      console.error('Failed to load executions:', error);
    } finally {
      setLoading(false);
    }
  };

  const startExecution = async () => {
    setStarting(true);
    try {
      const response = await combinedApi.createExecution({
        testIds: [testId],
        name: `Run ${issueKey} - ${new Date().toLocaleString()}`,
        testEnv: 'DEFAULT',
      });
      setCurrentExecution(response);
      setStepResults(new Map());
      loadExecutions();
    } catch (error) {
      console.error('Failed to start execution:', error);
    } finally {
      setStarting(false);
    }
  };

  const updateStepResult = (stepIndex: number, status: StepResultRequest['status']) => {
    setStepResults((prev) => {
      const updated = new Map(prev);
      const existing = updated.get(stepIndex);
      updated.set(stepIndex, {
        stepIndex,
        status,
        comment: existing?.comment,
        evidenceUrls: existing?.evidenceUrls,
        defectKey: existing?.defectKey,
      });
      return updated;
    });
  };

  const saveStepResult = async (stepIndex: number) => {
    const result = stepResults.get(stepIndex);
    if (!result || !currentExecution) return;

    try {
      await combinedApi.addStepResult(currentExecution.id, result);
    } catch (error) {
      console.error('Failed to save step result:', error);
    }
  };

  const completeExecution = async () => {
    if (!currentExecution) return;

    const passedSteps = Array.from(stepResults.values()).filter((r) => r.status === 'PASSED').length;
    const failedSteps = Array.from(stepResults.values()).filter((r) => r.status === 'FAILED').length;
    const status = failedSteps > 0 ? 'FAILED' : passedSteps === testSteps.length ? 'PASSED' : 'RUNNING';

    setCompleting(true);
    try {
      await combinedApi.completeExecution(currentExecution.id, status);
      loadExecutions();
      setCurrentExecution(null);
    } catch (error) {
      console.error('Failed to complete execution:', error);
    } finally {
      setCompleting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-8">
        <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="test-execution-panel">
      <div className="execution-header mb-4">
        <h3 className="text-lg font-semibold">Test Execution</h3>
        <p className="text-sm text-gray-600">
          {testName} ({issueKey})
        </p>
      </div>

      {!currentExecution ? (
        <div className="text-center py-8">
          <p className="text-gray-500 mb-4">No active execution</p>
          <button
            onClick={startExecution}
            disabled={starting}
            className="btn btn-primary"
          >
            {starting ? 'Starting...' : 'Start Execution'}
          </button>
        </div>
      ) : (
        <div className="execution-content">
          <div className="execution-info mb-4 p-3 bg-gray-50 rounded-lg">
            <div className="flex items-center justify-between">
              <div>
                <span className="font-medium">Execution ID:</span> {currentExecution.id.slice(0, 8)}...
              </div>
              <ExecutionStatusBadge status={currentExecution.status} />
            </div>
            {currentExecution.startedAt && (
              <div className="text-sm text-gray-600 mt-1">
                Started: {new Date(currentExecution.startedAt).toLocaleString()}
              </div>
            )}
          </div>

          <div className="steps-execution space-y-3">
            <h4 className="font-medium">Test Steps</h4>
            {testSteps.map((step) => {
              const result = stepResults.get(step.index);
              const status = result?.status || 'UNTESTED';

              return (
                <div
                  key={step.index}
                  className={`step-item border rounded-lg p-4 ${
                    status === 'PASSED'
                      ? 'border-green-300 bg-green-50'
                      : status === 'FAILED'
                      ? 'border-red-300 bg-red-50'
                      : status === 'BLOCKED'
                      ? 'border-orange-300 bg-orange-50'
                      : 'border-gray-200 bg-white'
                  }`}
                >
                  <div className="flex items-start justify-between mb-2">
                    <div className="flex items-center">
                      <span className="step-number font-semibold text-blue-600 mr-3">
                        Step {step.index}
                      </span>
                      <ExecutionStatusBadge status={status} />
                    </div>
                    {currentExecution.status === 'RUNNING' && (
                      <div className="step-controls flex gap-2">
                        <button
                          onClick={() => updateStepResult(step.index, 'PASSED')}
                          className="btn btn-sm bg-green-500 text-white hover:bg-green-600"
                        >
                          Pass
                        </button>
                        <button
                          onClick={() => updateStepResult(step.index, 'FAILED')}
                          className="btn btn-sm bg-red-500 text-white hover:bg-red-600"
                        >
                          Fail
                        </button>
                        <button
                          onClick={() => updateStepResult(step.index, 'BLOCKED')}
                          className="btn btn-sm bg-orange-500 text-white hover:bg-orange-600"
                        >
                          Block
                        </button>
                        <button
                          onClick={() => updateStepResult(step.index, 'SKIPPED')}
                          className="btn btn-sm bg-yellow-500 text-white hover:bg-yellow-600"
                        >
                          Skip
                        </button>
                      </div>
                    )}
                  </div>

                  <div className="step-content text-sm">
                    <p className="mb-2">
                      <strong>Given:</strong> {step.description}
                    </p>
                    <p className="text-gray-600">
                      <strong>Expected:</strong> {step.expectedResult}
                    </p>
                  </div>

                  {result?.comment && (
                    <div className="step-comment mt-2 p-2 bg-white rounded border">
                      <p className="text-sm">{result.comment}</p>
                    </div>
                  )}

                  {currentExecution.status === 'RUNNING' && status !== 'UNTESTED' && (
                    <button
                      onClick={() => saveStepResult(step.index)}
                      className="btn btn-sm btn-secondary mt-2"
                    >
                      Save Result
                    </button>
                  )}
                </div>
              );
            })}
          </div>

          {currentExecution.status === 'RUNNING' && (
            <div className="mt-4 pt-4 border-t flex justify-end gap-3">
              <button
                onClick={completeExecution}
                disabled={completing}
                className="btn btn-primary"
              >
                {completing ? 'Completing...' : 'Complete Execution'}
              </button>
            </div>
          )}
        </div>
      )}

      {executions.length > 0 && (
        <div className="execution-history mt-8">
          <h4 className="font-medium mb-3">Execution History</h4>
          <div className="space-y-2">
            {(Array.isArray(executions) ? executions : []).slice(0, 5).map((exec) => (
              <div
                key={exec.id}
                className="flex items-center justify-between p-3 bg-gray-50 rounded hover:bg-gray-100 cursor-pointer"
                onClick={() => setCurrentExecution(exec)}
              >
                <div>
                  <span className="font-mono text-sm">{exec.id.slice(0, 8)}...</span>
                  <span className="ml-3 text-sm text-gray-600">
                    {exec.startedAt && new Date(exec.startedAt).toLocaleDateString()}
                  </span>
                </div>
                <ExecutionStatusBadge status={exec.status} />
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default TestExecutionPanel;