import type { BoardColumn, BoardIssue, AgileBoard } from '../../../api/boardApi';

export interface BoardState {
  board: AgileBoard | null;
  columns: BoardColumn[];
  issues: BoardIssue[];
  isLoading: boolean;
  error: string | null;
  lastUpdated: Date | null;
}

export interface BoardActions {
  moveIssue: (issueId: string, toColumnId: string, toIndex: number) => Promise<void>;
  reorderIssue: (issueId: string, toIndex: number) => Promise<void>;
  createIssue: (data: Partial<BoardIssue>) => Promise<BoardIssue>;
  updateIssue: (issueId: string, data: Partial<BoardIssue>) => Promise<void>;
  deleteIssue: (issueId: string) => Promise<void>;
  addColumn: (column: Partial<BoardColumn>) => Promise<void>;
  updateColumn: (columnId: string, data: Partial<BoardColumn>) => Promise<void>;
  removeColumn: (columnId: string) => Promise<void>;
}

const initialState: BoardState = {
  board: null,
  columns: [],
  issues: [],
  isLoading: false,
  error: null,
  lastUpdated: null,
};

export function createBoardReducer() {
  return function boardReducer(state: BoardState, action: { type: string; payload?: unknown }): BoardState {
    switch (action.type) {
      case 'LOAD_START':
        return { ...state, isLoading: true, error: null };
      case 'LOAD_SUCCESS':
        return {
          ...state,
          isLoading: false,
          board: (action.payload as { board: AgileBoard }).board,
          columns: (action.payload as { columns: BoardColumn[] }).columns,
          issues: (action.payload as { issues: BoardIssue[] }).issues,
          lastUpdated: new Date(),
        };
      case 'LOAD_ERROR':
        return { ...state, isLoading: false, error: (action.payload as string) };
      case 'ISSUE_MOVED':
        return { ...state, lastUpdated: new Date() };
      case 'ISSUE_CREATED':
        return {
          ...state,
          issues: [...state.issues, action.payload as BoardIssue],
          lastUpdated: new Date(),
        };
      case 'ISSUE_UPDATED':
        return {
          ...state,
          issues: state.issues.map((i) =>
            i.id === (action.payload as { id: string }).id ? { ...i, ...action.payload } : i,
          ),
          lastUpdated: new Date(),
        };
      case 'ISSUE_DELETED':
        return {
          ...state,
          issues: state.issues.filter((i) => i.id !== action.payload),
          lastUpdated: new Date(),
        };
      case 'COLUMN_ADDED':
        return {
          ...state,
          columns: [...state.columns, action.payload as BoardColumn],
          lastUpdated: new Date(),
        };
      case 'COLUMN_UPDATED':
        return {
          ...state,
          columns: state.columns.map((c) =>
            c.id === (action.payload as { id: string }).id
              ? { ...c, ...(action.payload as Partial<BoardColumn>) }
              : c,
          ),
          lastUpdated: new Date(),
        };
      case 'COLUMN_REMOVED':
        return {
          ...state,
          columns: state.columns.filter((c) => c.id !== action.payload),
          lastUpdated: new Date(),
        };
      default:
        return state;
    }
  };
}

export function validateBoardOperation(
  state: BoardState,
  operation: 'move' | 'create' | 'update' | 'delete',
  data?: { issueId?: string; columnId?: string },
): { valid: boolean; error?: string } {
  switch (operation) {
    case 'move': {
      const targetColumn = state.columns.find((c) => c.id === data?.columnId);
      if (targetColumn?.maxIssues && targetColumn.isHidden === false) {
        const issuesInColumn = state.issues.filter(
          (i) => (i as unknown as { columnId?: string }).columnId === data?.columnId,
        );
        if (issuesInColumn.length >= targetColumn.maxIssues) {
          return { valid: false, error: `Column "${targetColumn.name}" has reached its WIP limit` };
        }
      }
      return { valid: true };
    }
    case 'create': {
      if (!state.board) {
        return { valid: false, error: 'No active board' };
      }
      return { valid: true };
    }
    case 'update':
    case 'delete': {
      if (!data?.issueId) {
        return { valid: false, error: 'Issue ID required' };
      }
      const issue = state.issues.find((i) => i.id === data.issueId);
      if (!issue) {
        return { valid: false, error: 'Issue not found' };
      }
      return { valid: true };
    }
    default:
      return { valid: true };
  }
}

export function getColumnIssueCount(state: BoardState, columnId: string): number {
  return state.issues.filter((issue) => {
    const statusCategory = issue.statusCategory?.toUpperCase() || 'TODO';
    const column = state.columns.find((c) => c.id === columnId);
    if (!column) return false;
    switch (column.statusCategory) {
      case 'TODO':
        return statusCategory === 'TODO' || statusCategory === 'BACKLOG';
      case 'IN_PROGRESS':
        return statusCategory === 'IN_PROGRESS';
      case 'DONE':
        return statusCategory === 'DONE';
      default:
        return false;
    }
  }).length;
}

export function reorderIssues(
  issues: BoardIssue[],
  sourceIndex: number,
  destinationIndex: number,
): BoardIssue[] {
  const result = Array.from(issues);
  const [removed] = result.splice(sourceIndex, 1);
  result.splice(destinationIndex, 0, removed);
  return result;
}