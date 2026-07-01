import { useMemo } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { sprintApi, SprintResponse } from '../../../api/sprintApi';

interface ScrumBoardToolbarProps {
  sprint: SprintResponse | null;
  projectId: string;
  issueCount: number;
  onSprintComplete?: () => void;
}

function daysRemaining(endDate?: string): number | null {
  if (!endDate) return null;
  const end = new Date(endDate);
  const now = new Date();
  return Math.ceil((end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
}

export default function ScrumBoardToolbar({
  sprint,
  projectId,
  issueCount,
  onSprintComplete,
}: ScrumBoardToolbarProps) {
  const queryClient = useQueryClient();
  const days = useMemo(() => daysRemaining(sprint?.endDate), [sprint?.endDate]);

  const completeMutation = useMutation({
    mutationFn: () => sprintApi.complete(sprint!.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints', projectId] });
      queryClient.invalidateQueries({ queryKey: ['board-data'] });
      queryClient.invalidateQueries({ queryKey: ['board-issues'] });
      onSprintComplete?.();
    },
  });

  if (!sprint) {
    return (
      <div className="jdc-scrum-board-toolbar">
        <div className="jdc-scrum-board-toolbar-left">
          <h2 className="jdc-scrum-sprint-title">No active sprint</h2>
          <span className="jdc-scrum-sprint-meta">Start a sprint from the backlog to work on the board.</span>
        </div>
      </div>
    );
  }

  return (
    <div className="jdc-scrum-board-toolbar">
      <div className="jdc-scrum-board-toolbar-left">
        <h2 className="jdc-scrum-sprint-title">{sprint.name}</h2>
        <span className="jdc-scrum-sprint-meta">
          {issueCount} issues
          {days !== null && (
            <> · {days > 0 ? `${days} days remaining` : days === 0 ? 'Ends today' : 'Sprint ended'}</>
          )}
          {sprint.goal && <> · {sprint.goal}</>}
        </span>
      </div>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
        <button
          type="button"
          className="jdc-btn jdc-btn-primary"
          disabled={completeMutation.isPending}
          onClick={() => {
            if (window.confirm(`Complete sprint "${sprint.name}"? Incomplete issues can be moved to backlog.`)) {
              completeMutation.mutate();
            }
          }}
        >
          {completeMutation.isPending ? 'Completing…' : 'Complete sprint'}
        </button>
      </div>
    </div>
  );
}
