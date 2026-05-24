import { useEffect, useCallback, useRef, useState } from 'react';
import type { BoardIssue } from '../../../api/boardApi';
import type { BoardColumn } from '../../../api/boardApi';

interface UseKeyboardNavigationOptions {
  columns: BoardColumn[];
  issues: BoardIssue[];
  onIssueSelect: (issueId: string) => void;
  onIssueOpen: (issueId: string) => void;
  onDragStart: (issueId: string) => void;
  onDragEnd: () => void;
  onEscape: () => void;
}

interface FocusPosition {
  columnIndex: number;
  issueIndex: number;
}

export function useKeyboardNavigation({
  columns,
  issues,
  onIssueSelect,
  onIssueOpen,
  onDragStart,
  onDragEnd,
  onEscape,
}: UseKeyboardNavigationOptions) {
  const [focusPosition, setFocusPosition] = useState<FocusPosition>({
    columnIndex: 0,
    issueIndex: -1,
  });
  const [isDragging, setIsDragging] = useState(false);
  const containerRef = useRef<HTMLDivElement | null>(null);

  const getIssuesInColumn = useCallback(
    (columnIndex: number) => {
      const column = columns[columnIndex];
      if (!column) return [];
      return issues.filter((issue) => {
        const statusCategory = issue.statusCategory?.toUpperCase() || '';
        switch (column.statusCategory) {
          case 'TODO':
            return statusCategory === 'TODO' || statusCategory === 'BACKLOG';
          case 'IN_PROGRESS':
            return statusCategory === 'IN_PROGRESS';
          case 'DONE':
            return statusCategory === 'DONE';
          default:
            return true;
        }
      });
    },
    [columns, issues],
  );

  const getFocusedIssue = useCallback(() => {
    const issuesInColumn = getIssuesInColumn(focusPosition.columnIndex);
    if (focusPosition.issueIndex >= 0 && focusPosition.issueIndex < issuesInColumn.length) {
      return issuesInColumn[focusPosition.issueIndex];
    }
    return null;
  }, [focusPosition, getIssuesInColumn]);

  const moveFocus = useCallback(
    (direction: 'up' | 'down' | 'left' | 'right') => {
      setFocusPosition((prev) => {
        const issuesInCurrentColumn = getIssuesInColumn(prev.columnIndex);
        let newColumnIndex = prev.columnIndex;
        let newIssueIndex = prev.issueIndex;

        switch (direction) {
          case 'left':
            if (prev.columnIndex > 0) {
              newColumnIndex = prev.columnIndex - 1;
              const issuesInNewColumn = getIssuesInColumn(newColumnIndex);
              newIssueIndex = Math.min(prev.issueIndex, issuesInNewColumn.length - 1);
            }
            break;
          case 'right':
            if (prev.columnIndex < columns.length - 1) {
              newColumnIndex = prev.columnIndex + 1;
              const issuesInNewColumn = getIssuesInColumn(newColumnIndex);
              newIssueIndex = Math.min(prev.issueIndex, issuesInNewColumn.length - 1);
            }
            break;
          case 'up':
            if (prev.issueIndex > 0) {
              newIssueIndex = prev.issueIndex - 1;
            } else if (prev.issueIndex === 0 && prev.columnIndex > 0) {
              newColumnIndex = prev.columnIndex - 1;
              const issuesInNewColumn = getIssuesInColumn(newColumnIndex);
              newIssueIndex = issuesInNewColumn.length - 1;
            }
            break;
          case 'down':
            if (prev.issueIndex < issuesInCurrentColumn.length - 1) {
              newIssueIndex = prev.issueIndex + 1;
            } else if (prev.issueIndex === issuesInCurrentColumn.length - 1 && prev.columnIndex < columns.length - 1) {
              newColumnIndex = prev.columnIndex + 1;
              newIssueIndex = 0;
            }
            break;
        }

        return {
          columnIndex: newColumnIndex,
          issueIndex: Math.max(-1, newIssueIndex),
        };
      });
    },
    [columns.length, getIssuesInColumn],
  );

  const startDrag = useCallback(() => {
    const issue = getFocusedIssue();
    if (issue) {
      setIsDragging(true);
      onDragStart(issue.id);
    }
  }, [getFocusedIssue, onDragStart]);

  const endDrag = useCallback(() => {
    setIsDragging(false);
    onDragEnd();
  }, [onDragEnd]);

  const handleKeyDown = useCallback(
    (event: KeyboardEvent) => {
      const issue = getFocusedIssue();

      switch (event.key) {
        case 'ArrowUp':
          event.preventDefault();
          moveFocus('up');
          break;
        case 'ArrowDown':
          event.preventDefault();
          moveFocus('down');
          break;
        case 'ArrowLeft':
          event.preventDefault();
          moveFocus('left');
          break;
        case 'ArrowRight':
          event.preventDefault();
          moveFocus('right');
          break;
        case 'Enter':
          event.preventDefault();
          if (issue) {
            if (event.shiftKey) {
              startDrag();
            } else {
              onIssueOpen(issue.id);
            }
          }
          break;
        case ' ':
          event.preventDefault();
          if (isDragging) {
            endDrag();
          } else if (issue) {
            startDrag();
          }
          break;
        case 'Escape':
          event.preventDefault();
          if (isDragging) {
            endDrag();
          }
          onEscape();
          break;
        case 'Tab':
          if (issue) {
            event.preventDefault();
            if (event.shiftKey) {
              moveFocus('left');
            } else {
              moveFocus('right');
            }
          }
          break;
        default:
          break;
      }
    },
    [
      getFocusedIssue,
      moveFocus,
      onIssueOpen,
      startDrag,
      endDrag,
      onEscape,
      isDragging,
    ],
  );

  useEffect(() => {
    const container = containerRef.current;
    if (container) {
      container.addEventListener('keydown', handleKeyDown);
      container.setAttribute('tabindex', '0');
      return () => {
        container.removeEventListener('keydown', handleKeyDown);
      };
    }
  }, [handleKeyDown]);

  return {
    focusPosition,
    setFocusPosition,
    containerRef,
    isDragging,
    getFocusedIssue,
    moveFocus,
    startDrag,
    endDrag,
  };
}
