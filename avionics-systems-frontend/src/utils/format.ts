/** Shared date/priority formatting for boards and issue UI. */

export function formatDate(dateStr: string | undefined | null): string {
  if (!dateStr) return '—';
  const d = new Date(dateStr);
  if (Number.isNaN(d.getTime())) return dateStr;
  return d.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

/** Emoji icon for issue priority (matches IssueCard). */
export function getPriorityIcon(priority: string | undefined): string {
  switch (priority?.toLowerCase()) {
    case 'critical':
    case 'highest':
      return '🔴';
    case 'high':
      return '🟠';
    case 'medium':
      return '🟡';
    case 'low':
    case 'lowest':
      return '🟢';
    default:
      return '⚪';
  }
}
