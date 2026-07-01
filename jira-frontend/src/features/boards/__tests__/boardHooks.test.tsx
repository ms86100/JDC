import { renderHook, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useBoardWebSocket } from '../hooks/useBoardWebSocket';
import { useBoardPermissions } from '../hooks/useBoardPermissions';
import { useKeyboardNavigation } from '../hooks/useKeyboardNavigation';
import { useBoardErrorHandler } from '../hooks/useBoardErrorHandler';

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('useBoardWebSocket', () => {
  it('should initialize with disconnected state', () => {
    const { result } = renderHook(
      () => useBoardWebSocket({ boardId: 'test-board' }),
      { wrapper: createWrapper() },
    );
    expect(result.current.isConnected).toBe(false);
  });

  it('should not connect when disabled', () => {
    const { result } = renderHook(
      () => useBoardWebSocket({ boardId: 'test-board', enabled: false }),
      { wrapper: createWrapper() },
    );
    expect(result.current.isConnected).toBe(false);
  });
});

describe('useBoardErrorHandler', () => {
  it('should start with no errors', () => {
    const { result } = renderHook(() => useBoardErrorHandler());
    expect(result.current.errors).toHaveLength(0);
    expect(result.current.hasErrors).toBe(false);
  });

  it('should add error', () => {
    const { result } = renderHook(() => useBoardErrorHandler());
    const errorId = result.current.addError('Test error', 'network');
    expect(result.current.errors).toHaveLength(1);
    expect(errorId).toBeTruthy();
  });

  it('should remove error', () => {
    const { result } = renderHook(() => useBoardErrorHandler());
    const errorId = result.current.addError('Test error', 'network');
    act(() => {
      result.current.removeError(errorId);
    });
    expect(result.current.errors).toHaveLength(0);
  });

  it('should auto-remove errors after timeout', async () => {
    jest.useFakeTimers();
    const { result } = renderHook(() => useBoardErrorHandler({}));
    result.current.addError('Test error', 'network');
    expect(result.current.errors).toHaveLength(1);
    act(() => {
      jest.advanceTimersByTime;
    });
    expect(result.current.errors).toHaveLength(0);
    jest.useRealTimers();
  });
});

describe('useKeyboardNavigation', () => {
  const mockColumns = [
    { id: 'col1', name: 'To Do', statusCategory: 'TODO' },
    { id: 'col2', name: 'In Progress', statusCategory: 'IN_PROGRESS' },
    { id: 'col3', name: 'Done', statusCategory: 'DONE' },
  ];

  const mockIssues = [
    { id: 'issue1', issueKey: 'TEST-1', statusCategory: 'TODO' },
    { id: 'issue2', issueKey: 'TEST-2', statusCategory: 'TODO' },
    { id: 'issue3', issueKey: 'TEST-3', statusCategory: 'IN_PROGRESS' },
  ];

  it('should initialize with no focus', () => {
    const { result } = renderHook(() =>
      useKeyboardNavigation({
        columns: mockColumns,
        issues: mockIssues,
        onIssueSelect: jest.fn(),
        onIssueOpen: jest.fn(),
        onDragStart: jest.fn(),
        onDragEnd: jest.fn(),
        onEscape: jest.fn(),
      }),
    );
    expect(result.current.focusPosition.issueIndex).toBe(-1);
  });
});

describe('useBoardPermissions', () => {
  it('should require authentication context', () => {
    expect(() => {
      renderHook(() =>
        useBoardPermissions({ boardId: 'test-board' }),
      );
    }).toThrow();
  });
});

export function createMockBoardState() {
  return {
    board: {
      id: 'board-1',
      name: 'Test Board',
      type: 'kanban' as const,
      cardLayout: 'FULL' as const,
    },
    columns: [
      { id: 'col1', name: 'To Do', color: '#0065ff', statusCategory: 'TODO' },
      { id: 'col2', name: 'In Progress', color: '#ff8b00', statusCategory: 'IN_PROGRESS' },
      { id: 'col3', name: 'Done', color: '#00875a', statusCategory: 'DONE', isDone: true },
    ],
    issues: [
      { id: 'issue1', issueKey: 'TEST-1', title: 'Test Issue 1', statusCategory: 'TODO' },
      { id: 'issue2', issueKey: 'TEST-2', title: 'Test Issue 2', statusCategory: 'IN_PROGRESS' },
    ],
    isLoading: false,
    error: null,
    lastUpdated: new Date(),
  };
}

export function createMockDragEvent() {
  return {
    dataTransfer: {
      setData: jest.fn(),
      effectAllowed: 'move',
      getData: jest.fn(),
    },
    preventDefault: jest.fn(),
    stopPropagation: jest.fn(),
  } as unknown as React.DragEvent;
}