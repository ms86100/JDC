import { useState } from 'react';
import {
  useBacklogPlanning,
  useBulkMoveIssues,
  useMoveToBacklog,
  BacklogPlanningResponse,
} from '../../hooks/useSprint';

interface SprintBacklogPlanningViewProps {
  boardId: string;
}

const stateBadgeColors: Record<string, string> = {
  FUTURE: '#3b82f6',
  ACTIVE: '#22c55e',
  CLOSED: '#6b7280',
};

export default function SprintBacklogPlanningView({ boardId }: SprintBacklogPlanningViewProps) {
  const { data, isLoading } = useBacklogPlanning(boardId);
  const bulkMoveIssues = useBulkMoveIssues();
  const moveToBacklog = useMoveToBacklog();

  const [collapsedSprints, setCollapsedSprints] = useState<Record<string, boolean>>({});
  const [selectedIssues, setSelectedIssues] = useState<string[]>([]);
  const [moveToSprintId, setMoveToSprintId] = useState<string>('');
  const [backlogCollapsed, setBacklogCollapsed] = useState(false);

  if (isLoading) {
    return (
      <div className="ab-backlog-planning-loading" style={{ padding: '32px', textAlign: 'center' }}>
        <div
          className="ab-spinner"
          style={{
            width: '32px',
            height: '32px',
            border: '3px solid var(--ab-color-border, #e2e8f0)',
            borderTopColor: 'var(--ab-color-primary, #3b82f6)',
            borderRadius: '50%',
            animation: 'ab-spin 0.8s linear infinite',
            margin: '0 auto 12px',
          }}
        />
        <p style={{ color: 'var(--ab-color-text-secondary, #64748b)', fontSize: '14px' }}>
          Loading backlog planning...
        </p>
        <style>{`@keyframes ab-spin { to { transform: rotate(360deg); } }`}</style>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="ab-backlog-planning-empty" style={{ padding: '32px', textAlign: 'center' }}>
        <p style={{ color: 'var(--ab-color-text-secondary, #64748b)', fontSize: '14px' }}>
          No backlog planning data available.
        </p>
      </div>
    );
  }

  const toggleSprint = (sprintId: string) => {
    setCollapsedSprints((prev) => ({
      ...prev,
      [sprintId]: !prev[sprintId],
    }));
  };

  const toggleIssueSelection = (planItemId: string) => {
    setSelectedIssues((prev) =>
      prev.includes(planItemId)
        ? prev.filter((id) => id !== planItemId)
        : [...prev, planItemId]
    );
  };

  const handleMoveToSprint = () => {
    if (!moveToSprintId || selectedIssues.length === 0) return;
    bulkMoveIssues.mutate(
      { sprintId: moveToSprintId, issueIds: selectedIssues, userId: '' },
      { onSuccess: () => setSelectedIssues([]) }
    );
  };

  const handleMoveToBacklog = () => {
    if (selectedIssues.length === 0) return;
    moveToBacklog.mutate(
      { planItemIds: selectedIssues, userId: '' },
      { onSuccess: () => setSelectedIssues([]) }
    );
  };

  return (
    <div className="ab-backlog-planning" style={{ padding: '16px' }}>
      {/* Action Bar */}
      {selectedIssues.length > 0 && (
        <div
          className="ab-backlog-planning-actions"
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            padding: '10px 16px',
            marginBottom: '16px',
            backgroundColor: 'var(--ab-color-surface, #f8fafc)',
            border: '1px solid var(--ab-color-border, #e2e8f0)',
            borderRadius: '8px',
          }}
        >
          <span
            style={{
              fontSize: '13px',
              color: 'var(--ab-color-text-secondary, #64748b)',
              fontWeight: 500,
            }}
          >
            {selectedIssues.length} selected
          </span>

          <select
            className="ab-select"
            value={moveToSprintId}
            onChange={(e) => setMoveToSprintId(e.target.value)}
            style={{
              padding: '6px 10px',
              fontSize: '13px',
              border: '1px solid var(--ab-color-border, #e2e8f0)',
              borderRadius: '6px',
              backgroundColor: '#fff',
              color: 'var(--ab-color-text, #1e293b)',
            }}
          >
            <option value="">Move to Sprint...</option>
            {data.sprintSections.map((sprint) => (
              <option key={sprint.sprintId} value={sprint.sprintId}>
                {sprint.sprintName}
              </option>
            ))}
          </select>

          <button
            className="ab-btn ab-btn-primary ab-btn-sm"
            onClick={handleMoveToSprint}
            disabled={!moveToSprintId}
            style={{
              padding: '6px 14px',
              fontSize: '13px',
              fontWeight: 500,
              border: 'none',
              borderRadius: '6px',
              backgroundColor: moveToSprintId
                ? 'var(--ab-color-primary, #3b82f6)'
                : 'var(--ab-color-border, #e2e8f0)',
              color: moveToSprintId ? '#fff' : 'var(--ab-color-text-secondary, #64748b)',
              cursor: moveToSprintId ? 'pointer' : 'not-allowed',
            }}
          >
            Move to Sprint
          </button>

          <button
            className="ab-btn ab-btn-secondary ab-btn-sm"
            onClick={handleMoveToBacklog}
            style={{
              padding: '6px 14px',
              fontSize: '13px',
              fontWeight: 500,
              border: '1px solid var(--ab-color-border, #e2e8f0)',
              borderRadius: '6px',
              backgroundColor: '#fff',
              color: 'var(--ab-color-text, #1e293b)',
              cursor: 'pointer',
            }}
          >
            Move to Backlog
          </button>
        </div>
      )}

      {/* Sprint Sections */}
      {data.sprintSections.map((sprint) => {
        const isCollapsed = collapsedSprints[sprint.sprintId] ?? false;
        const badgeColor = stateBadgeColors[sprint.sprintState] || '#6b7280';

        return (
          <div
            key={sprint.sprintId}
            className="ab-backlog-sprint-section"
            style={{
              marginBottom: '12px',
              border: '1px solid var(--ab-color-border, #e2e8f0)',
              borderRadius: '8px',
              overflow: 'hidden',
            }}
          >
            {/* Sprint Header */}
            <div
              className="ab-backlog-sprint-header"
              onClick={() => toggleSprint(sprint.sprintId)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                padding: '12px 16px',
                backgroundColor: 'var(--ab-color-surface, #f8fafc)',
                cursor: 'pointer',
                userSelect: 'none',
              }}
            >
              <span
                className="ab-collapse-icon"
                style={{
                  fontSize: '12px',
                  color: 'var(--ab-color-text-secondary, #64748b)',
                  transition: 'transform 0.2s',
                  transform: isCollapsed ? 'rotate(-90deg)' : 'rotate(0deg)',
                  display: 'inline-block',
                }}
              >
                &#9660;
              </span>

              <span
                className="ab-sprint-name"
                style={{
                  fontWeight: 600,
                  fontSize: '14px',
                  color: 'var(--ab-color-text, #1e293b)',
                }}
              >
                {sprint.sprintName}
              </span>

              <span
                className="ab-sprint-state"
                style={{
                  fontSize: '11px',
                  fontWeight: 600,
                  padding: '2px 8px',
                  borderRadius: '10px',
                  backgroundColor: badgeColor,
                  color: '#fff',
                  textTransform: 'uppercase',
                  letterSpacing: '0.5px',
                }}
              >
                {sprint.sprintState}
              </span>

              <span
                className="ab-sprint-meta"
                style={{
                  marginLeft: 'auto',
                  fontSize: '13px',
                  color: 'var(--ab-color-text-secondary, #64748b)',
                }}
              >
                {sprint.totalIssues} issues &middot; {sprint.totalPoints} points
              </span>
            </div>

            {/* Sprint Issues */}
            {!isCollapsed && (
              <div className="ab-backlog-sprint-issues" style={{ padding: '4px 0' }}>
                {sprint.issues.length === 0 ? (
                  <div
                    className="ab-backlog-sprint-empty"
                    style={{
                      padding: '16px',
                      textAlign: 'center',
                      color: 'var(--ab-color-text-secondary, #64748b)',
                      fontSize: '13px',
                    }}
                  >
                    No issues in this sprint.
                  </div>
                ) : (
                  sprint.issues.map((issue) => (
                    <div
                      key={issue.id}
                      className={`ab-backlog-issue ${selectedIssues.includes(issue.planItemId) ? 'ab-selected' : ''}`}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '10px',
                        padding: '8px 16px',
                        borderBottom: '1px solid var(--ab-color-border, #f1f5f9)',
                        backgroundColor: selectedIssues.includes(issue.planItemId)
                          ? 'var(--ab-color-selected, #eff6ff)'
                          : 'transparent',
                      }}
                    >
                      <input
                        type="checkbox"
                        className="ab-issue-checkbox"
                        checked={selectedIssues.includes(issue.planItemId)}
                        onChange={() => toggleIssueSelection(issue.planItemId)}
                        onClick={(e) => e.stopPropagation()}
                        style={{ cursor: 'pointer', accentColor: 'var(--ab-color-primary, #3b82f6)' }}
                      />
                      <span
                        className="ab-issue-id"
                        style={{
                          fontSize: '13px',
                          fontWeight: 500,
                          color: 'var(--ab-color-primary, #3b82f6)',
                          minWidth: '80px',
                        }}
                      >
                        {issue.issueId}
                      </span>
                      <span
                        className="ab-issue-status"
                        style={{
                          fontSize: '12px',
                          fontWeight: 500,
                          padding: '2px 8px',
                          borderRadius: '10px',
                          backgroundColor:
                            issue.completionStatus === 'COMPLETED'
                              ? '#dcfce7'
                              : issue.completionStatus === 'DROPPED'
                                ? '#fee2e2'
                                : '#f1f5f9',
                          color:
                            issue.completionStatus === 'COMPLETED'
                              ? '#16a34a'
                              : issue.completionStatus === 'DROPPED'
                                ? '#dc2626'
                                : '#64748b',
                        }}
                      >
                        {issue.completionStatus}
                      </span>
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        );
      })}

      {/* Backlog Section */}
      <div
        className="ab-backlog-section"
        style={{
          marginBottom: '12px',
          border: '1px solid var(--ab-color-border, #e2e8f0)',
          borderRadius: '8px',
          overflow: 'hidden',
        }}
      >
        <div
          className="ab-backlog-header"
          onClick={() => setBacklogCollapsed((prev) => !prev)}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            padding: '12px 16px',
            backgroundColor: 'var(--ab-color-surface, #f8fafc)',
            cursor: 'pointer',
            userSelect: 'none',
          }}
        >
          <span
            className="ab-collapse-icon"
            style={{
              fontSize: '12px',
              color: 'var(--ab-color-text-secondary, #64748b)',
              transition: 'transform 0.2s',
              transform: backlogCollapsed ? 'rotate(-90deg)' : 'rotate(0deg)',
              display: 'inline-block',
            }}
          >
            &#9660;
          </span>

          <span
            className="ab-backlog-name"
            style={{
              fontWeight: 600,
              fontSize: '14px',
              color: 'var(--ab-color-text, #1e293b)',
            }}
          >
            Backlog
          </span>

          <span
            className="ab-backlog-meta"
            style={{
              marginLeft: 'auto',
              fontSize: '13px',
              color: 'var(--ab-color-text-secondary, #64748b)',
            }}
          >
            {data.backlog.totalIssues} issues &middot; {data.backlog.totalPoints} points
          </span>
        </div>

        {!backlogCollapsed && (
          <div className="ab-backlog-items" style={{ padding: '4px 0' }}>
            {data.backlog.planItemIds.length === 0 ? (
              <div
                className="ab-backlog-empty"
                style={{
                  padding: '16px',
                  textAlign: 'center',
                  color: 'var(--ab-color-text-secondary, #64748b)',
                  fontSize: '13px',
                }}
              >
                No items in backlog.
              </div>
            ) : (
              data.backlog.planItemIds.map((planItemId) => (
                <div
                  key={planItemId}
                  className={`ab-backlog-issue ${selectedIssues.includes(planItemId) ? 'ab-selected' : ''}`}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '10px',
                    padding: '8px 16px',
                    borderBottom: '1px solid var(--ab-color-border, #f1f5f9)',
                    backgroundColor: selectedIssues.includes(planItemId)
                      ? 'var(--ab-color-selected, #eff6ff)'
                      : 'transparent',
                  }}
                >
                  <input
                    type="checkbox"
                    className="ab-issue-checkbox"
                    checked={selectedIssues.includes(planItemId)}
                    onChange={() => toggleIssueSelection(planItemId)}
                    style={{ cursor: 'pointer', accentColor: 'var(--ab-color-primary, #3b82f6)' }}
                  />
                  <span
                    className="ab-issue-id"
                    style={{
                      fontSize: '13px',
                      fontWeight: 500,
                      color: 'var(--ab-color-text-secondary, #64748b)',
                    }}
                  >
                    {planItemId}
                  </span>
                </div>
              ))
            )}
          </div>
        )}
      </div>
    </div>
  );
}
