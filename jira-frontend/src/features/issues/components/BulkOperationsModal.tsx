import React, { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { bulkApi, BulkOperationType, BulkOperationResponse } from '../../../api/bulkApi';
import { IssueResponse } from '../../../api/issueApi';

interface BulkOperationsModalProps {
  issues: IssueResponse[];
  onClose: () => void;
}

type OperationStep = 'select' | 'configure' | 'preview' | 'result';

const OPERATIONS: { type: BulkOperationType; label: string; icon: string; description: string }[] = [
  { type: 'UPDATE_STATUS', label: 'Change Status', icon: '🔄', description: 'Change the status of selected issues' },
  { type: 'UPDATE_FIELDS', label: 'Edit Fields', icon: '✏️', description: 'Update assignee, priority, or other fields' },
  { type: 'CLONE', label: 'Clone Issues', icon: '📋', description: 'Create copies of selected issues' },
  { type: 'MOVE_TO_SPRINT', label: 'Move to Sprint', icon: '🏃', description: 'Add issues to a sprint' },
  { type: 'ADD_LABELS', label: 'Add Labels', icon: '🏷️', description: 'Add labels to selected issues' },
  { type: 'DELETE', label: 'Delete Issues', icon: '🗑️', description: 'Permanently delete selected issues', danger: true },
];

const STATUSES = ['To Do', 'In Progress', 'In Review', 'Done', 'Blocked'];
const PRIORITIES = ['Highest', 'High', 'Medium', 'Low', 'Lowest'];

export default function BulkOperationsModal({ issues, onClose }: BulkOperationsModalProps) {
  const [step, setStep] = useState<OperationStep>('select');
  const [selectedOperation, setSelectedOperation] = useState<BulkOperationType | null>(null);
  const [config, setConfig] = useState({
    newStatus: '',
    assigneeId: '',
    priority: '',
    labels: '',
    sprintId: '',
  });

  const executeMutation = useMutation({
    mutationFn: (data: Parameters<typeof bulkApi.execute>[0]) => bulkApi.execute(data),
    onSuccess: () => setStep('result'),
  });

  const handleSelectOperation = (type: BulkOperationType) => {
    setSelectedOperation(type);
    setStep('configure');
  };

  const handleExecute = () => {
    if (!selectedOperation || issues.length === 0) return;

    executeMutation.mutate({
      issueIds: issues.map(i => i.id),
      operationType: selectedOperation,
      ...(selectedOperation === 'UPDATE_STATUS' && { newStatus: config.newStatus }),
      ...(selectedOperation === 'UPDATE_FIELDS' && {
        assigneeId: config.assigneeId,
        priority: config.priority,
      }),
      ...(selectedOperation === 'ADD_LABELS' && { labels: config.labels }),
      ...(selectedOperation === 'MOVE_TO_SPRINT' && { sprintId: config.sprintId }),
    });
  };

  const renderSelectStep = () => (
    <div className="ab-bulk-select">
      <h3>Select Operation</h3>
      <p className="ab-bulk-info">
        <strong>{issues.length}</strong> issue{issues.length !== 1 ? 's' : ''} selected
      </p>
      <div className="ab-operation-grid">
        {OPERATIONS.map((op) => (
          <button
            key={op.type}
            className={`ab-operation-card ${op.danger ? 'danger' : ''}`}
            onClick={() => handleSelectOperation(op.type)}
          >
            <span className="ab-op-icon">{op.icon}</span>
            <span className="ab-op-label">{op.label}</span>
            <span className="ab-op-desc">{op.description}</span>
          </button>
        ))}
      </div>
    </div>
  );

  const renderConfigureStep = () => {
    const getOperationLabel = () => OPERATIONS.find(o => o.type === selectedOperation)?.label || '';

    return (
      <div className="ab-bulk-configure">
        <h3>Configure: {getOperationLabel()}</h3>

        {selectedOperation === 'UPDATE_STATUS' && (
          <div className="ab-form-group">
            <label className="ab-label">New Status</label>
            <select
              className="ab-select"
              value={config.newStatus}
              onChange={(e) => setConfig({ ...config, newStatus: e.target.value })}
            >
              <option value="">Select status...</option>
              {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
        )}

        {selectedOperation === 'UPDATE_FIELDS' && (
          <>
            <div className="ab-form-group">
              <label className="ab-label">Assignee</label>
              <input
                type="text"
                className="ab-input"
                placeholder="Enter user ID or email..."
                value={config.assigneeId}
                onChange={(e) => setConfig({ ...config, assigneeId: e.target.value })}
              />
            </div>
            <div className="ab-form-group">
              <label className="ab-label">Priority</label>
              <select
                className="ab-select"
                value={config.priority}
                onChange={(e) => setConfig({ ...config, priority: e.target.value })}
              >
                <option value="">Select priority...</option>
                {PRIORITIES.map(p => <option key={p} value={p}>{p}</option>)}
              </select>
            </div>
          </>
        )}

        {selectedOperation === 'ADD_LABELS' && (
          <div className="ab-form-group">
            <label className="ab-label">Labels (comma separated)</label>
            <input
              type="text"
              className="ab-input"
              placeholder="frontend, bug, urgent"
              value={config.labels}
              onChange={(e) => setConfig({ ...config, labels: e.target.value })}
            />
          </div>
        )}

        {selectedOperation === 'MOVE_TO_SPRINT' && (
          <div className="ab-form-group">
            <label className="ab-label">Sprint</label>
            <select
              className="ab-select"
              value={config.sprintId}
              onChange={(e) => setConfig({ ...config, sprintId: e.target.value })}
            >
              <option value="">Select sprint...</option>
              <option value="sprint-1">Sprint 1</option>
              <option value="sprint-2">Sprint 2</option>
              <option value="sprint-3">Sprint 3</option>
            </select>
          </div>
        )}

        {selectedOperation === 'DELETE' && (
          <div className="ab-danger-warning">
            <span className="ab-warning-icon">⚠️</span>
            <p>
              You are about to delete <strong>{issues.length}</strong> issue{issues.length !== 1 ? 's' : ''}.
              This action cannot be undone.
            </p>
          </div>
        )}

        <div className="ab-config-actions">
          <button className="ab-btn ab-btn-secondary" onClick={() => setStep('select')}>
            Back
          </button>
          <button
            className="ab-btn ab-btn-primary"
            onClick={handleExecute}
            disabled={
              executeMutation.isPending ||
              (selectedOperation === 'UPDATE_STATUS' && !config.newStatus) ||
              (selectedOperation === 'UPDATE_FIELDS' && !config.assigneeId && !config.priority) ||
              (selectedOperation === 'ADD_LABELS' && !config.labels) ||
              (selectedOperation === 'MOVE_TO_SPRINT' && !config.sprintId)
            }
          >
            {executeMutation.isPending ? 'Processing...' : `Apply to ${issues.length} Issues`}
          </button>
        </div>
      </div>
    );
  };

  const renderResultStep = () => {
    const result = executeMutation.data;
    if (!result) return null;

    const getStatusColor = (status: string) => {
      switch (status) {
        case 'COMPLETED': return '#22c55e';
        case 'FAILED': return '#ef4444';
        case 'PARTIAL_SUCCESS': return '#f59e0b';
        default: return '#6b7280';
      }
    };

    return (
      <div className="ab-bulk-result">
        <div className="ab-result-summary" style={{ borderColor: getStatusColor(result.status) }}>
          <span className="ab-result-icon">
            {result.status === 'COMPLETED' ? '✅' : result.status === 'FAILED' ? '❌' : '⚠️'}
          </span>
          <div className="ab-result-stats">
            <span className="ab-result-status">{result.status.replace('_', ' ')}</span>
            <span className="ab-result-counts">
              {result.successCount} successful, {result.failedCount} failed
            </span>
          </div>
        </div>

        <div className="ab-result-list">
          <h4>Results</h4>
          <div className="ab-results-scroll">
            {result.results.slice(0, 20).map((r, i) => (
              <div key={i} className={`ab-result-item ${r.success ? 'success' : 'failed'}`}>
                <span className="ab-result-key">{r.issueKey}</span>
                <span className="ab-result-msg">{r.message}</span>
                {!r.success && <span className="ab-result-error">{r.errorCode}</span>}
              </div>
            ))}
            {result.results.length > 20 && (
              <p className="ab-more-results">
                And {result.results.length - 20} more results...
              </p>
            )}
          </div>
        </div>

        <div className="ab-result-actions">
          <button className="ab-btn ab-btn-secondary" onClick={() => {
            setStep('select');
            setConfig({ newStatus: '', assigneeId: '', priority: '', labels: '', sprintId: '' });
          }}>
            New Operation
          </button>
          <button className="ab-btn ab-btn-primary" onClick={onClose}>
            Done
          </button>
        </div>
      </div>
    );
  };

  return (
    <div className="ab-bulk-overlay" onClick={onClose}>
      <div className="ab-bulk-modal" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="ab-bulk-header">
          <h2>Bulk Operations</h2>
          <button className="ab-close-btn" onClick={onClose}>
            <svg width="20" height="20" viewBox="0 0 16 16" fill="currentColor">
              <path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
            </svg>
          </button>
        </div>

        {/* Content */}
        <div className="ab-bulk-content">
          {step === 'select' && renderSelectStep()}
          {step === 'configure' && renderConfigureStep()}
          {step === 'result' && renderResultStep()}
        </div>

        <style>{`
          .ab-bulk-overlay {
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.5);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 1100;
          }

          .ab-bulk-modal {
            background: var(--ab-white);
            border-radius: var(--ab-radius-lg);
            width: 600px;
            max-width: 90%;
            max-height: 80vh;
            display: flex;
            flex-direction: column;
          }

          .ab-bulk-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: var(--ab-spacing-lg);
            border-bottom: 1px solid var(--ab-gray-200);
          }

          .ab-bulk-header h2 {
            margin: 0;
            font-size: var(--ab-font-size-lg);
            font-weight: 600;
          }

          .ab-close-btn {
            background: none;
            border: none;
            cursor: pointer;
            color: var(--ab-gray-500);
          }

          .ab-bulk-content {
            flex: 1;
            overflow-y: auto;
            padding: var(--ab-spacing-lg);
          }

          .ab-bulk-info {
            font-size: var(--ab-font-size-sm);
            color: var(--ab-gray-600);
            margin: 0 0 var(--ab-spacing-md);
          }

          .ab-operation-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: var(--ab-spacing-sm);
          }

          .ab-operation-card {
            display: flex;
            flex-direction: column;
            align-items: flex-start;
            padding: var(--ab-spacing-md);
            background: var(--ab-white);
            border: 2px solid var(--ab-gray-200);
            border-radius: var(--ab-radius-md);
            cursor: pointer;
            transition: all var(--ab-transition-fast);
            text-align: left;
          }

          .ab-operation-card:hover {
            border-color: var(--ab-primary-300);
            background: var(--ab-primary-50);
          }

          .ab-operation-card.danger:hover {
            border-color: var(--ab-danger-300);
            background: var(--ab-danger-50);
          }

          .ab-op-icon { font-size: 20px; margin-bottom: var(--ab-spacing-xs); }
          .ab-op-label { font-weight: 600; font-size: var(--ab-font-size-sm); }
          .ab-op-desc { font-size: var(--ab-font-size-xs); color: var(--ab-gray-500); margin-top: var(--ab-spacing-xs); }

          .ab-danger-warning {
            padding: var(--ab-spacing-md);
            background: var(--ab-danger-50);
            border: 1px solid var(--ab-danger-200);
            border-radius: var(--ab-radius-md);
            display: flex;
            gap: var(--ab-spacing-md);
          }

          .ab-warning-icon { font-size: 24px; }
          .ab-danger-warning p { margin: 0; font-size: var(--ab-font-size-sm); color: var(--ab-danger-700); }

          .ab-config-actions {
            display: flex;
            justify-content: flex-end;
            gap: var(--ab-spacing-sm);
            margin-top: var(--ab-spacing-lg);
            padding-top: var(--ab-spacing-md);
            border-top: 1px solid var(--ab-gray-200);
          }

          .ab-result-summary {
            display: flex;
            align-items: center;
            gap: var(--ab-spacing-md);
            padding: var(--ab-spacing-lg);
            border: 2px solid;
            border-radius: var(--ab-radius-md);
            margin-bottom: var(--ab-spacing-lg);
          }

          .ab-result-icon { font-size: 32px; }
          .ab-result-stats { display: flex; flex-direction: column; }
          .ab-result-status { font-size: var(--ab-font-size-lg); font-weight: 600; }
          .ab-result-counts { font-size: var(--ab-font-size-sm); color: var(--ab-gray-600); }

          .ab-results-scroll {
            max-height: 200px;
            overflow-y: auto;
            background: var(--ab-gray-50);
            border-radius: var(--ab-radius-md);
            padding: var(--ab-spacing-sm);
          }

          .ab-result-item {
            display: flex;
            align-items: center;
            gap: var(--ab-spacing-sm);
            padding: var(--ab-spacing-xs);
            font-size: var(--ab-font-size-sm);
          }

          .ab-result-key {
            font-family: var(--ab-font-mono);
            font-weight: 500;
            min-width: 80px;
          }

          .ab-result-msg { flex: 1; color: var(--ab-gray-600); }
          .ab-result-error { color: var(--ab-danger-600); font-size: var(--ab-font-size-xs); }
          .ab-result-item.failed { color: var(--ab-danger-600); }
          .ab-result-item.success { color: var(--ab-gray-700); }

          .ab-more-results {
            text-align: center;
            font-size: var(--ab-font-size-sm);
            color: var(--ab-gray-500);
            margin: var(--ab-spacing-sm) 0 0;
          }

          .ab-result-actions {
            display: flex;
            justify-content: flex-end;
            gap: var(--ab-spacing-sm);
            margin-top: var(--ab-spacing-lg);
          }
        `}</style>
      </div>
    </div>
  );
}