import type { BoardIssue } from '../../../api/boardApi';

/** Write drag payload (required for drop handlers — React state may clear before drop). */
export function writeDragPayload(e: React.DragEvent, issue: BoardIssue): void {
  e.dataTransfer.effectAllowed = 'move';
  try {
    e.dataTransfer.setData('application/json', JSON.stringify(issue));
  } catch {
    /* Some browsers restrict custom MIME types */
  }
  e.dataTransfer.setData('text/plain', issue.id);
}

/** Read dragged issue from the drop event, falling back to in-memory state. */
export function readDraggedIssue(
  e: React.DragEvent,
  fallback: BoardIssue | null,
): BoardIssue | null {
  // First, try to read from the drag event dataTransfer
  try {
    const raw = e.dataTransfer.getData('application/json');
    if (raw) {
      const parsed = JSON.parse(raw) as BoardIssue;
      if (parsed?.id) return parsed;
    }
  } catch {
    // ignore parse errors
  }

  // Try text/plain as fallback
  const id = e.dataTransfer.getData('text/plain');
  if (id && fallback?.id === id) return fallback;

  // If we have a fallback with an ID, return it
  if (fallback?.id) return fallback;

  // If we only have the ID from text/plain, return the fallback (even if null - let caller handle it)
  if (id) return fallback;

  return null;
}
