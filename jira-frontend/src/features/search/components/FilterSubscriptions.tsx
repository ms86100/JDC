import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { filterApi, FilterSubscription } from '../../../api/filterApi';

interface FilterSubscriptionsProps {
  filterName: string;
  jql: string;
  onClose: () => void;
}

export default function FilterSubscriptions({ filterName, jql, onClose }: FilterSubscriptionsProps) {
  const queryClient = useQueryClient();
  const [frequency, setFrequency] = useState<'INSTANT' | 'DAILY' | 'WEEKLY'>('INSTANT');
  const [emailNotification, setEmailNotification] = useState(true);

  const { data: subscriptions, isLoading } = useQuery<FilterSubscription[]>({
    queryKey: ['filter-subscriptions'],
    queryFn: () => filterApi.getSubscriptions().then(res => res.data),
  });

  const createMutation = useMutation({
    mutationFn: () => filterApi.createSubscription({
      filterName,
      jql,
      frequency,
      emailNotification,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['filter-subscriptions'] });
      onClose();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => filterApi.deleteSubscription(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['filter-subscriptions'] });
    },
  });

  const toggleMutation = useMutation({
    mutationFn: (id: string) => filterApi.toggleSubscription(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['filter-subscriptions'] });
    },
  });

  const formatDate = (dateStr: string | undefined) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    });
  };

  const getFrequencyIcon = (freq: string) => {
    switch (freq) {
      case 'INSTANT': return '⚡';
      case 'DAILY': return '📅';
      case 'WEEKLY': return '📆';
      default: return '🔔';
    }
  };

  const currentFilterSubscriptions = subscriptions?.filter(s => s.filterName === filterName) || [];

  return (
    <div className="ab-subs-overlay" onClick={onClose}>
      <div className="ab-subs-panel" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="ab-subs-header">
          <h2>Subscribe to "{filterName}"</h2>
          <button className="ab-close-btn" onClick={onClose}>
            <svg width="20" height="20" viewBox="0 0 16 16" fill="currentColor">
              <path d="M4.646 4.646a.5.5 0 0 1 .708 0L8 7.293l2.646-2.647a.5.5 0 0 1 .708.708L8.707 8l2.647 2.646a.5.5 0 0 1-.708.708L8 8.707l-2.646 2.647a.5.5 0 0 1-.708-.708L7.293 8 4.646 5.354a.5.5 0 0 1 0-.708z"/>
            </svg>
          </button>
        </div>

        {/* Create Subscription Form */}
        <div className="ab-subs-form">
          <h3>New Subscription</h3>
          <div className="ab-form-group">
            <label className="ab-label">Notification Frequency</label>
            <div className="ab-frequency-options">
              {(['INSTANT', 'DAILY', 'WEEKLY'] as const).map((freq) => (
                <button
                  key={freq}
                  className={`ab-frequency-btn ${frequency === freq ? 'selected' : ''}`}
                  onClick={() => setFrequency(freq)}
                >
                  <span className="ab-freq-icon">{getFrequencyIcon(freq)}</span>
                  <span className="ab-freq-label">
                    {freq === 'INSTANT' ? 'Instant' : freq === 'DAILY' ? 'Daily Digest' : 'Weekly Digest'}
                  </span>
                </button>
              ))}
            </div>
          </div>

          <div className="ab-form-group">
            <label className="ab-checkbox-label">
              <input
                type="checkbox"
                checked={emailNotification}
                onChange={(e) => setEmailNotification(e.target.checked)}
              />
              <span>Send email notifications</span>
            </label>
          </div>

          <div className="ab-form-group">
            <label className="ab-label">JQL Preview</label>
            <div className="ab-jql-preview">{jql || '(all issues)'}</div>
          </div>

          <button
            className="ab-btn ab-btn-primary"
            onClick={() => createMutation.mutate()}
            disabled={createMutation.isPending}
          >
            {createMutation.isPending ? 'Creating...' : 'Subscribe'}
          </button>
        </div>

        {/* Existing Subscriptions */}
        {currentFilterSubscriptions.length > 0 && (
          <div className="ab-subs-list">
            <h3>Active Subscriptions</h3>
            {currentFilterSubscriptions.map((sub) => (
              <div key={sub.id} className="ab-sub-item">
                <div className="ab-sub-info">
                  <div className="ab-sub-header">
                    <span className="ab-sub-freq">
                      {getFrequencyIcon(sub.frequency)} {sub.frequency.charAt(0) + sub.frequency.slice(1).toLowerCase()}
                    </span>
                    <label className="ab-toggle">
                      <input
                        type="checkbox"
                        checked={sub.isActive}
                        onChange={() => toggleMutation.mutate(sub.id)}
                      />
                      <span className="ab-toggle-slider"></span>
                    </label>
                  </div>
                  <div className="ab-sub-meta">
                    {sub.emailNotification && <span>📧 Email enabled</span>}
                    {sub.lastNotified && <span>Last: {formatDate(sub.lastNotified)}</span>}
                  </div>
                </div>
                <button
                  className="ab-delete-btn"
                  onClick={() => deleteMutation.mutate(sub.id)}
                >
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                    <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6z"/>
                    <path d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1zM4.118 4L4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2.5 3V2h11v1h-11z"/>
                  </svg>
                </button>
              </div>
            ))}
          </div>
        )}

        <style>{`
          .ab-subs-overlay {
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.5);
            display: flex;
            justify-content: flex-end;
            z-index: 1100;
          }

          .ab-subs-panel {
            width: 420px;
            max-width: 100%;
            height: 100%;
            background: var(--ab-white);
            display: flex;
            flex-direction: column;
            animation: slideIn 0.2s ease-out;
          }

          @keyframes slideIn {
            from { transform: translateX(100%); }
            to { transform: translateX(0); }
          }

          .ab-subs-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: var(--ab-spacing-lg);
            border-bottom: 1px solid var(--ab-gray-200);
          }

          .ab-subs-header h2 {
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

          .ab-subs-form {
            padding: var(--ab-spacing-lg);
            border-bottom: 1px solid var(--ab-gray-200);
          }

          .ab-subs-form h3 {
            font-size: var(--ab-font-size-sm);
            font-weight: 600;
            color: var(--ab-gray-700);
            margin: 0 0 var(--ab-spacing-md);
          }

          .ab-frequency-options {
            display: flex;
            flex-direction: column;
            gap: var(--ab-spacing-sm);
          }

          .ab-frequency-btn {
            display: flex;
            align-items: center;
            gap: var(--ab-spacing-sm);
            padding: var(--ab-spacing-md);
            background: var(--ab-white);
            border: 2px solid var(--ab-gray-200);
            border-radius: var(--ab-radius-md);
            cursor: pointer;
            transition: all var(--ab-transition-fast);
          }

          .ab-frequency-btn:hover {
            border-color: var(--ab-primary-300);
          }

          .ab-frequency-btn.selected {
            border-color: var(--ab-primary-500);
            background: var(--ab-primary-50);
          }

          .ab-freq-icon { font-size: 18px; }

          .ab-freq-label {
            font-size: var(--ab-font-size-sm);
            font-weight: 500;
          }

          .ab-checkbox-label {
            display: flex;
            align-items: center;
            gap: var(--ab-spacing-sm);
            font-size: var(--ab-font-size-sm);
            cursor: pointer;
          }

          .ab-jql-preview {
            padding: var(--ab-spacing-sm);
            background: var(--ab-gray-50);
            border-radius: var(--ab-radius-sm);
            font-family: var(--ab-font-mono);
            font-size: var(--ab-font-size-xs);
            color: var(--ab-gray-600);
            word-break: break-all;
          }

          .ab-subs-list {
            flex: 1;
            overflow-y: auto;
            padding: var(--ab-spacing-lg);
          }

          .ab-subs-list h3 {
            font-size: var(--ab-font-size-sm);
            font-weight: 600;
            color: var(--ab-gray-700);
            margin: 0 0 var(--ab-spacing-md);
          }

          .ab-sub-item {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            padding: var(--ab-spacing-md);
            background: var(--ab-gray-50);
            border-radius: var(--ab-radius-md);
            margin-bottom: var(--ab-spacing-sm);
          }

          .ab-sub-info { flex: 1; }

          .ab-sub-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: var(--ab-spacing-xs);
          }

          .ab-sub-freq {
            font-size: var(--ab-font-size-sm);
            font-weight: 500;
            display: flex;
            align-items: center;
            gap: var(--ab-spacing-xs);
          }

          .ab-sub-meta {
            display: flex;
            gap: var(--ab-spacing-md);
            font-size: var(--ab-font-size-xs);
            color: var(--ab-gray-500);
          }

          .ab-toggle {
            position: relative;
            width: 36px;
            height: 20px;
          }

          .ab-toggle input {
            opacity: 0;
            width: 0;
            height: 0;
          }

          .ab-toggle-slider {
            position: absolute;
            cursor: pointer;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background-color: var(--ab-gray-300);
            transition: 0.2s;
            border-radius: 20px;
          }

          .ab-toggle-slider:before {
            position: absolute;
            content: "";
            height: 16px;
            width: 16px;
            left: 2px;
            bottom: 2px;
            background-color: white;
            transition: 0.2s;
            border-radius: 50%;
          }

          .ab-toggle input:checked + .ab-toggle-slider {
            background-color: var(--ab-primary-500);
          }

          .ab-toggle input:checked + .ab-toggle-slider:before {
            transform: translateX(16px);
          }

          .ab-delete-btn {
            background: none;
            border: none;
            cursor: pointer;
            color: var(--ab-gray-400);
            padding: var(--ab-spacing-xs);
          }

          .ab-delete-btn:hover { color: var(--ab-danger-500); }
        `}</style>
      </div>
    </div>
  );
}