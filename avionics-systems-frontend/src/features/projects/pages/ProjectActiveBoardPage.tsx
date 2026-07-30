import { useMemo } from 'react';
import { useOutletContext, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import boardApi from '../../../api/boardApi';
import { sprintApi } from '../../../api/sprintApi';
import { pickDefaultBoard } from '../../../components/workspace/boardLinks';
import { ProjectResponse } from '../../../api/projectApi';
import EnhancedKanbanBoard from '../../boards/components/EnhancedKanbanBoard';

interface LayoutContext {
  project?: ProjectResponse;
  projectId?: string;
}

export default function ProjectActiveBoardPage() {
  const { projectId: paramId } = useParams<{ projectId: string }>();
  const ctx = useOutletContext<LayoutContext>();
  const projectId = paramId ?? ctx.projectId ?? ctx.project?.id ?? '';

  const { data: boards = [], isPending: boardsLoading } = useQuery({
    queryKey: ['ws-default-board', projectId],
    queryFn: () => boardApi.getBoardsByProject(projectId),
    enabled: !!projectId,
    retry: 1,
  });

  const { data: sprints = [] } = useQuery({
    queryKey: ['sprints', projectId],
    queryFn: () => sprintApi.getAll(projectId).catch(() => []),
    enabled: !!projectId,
    retry: 1,
  });

  const defaultBoard = useMemo(() => pickDefaultBoard(boards), [boards]);
  const activeSprint = useMemo(
    () => sprints.find((s) => s.status === 'ACTIVE'),
    [sprints],
  );

  if (!projectId) {
    return (
      <div className="sa-project-board-empty">
        <h3>Project not found</h3>
        <p>Select a valid project to open the board.</p>
      </div>
    );
  }

  if (boardsLoading) {
    return (
      <div className="sa-project-board-shell">
        <div className="ab-loading" style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div className="ab-spinner" />
        </div>
      </div>
    );
  }

  if (!defaultBoard) {
    return (
      <div className="sa-project-board-shell">
        <div className="sa-project-board-empty">
          <h3>No board for this project</h3>
          <p>Create a Scrum or Kanban board to use the active sprint view.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="sa-project-board-shell">
      <EnhancedKanbanBoard
        projectId={projectId}
        initialBoardId={defaultBoard.id}
        scrumActiveSprintMode
        lockActiveSprintId={activeSprint?.id ?? null}
        useIssueDrawer
      />
    </div>
  );
}
