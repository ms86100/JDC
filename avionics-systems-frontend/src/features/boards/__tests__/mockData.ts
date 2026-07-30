import type { AgileBoard, BoardColumn, BoardIssue } from '../../../api/boardApi';

export function createMockBoard(overrides?: Partial<AgileBoard>): AgileBoard {
  return {
    id: 'board-1',
    name: 'Test Board',
    description: 'A test board for unit tests',
    type: 'kanban',
    cardLayout: 'FULL',
    ownerId: 'user-1',
    projectId: 'project-1',
    ...overrides,
  };
}

export function createMockColumn(overrides?: Partial<BoardColumn>): BoardColumn {
  return {
    id: 'col-1',
    name: 'To Do',
    color: '#0065ff',
    statusCategory: 'TODO',
    maxIssues: undefined,
    isHidden: false,
    ...overrides,
  };
}

export function createMockIssue(overrides?: Partial<BoardIssue>): BoardIssue {
  return {
    id: 'issue-1',
    issueKey: 'TEST-1',
    title: 'Test Issue',
    description: 'Test description',
    status: 'To Do',
    statusCategory: 'TODO',
    priority: 'Medium',
    issueType: 'Story',
    assignee: null,
    reporter: null,
    labels: [],
    epicId: null,
    epicName: null,
    epicColor: null,
    sprintId: null,
    sprintName: null,
    projectId: 'project-1',
    projectKey: 'TEST',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    dueDate: null,
    estimate: null,
    timeSpent: null,
    parentId: null,
    ...overrides,
  };
}

export function createMockUser() {
  return {
    userId: 'user-1',
    email: 'test@example.com',
    displayName: 'Test User',
    avatarUrl: 'https://example.com/avatar.png',
    isAdmin: false,
  };
}

export function createMockPermissions() {
  return {
    projectPermissions: {
      'project-1': {
        view: true,
        edit: true,
        admin: false,
      },
    },
    boardPermissions: {
      'board-1': {
        view: true,
        edit: true,
        admin: false,
      },
    },
  };
}

export const MOCK_BOARD_DATA = {
  board: createMockBoard(),
  columns: [
    createMockColumn({ id: 'col-1', name: 'To Do', statusCategory: 'TODO' }),
    createMockColumn({ id: 'col-2', name: 'In Progress', statusCategory: 'IN_PROGRESS' }),
    createMockColumn({ id: 'col-3', name: 'Done', statusCategory: 'DONE', isDone: true }),
  ],
  issues: [
    createMockIssue({ id: 'issue-1', issueKey: 'TEST-1' }),
    createMockIssue({ id: 'issue-2', issueKey: 'TEST-2', statusCategory: 'IN_PROGRESS' }),
    createMockIssue({ id: 'issue-3', issueKey: 'TEST-3', statusCategory: 'DONE' }),
  ],
};

export const MOCK_TRANSITIONS = [
  { id: 't1', name: 'To Do', to: { name: 'To Do', statusCategory: 'TODO' } },
  { id: 't2', name: 'In Progress', to: { name: 'In Progress', statusCategory: 'IN_PROGRESS' } },
  { id: 't3', name: 'Done', to: { name: 'Done', statusCategory: 'DONE' } },
];

export function mockApiResponse<T>(data: T, delay = 100): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(data), delay));
}

export function mockApiError(message: string, status = 500): Promise<never> {
  return Promise.reject(new Error(message));
}