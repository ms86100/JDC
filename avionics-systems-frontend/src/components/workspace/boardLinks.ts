import type { AgileBoard } from '../../api/boardApi';

/** Canonical board route — EnhancedKanbanBoard reads `boardId` from search params */
export function boardPath(boardId: string): string {
  return `/boards?boardId=${encodeURIComponent(boardId)}`;
}

export function boardsListPath(): string {
  return '/boards';
}

/** Prefer explicit default flag, then SCRUM, then first board */
export function pickDefaultBoard(boards: AgileBoard[] | null | undefined): AgileBoard | undefined {
  if (!boards?.length) return undefined;
  return (
    boards.find((b) => b.isDefault) ??
    boards.find((b) => b.boardType === 'SCRUM') ??
    boards.find((b) => b.boardType === 'KANBAN') ??
    boards[0]
  );
}

export function defaultBoardPath(boards: AgileBoard[]): string {
  const board = pickDefaultBoard(boards);
  return board ? boardPath(board.id) : boardsListPath();
}
