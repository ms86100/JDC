import { useQuery } from '@tanstack/react-query';
import boardApi from '../../api/boardApi';
import { defaultBoardPath, pickDefaultBoard } from './boardLinks';

export function useDefaultBoard(projectId: string | undefined) {
  const query = useQuery({
    queryKey: ['ws-default-board', projectId],
    queryFn: () => boardApi.getBoardsByProject(projectId!),
    enabled: !!projectId,
    staleTime: 60000,
  });

  const boards = query.data ?? [];
  const defaultBoard = pickDefaultBoard(boards);

  return {
    boards,
    defaultBoard,
    boardHref: defaultBoardPath(boards),
    isLoading: query.isLoading,
  };
}
