import React, { useEffect, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import boardApi from '../../../api/boardApi';
import { projectApi } from '../../../api/projectApi';
import { asArray } from '../../../utils/apiList';
import EnhancedKanbanBoard from '../components/EnhancedKanbanBoard';
import { useEnsureKanbanBoard } from '../hooks/useEnsureKanbanBoard';
import type { KanbanStatusBanner } from '../components/KanbanWorkspaceToolbar';
import '../../projects/styles/project-subpages.css';

function pickKanbanBoard(
  boards: Awaited<ReturnType<typeof boardApi.getBoardsByProject>>,
) {
  if (!boards?.length) return undefined;
  return (
    boards.find((b) => b.boardType === 'KANBAN') ??
    boards.find((b) => b.isDefault) ??
    boards[0]
  );
}

/**
 * Jira DC Kanban workspace — /kanban and /board/classic.
 */
export default function KanbanBoardPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const projectIdParam = searchParams.get('project') ?? '';
  const boardIdParam = searchParams.get('boardId') ?? '';

  const { data: projectsRaw, isLoading: projectsLoading } = useQuery({
    queryKey: ['classic-board-projects'],
    queryFn: () => projectApi.getAll({ size: 100 }),
    retry: 1,
  });

  const projects = useMemo(() => asArray(projectsRaw), [projectsRaw]);

  const resolvedProjectId = projectIdParam || projects[0]?.id || '';

  const { data: boards = [], isLoading: boardsLoading, refetch: refetchBoards } = useQuery({
    queryKey: ['classic-board-boards', resolvedProjectId],
    queryFn: () => boardApi.getBoardsByProject(resolvedProjectId),
    enabled: !!resolvedProjectId,
    retry: 1,
  });

  const selectedProject = projects.find((p) => p.id === resolvedProjectId);

  const { isProvisioning } = useEnsureKanbanBoard(
    resolvedProjectId,
    boards,
    boardsLoading,
    selectedProject?.name,
  );

  const defaultBoard = useMemo(() => pickKanbanBoard(boards), [boards]);
  const resolvedBoardId = boardIdParam || defaultBoard?.id || '';

  useEffect(() => {
    if (projectsLoading || !resolvedProjectId) return;
    const next = new URLSearchParams(searchParams);
    let changed = false;
    if (!projectIdParam) {
      next.set('project', resolvedProjectId);
      changed = true;
    }
    if (!boardIdParam && resolvedBoardId) {
      next.set('boardId', resolvedBoardId);
      changed = true;
    }
    if (changed) setSearchParams(next, { replace: true });
  }, [
    projectsLoading,
    projectIdParam,
    boardIdParam,
    resolvedProjectId,
    resolvedBoardId,
    setSearchParams,
    searchParams,
  ]);

  useEffect(() => {
    if (resolvedBoardId && boards.some((b) => b.id === resolvedBoardId)) return;
    if (defaultBoard?.id) refetchBoards();
  }, [isProvisioning, defaultBoard?.id, resolvedBoardId, boards, refetchBoards]);

  const onProjectChange = (projectId: string) => {
    const next = new URLSearchParams(searchParams);
    next.set('project', projectId);
    next.delete('boardId');
    setSearchParams(next, { replace: true });
  };

  const onBoardChange = (boardId: string) => {
    const next = new URLSearchParams(searchParams);
    next.set('boardId', boardId);
    setSearchParams(next, { replace: true });
  };

  const statusBanner: KanbanStatusBanner | null = useMemo(() => {
    if (isProvisioning) {
      return { tone: 'info', message: 'Creating your Kanban board for this project…' };
    }
    if (!resolvedBoardId && !boardsLoading) {
      return {
        tone: 'warn',
        message:
          'Board service is offline. You can still move issues by workflow; start sprint-service to save board settings.',
        actionLabel: 'Retry',
        onAction: () => refetchBoards(),
      };
    }
    return null;
  }, [isProvisioning, resolvedBoardId, boardsLoading, refetchBoards]);

  if (projectsLoading) {
    return (
      <div className="sa-project-board-shell sa-kanban-workspace">
        <div className="ab-loading" style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div className="ab-spinner" />
        </div>
      </div>
    );
  }

  if (!projects.length) {
    return (
      <div className="sa-project-board-shell sa-kanban-workspace">
        <div className="sa-project-board-empty">
          <h3>No projects yet</h3>
          <p>Create a project from the top bar, then return here to plan work on your Kanban board.</p>
        </div>
      </div>
    );
  }

  if (!resolvedProjectId) {
    return null;
  }

  return (
    <div className="sa-project-board-shell sa-kanban-workspace">
      {(boardsLoading || isProvisioning) && !resolvedBoardId ? (
        <div className="ab-loading" style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div className="ab-spinner" />
        </div>
      ) : (
        <EnhancedKanbanBoard
          projectId={resolvedProjectId}
          initialBoardId={resolvedBoardId || undefined}
          kanbanClassicMode
          unifiedWorkspace
          useIssueDrawer
          workspaceContext={{
            projects,
            projectId: resolvedProjectId,
            onProjectChange,
            boards,
            boardId: resolvedBoardId || undefined,
            onBoardChange: boards.length > 1 ? onBoardChange : undefined,
            statusBanner,
            onRetryBoard: refetchBoards,
          }}
        />
      )}
    </div>
  );
}
