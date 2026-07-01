import { useQuery } from '@tanstack/react-query';
import { changeHistoryApi, ChangeHistoryResponse } from '../../../api/changeHistoryApi';
import {
  transitionHistoryApi,
  IssueTransitionHistoryEntry,
} from '../../../api/transitionHistoryApi';

interface ActivityTabProps {
  issueId: string;
}

const FIELD_LABELS: Record<string, string> = {
  title: 'Summary',
  description: 'Description',
  status: 'Status',
  priority: 'Priority',
  assigneeId: 'Assignee',
  reporterId: 'Reporter',
  issueType: 'Type',
  parentIssueId: 'Parent',
};

function getFieldIcon(field: string): string {
  switch (field) {
    case 'title':
    case 'description':
      return '📝';
    case 'status':
      return '🔄';
    case 'priority':
      return '⚡';
    case 'assigneeId':
      return '👤';
    case 'reporterId':
      return '📢';
    case 'issueType':
      return '📋';
    default:
      return '✏️';
  }
}

export default function ActivityTab({ issueId }: ActivityTabProps) {
  const { data: history, isLoading: historyLoading } = useQuery<ChangeHistoryResponse[]>({
    queryKey: ['change-history', issueId],
    queryFn: async () => {
      const response = await changeHistoryApi.getByIssue(issueId);
      return response.data;
    },
    enabled: !!issueId,
  });

  const { data: transitions, isLoading: transitionsLoading } = useQuery<IssueTransitionHistoryEntry[]>({
    queryKey: ['transition-history', issueId],
    queryFn: async () => {
      const response = await transitionHistoryApi.listByIssue(issueId);
      return response.data;
    },
    enabled: !!issueId,
  });

  const isLoading = historyLoading || transitionsLoading;

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`;
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;

    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined,
    });
  };

  const formatDateFull = (dateStr: string) => {
    return new Date(dateStr).toLocaleString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    });
  };

  if (isLoading) {
    return (
      <div className="ab-loading">
        <div className="ab-spinner"></div>
      </div>
    );
  }

  const hasHistory = history && history.length > 0;
  const hasTransitions = transitions && transitions.length > 0;

  if (!hasHistory && !hasTransitions) {
    return (
      <div className="ab-activity-tab ab-activity-tab--empty">
        <div className="ab-empty-state ab-empty-state--flat">
          <div className="ab-empty-state-icon" aria-hidden="true">📋</div>
          <p className="ab-empty-state-title">No activity yet</p>
          <p className="ab-empty-state-description">
            Changes and workflow transitions for this issue will appear here.
          </p>
        </div>
        <style>{activityStyles}</style>
      </div>
    );
  }

  return (
    <div className="ab-activity-tab">
      <div className="ab-activity-header">
        <h3>Activity</h3>
      </div>

      {hasTransitions && (
        <section className="ab-transition-history">
          <h4 className="ab-subsection-title">Workflow transitions</h4>
          <div className="ab-timeline ab-timeline-compact">
            {transitions.map((t) => (
              <div key={t.id} className="ab-timeline-item">
                <div className="ab-timeline-marker">
                  <div className={`ab-timeline-dot ${t.success === false ? 'ab-timeline-dot-error' : ''}`} />
                  <div className="ab-timeline-line" />
                </div>
                <div className="ab-timeline-content">
                  <div className="ab-change-item">
                    <span className="ab-change-icon">🔄</span>
                    <span className="ab-change-field">
                      {t.transitionName || 'Transition'}
                    </span>
                    {t.success === false && (
                      <span className="ab-change-new ab-change-error">Failed</span>
                    )}
                    <span
                      className="ab-timeline-time"
                      title={formatDateFull(t.executedAt)}
                    >
                      {formatDate(t.executedAt)}
                    </span>
                  </div>
                  {t.comment && (
                    <p className="ab-transition-comment">{t.comment}</p>
                  )}
                  {t.errorMessage && (
                    <p className="ab-transition-error">{t.errorMessage}</p>
                  )}
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {hasHistory && (
        <>
          {hasTransitions && <h4 className="ab-subsection-title">Field changes</h4>}
          <div className="ab-timeline">
        {history.map((entry) => (
          <div key={entry.id} className="ab-timeline-item">
            <div className="ab-timeline-marker">
              <div className="ab-timeline-dot"></div>
              <div className="ab-timeline-line"></div>
            </div>

            <div className="ab-timeline-content">
              <div className="ab-timeline-header">
                <div className="ab-timeline-avatar">
                  {(entry.authorName || 'U').charAt(0).toUpperCase()}
                </div>
                <div className="ab-timeline-meta">
                  <span className="ab-timeline-author">{entry.authorName || 'Unknown user'}</span>
                  <span
                    className="ab-timeline-time"
                    title={formatDateFull(entry.createdAt)}
                  >
                    {formatDate(entry.createdAt)}
                  </span>
                </div>
              </div>

              <div className="ab-timeline-changes">
                {entry.changes.map((change, idx) => (
                  <div key={idx} className="ab-change-item">
                    <span className="ab-change-icon">{getFieldIcon(change.field)}</span>
                    <span className="ab-change-field">
                      {FIELD_LABELS[change.field] || change.field}
                    </span>
                    {change.oldString && (
                      <>
                        <span className="ab-change-arrow">→</span>
                        <span className="ab-change-old">{change.oldString}</span>
                        <span className="ab-change-arrow">→</span>
                      </>
                    )}
                    <span className="ab-change-new">
                      {change.newString || (change.newValue ? `[${change.newValue}]` : 'None')}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        ))}
          </div>
        </>
      )}

      <style>{activityStyles}</style>
    </div>
  );
}

const activityStyles = `
  .ab-activity-tab {
    padding: 0;
  }

  .ab-activity-tab--empty .ab-empty-state--flat {
    border: none;
    background: transparent;
    box-shadow: none;
    padding: 32px 16px;
  }

  .ab-activity-header {
    margin-bottom: var(--ab-spacing-lg);
  }

  .ab-activity-header h3 {
    font-size: var(--ab-font-size-base);
    font-weight: 600;
    margin: 0;
  }

  .ab-timeline {
    position: relative;
  }

  .ab-timeline-item {
    display: flex;
    gap: var(--ab-spacing-md);
    padding-bottom: var(--ab-spacing-lg);
  }

  .ab-timeline-item:last-child {
    padding-bottom: 0;
  }

  .ab-timeline-marker {
    display: flex;
    flex-direction: column;
    align-items: center;
    flex-shrink: 0;
  }

  .ab-timeline-dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: var(--ab-primary-500);
    border: 2px solid var(--ab-white);
    box-shadow: 0 0 0 2px var(--ab-primary-200);
    margin-top: 6px;
  }

  .ab-timeline-line {
    flex: 1;
    width: 2px;
    background: var(--ab-gray-200);
    margin-top: var(--ab-spacing-xs);
  }

  .ab-timeline-item:last-child .ab-timeline-line {
    display: none;
  }

  .ab-timeline-content {
    flex: 1;
    background: var(--ab-white);
    border: 1px solid var(--ab-gray-200);
    border-radius: var(--ab-radius-md);
    padding: var(--ab-spacing-md);
  }

  .ab-timeline-header {
    display: flex;
    align-items: center;
    gap: var(--ab-spacing-sm);
    margin-bottom: var(--ab-spacing-sm);
  }

  .ab-timeline-avatar {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: var(--ab-primary-500);
    color: var(--ab-white);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: var(--ab-font-size-xs);
    font-weight: 600;
  }

  .ab-timeline-meta {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .ab-timeline-author {
    font-size: var(--ab-font-size-sm);
    font-weight: 500;
    color: var(--ab-gray-800);
  }

  .ab-timeline-time {
    font-size: var(--ab-font-size-xs);
    color: var(--ab-gray-400);
    cursor: help;
  }

  .ab-timeline-changes {
    display: flex;
    flex-direction: column;
    gap: var(--ab-spacing-xs);
    margin-top: var(--ab-spacing-sm);
    padding-top: var(--ab-spacing-sm);
    border-top: 1px solid var(--ab-gray-100);
  }

  .ab-change-item {
    display: flex;
    align-items: center;
    gap: var(--ab-spacing-xs);
    font-size: var(--ab-font-size-sm);
    flex-wrap: wrap;
  }

  .ab-change-icon {
    font-size: var(--ab-font-size-sm);
  }

  .ab-change-field {
    font-weight: 500;
    color: var(--ab-gray-700);
    min-width: 70px;
  }

  .ab-change-old {
    background: var(--ab-gray-100);
    padding: 2px 6px;
    border-radius: var(--ab-radius-sm);
    color: var(--ab-gray-500);
    text-decoration: line-through;
    max-width: 150px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .ab-change-arrow {
    color: var(--ab-gray-400);
    font-size: var(--ab-font-size-xs);
  }

  .ab-change-new {
    background: var(--ab-success-50);
    padding: 2px 6px;
    border-radius: var(--ab-radius-sm);
    color: var(--ab-success-700);
    font-weight: 500;
    max-width: 150px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .ab-subsection-title {
    font-size: var(--ab-font-size-sm);
    font-weight: 600;
    color: var(--ab-gray-600);
    margin: var(--ab-spacing-md) 0 var(--ab-spacing-sm);
  }

  .ab-transition-history {
    margin-bottom: var(--ab-spacing-lg);
  }

  .ab-timeline-dot-error {
    background: var(--ab-danger-500);
    box-shadow: 0 0 0 2px var(--ab-danger-200);
  }

  .ab-change-error {
    background: var(--ab-danger-50);
    color: var(--ab-danger-700);
  }

  .ab-transition-comment,
  .ab-transition-error {
    font-size: var(--ab-font-size-sm);
    margin: var(--ab-spacing-xs) 0 0;
    color: var(--ab-gray-600);
  }

  .ab-transition-error {
    color: var(--ab-danger-600);
  }
`;
