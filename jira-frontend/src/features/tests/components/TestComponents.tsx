import React, { useState, useEffect } from 'react';
import testApi, { TestResponse, CreateTestRequest, UpdateTestRequest } from '../../../api/testApi';
import { useParams, useNavigate } from 'react-router-dom';

interface TestStepsEditorProps {
  steps: { index: number; description: string; expectedResult: string; testData?: string }[];
  onChange: (steps: { index: number; description: string; expectedResult: string; testData?: string }[]) => void;
  readOnly?: boolean;
}

export const TestStepsEditor: React.FC<TestStepsEditorProps> = ({ steps, onChange, readOnly = false }) => {
  const addStep = () => {
    onChange([...steps, { index: steps.length + 1, description: '', expectedResult: '', testData: '' }]);
  };

  const updateStep = (index: number, field: string, value: string) => {
    const updated = [...steps];
    updated[index] = { ...updated[index], [field]: value };
    onChange(updated);
  };

  const removeStep = (index: number) => {
    const updated = steps.filter((_, i) => i !== index).map((s, i) => ({ ...s, index: i + 1 }));
    onChange(updated);
  };

  const moveStep = (index: number, direction: 'up' | 'down') => {
    const newIndex = direction === 'up' ? index - 1 : index + 1;
    if (newIndex < 0 || newIndex >= steps.length) return;
    const updated = [...steps];
    [updated[index], updated[newIndex]] = [updated[newIndex], updated[index]];
    onChange(updated.map((s, i) => ({ ...s, index: i + 1 })));
  };

  return (
    <div className="test-steps-editor">
      <div className="steps-header flex items-center justify-between mb-3">
        <h4 className="font-medium">Test Steps</h4>
        {!readOnly && (
          <button type="button" onClick={addStep} className="btn btn-sm btn-secondary">
            + Add Step
          </button>
        )}
      </div>

      {steps.length === 0 ? (
        <div className="empty-state text-center py-8 text-gray-500">
          <p>No test steps defined. Click "Add Step" to begin.</p>
        </div>
      ) : (
        <div className="steps-list space-y-3">
          {steps.map((step, index) => (
            <div key={index} className="step-card border rounded-lg p-4 bg-white">
              <div className="step-header flex items-center justify-between mb-2">
                <span className="step-number font-semibold text-blue-600">Step {step.index}</span>
                {!readOnly && (
                  <div className="step-actions flex gap-2">
                    <button
                      type="button"
                      onClick={() => moveStep(index, 'up')}
                      disabled={index === 0}
                      className="btn btn-sm btn-ghost"
                    >
                      ↑
                    </button>
                    <button
                      type="button"
                      onClick={() => moveStep(index, 'down')}
                      disabled={index === steps.length - 1}
                      className="btn btn-sm btn-ghost"
                    >
                      ↓
                    </button>
                    <button
                      type="button"
                      onClick={() => removeStep(index)}
                      className="btn btn-sm btn-ghost text-red-600"
                    >
                      ✕
                    </button>
                  </div>
                )}
              </div>

              <div className="step-fields space-y-2">
                <div>
                  <label className="block text-sm font-medium mb-1">Description</label>
                  <textarea
                    value={step.description}
                    onChange={(e) => updateStep(index, 'description', e.target.value)}
                    readOnly={readOnly}
                    className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                    rows={2}
                    placeholder="Enter step description (Given/When/Then for BDD)..."
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium mb-1">Expected Result</label>
                  <textarea
                    value={step.expectedResult}
                    onChange={(e) => updateStep(index, 'expectedResult', e.target.value)}
                    readOnly={readOnly}
                    className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                    rows={2}
                    placeholder="Enter expected result..."
                  />
                </div>
                {step.testData && (
                  <div>
                    <label className="block text-sm font-medium mb-1">Test Data</label>
                    <input
                      type="text"
                      value={step.testData}
                      onChange={(e) => updateStep(index, 'testData', e.target.value)}
                      readOnly={readOnly}
                      className="w-full px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                      placeholder="Enter test data..."
                    />
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

interface TestStatusBadgeProps {
  status: string;
}

export const TestStatusBadge: React.FC<TestStatusBadgeProps> = ({ status }) => {
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'READY': return 'bg-green-100 text-green-800';
      case 'DRAFT': return 'bg-gray-100 text-gray-800';
      case 'APPROVED': return 'bg-blue-100 text-blue-800';
      case 'DEPRECATED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(status)}`}>
      {status}
    </span>
  );
};

interface TestTypeBadgeProps {
  type: string;
}

export const TestTypeBadge: React.FC<TestTypeBadgeProps> = ({ type }) => {
  const getTypeConfig = (type: string) => {
    switch (type) {
      case 'MANUAL': return { label: 'Manual', color: 'bg-purple-100 text-purple-800' };
      case 'AUTOMATED': return { label: 'Automated', color: 'bg-blue-100 text-blue-800' };
      case 'BDD': return { label: 'BDD', color: 'bg-green-100 text-green-800' };
      default: return { label: type, color: 'bg-gray-100 text-gray-800' };
    }
  };

  const config = getTypeConfig(type);
  return (
    <span className={`px-2 py-1 rounded-full text-xs font-medium ${config.color}`}>
      {config.label}
    </span>
  );
};

interface ExecutionStatusBadgeProps {
  status: string;
}

export const ExecutionStatusBadge: React.FC<ExecutionStatusBadgeProps> = ({ status }) => {
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PASSED': return 'bg-green-100 text-green-800';
      case 'FAILED': return 'bg-red-100 text-red-800';
      case 'BLOCKED': return 'bg-orange-100 text-orange-800';
      case 'SKIPPED': return 'bg-yellow-100 text-yellow-800';
      case 'RUNNING': return 'bg-blue-100 text-blue-800';
      case 'READY': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(status)}`}>
      {status}
    </span>
  );
};

interface RequirementTagProps {
  requirementKey: string;
  onRemove?: () => void;
}

export const RequirementTag: React.FC<RequirementTagProps> = ({ requirementKey, onRemove }) => {
  return (
    <span className="inline-flex items-center px-2 py-1 rounded bg-blue-100 text-blue-800 text-xs mr-2 mb-1">
      <span className="mr-1">📋</span>
      {requirementKey}
      {onRemove && (
        <button onClick={onRemove} className="ml-1 hover:text-red-600">×</button>
      )}
    </span>
  );
};