import { useEffect, useRef } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import boardApi from '../../../api/boardApi';

/**
 * Ensures a KANBAN agile board exists for the project (Jira DC: every software project has a board).
 */
export function useEnsureKanbanBoard(
  projectId: string | undefined,
  boards: { id: string; boardType?: string }[],
  boardsLoading: boolean,
  projectName?: string,
) {
  const queryClient = useQueryClient();
  const attemptedRef = useRef<string | null>(null);

  const createMutation = useMutation({
    mutationFn: async () => {
      if (!projectId) throw new Error('No project');
      return boardApi.createBoard({
        name: projectName ? `${projectName} Kanban` : 'Kanban board',
        projectId,
        boardType: 'KANBAN',
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['classic-board-boards', projectId] });
      queryClient.invalidateQueries({ queryKey: ['project-boards-pick', projectId] });
    },
  });

  useEffect(() => {
    if (!projectId || boardsLoading || boards.length > 0) return;
    if (attemptedRef.current === projectId) return;
    attemptedRef.current = projectId;
    createMutation.mutate();
  }, [projectId, boardsLoading, boards.length]);

  return {
    isProvisioning: createMutation.isPending,
    provisionError: createMutation.error,
  };
}
