import type { BoardColumn, BoardIssue } from '../../../api/boardApi';

/** Jira Data Center classic Kanban column layout */
export const KANBAN_DC_COLUMNS: BoardColumn[] = [
  {
    id: 'col-backlog',
    name: 'Backlog',
    sequence: 0,
    statusCategory: 'TODO',
    isDone: false,
    currentIssues: 0,
    color: '#6b778c',
    isCollapsible: true,
    isHidden: false,
  },
  {
    id: 'col-selected',
    name: 'Selected for Development',
    sequence: 1,
    statusCategory: 'TODO',
    isDone: false,
    currentIssues: 0,
    color: '#255fcc',
    isCollapsible: true,
    isHidden: false,
  },
  {
    id: 'col-inprogress',
    name: 'In Progress',
    sequence: 2,
    statusCategory: 'IN_PROGRESS',
    isDone: false,
    currentIssues: 0,
    color: '#ff8b00',
    isCollapsible: true,
    isHidden: false,
    maxIssues: 5,
  },
  {
    id: 'col-done',
    name: 'Done',
    sequence: 3,
    statusCategory: 'DONE',
    isDone: true,
    currentIssues: 0,
    color: '#36b37e',
    isCollapsible: true,
    isHidden: false,
  },
];

function norm(s: string): string {
  if (!s) return '';
  // Remove (legacy), (new), etc. and normalize
  return s.toLowerCase()
    .replace(/\(legacy\)/gi, '')
    .replace(/\(new\)/gi, '')
    .replace(/[\s\(\)]+/g, ' ')
    .trim()
    .replace(/[\s_-]+/g, '');
}

export function normalizeBoardStatus(s: string | undefined | null): string {
  if (!s) return '';
  return norm(s);
}

const COLUMN_STATUS_ALIASES: Record<string, string[]> = {
  backlog: ['backlog', 'open', 'new', 'draft', 'pending', 'defined'],
  selectedfordevelopment: ['todo', 'todevelopment', 'selectedfordevelopment', 'selected', 'ready', 'readyfordevelopment'],
  inprogress: ['inprogress', 'inreview', 'review', 'development', 'implementing'],
  done: ['done', 'closed', 'resolved', 'complete', 'completed', 'finished'],
};

function columnKey(column: BoardColumn): string {
  const n = norm(column.name);
  if (n.includes('backlog')) return 'backlog';
  if (n.includes('selected')) return 'selectedfordevelopment';
  if (n.includes('progress') || n.includes('review')) return 'inprogress';
  if (column.isDone || column.statusCategory === 'DONE') return 'done';
  if (column.statusCategory === 'IN_PROGRESS') return 'inprogress';
  return n;
}

/** Whether an issue belongs in a board column (DC status mapping). */
export function issueMatchesColumn(issue: BoardIssue, column: BoardColumn): boolean {
  const key = columnKey(column);
  const aliases = COLUMN_STATUS_ALIASES[key];
  const status = norm(issue.status ?? '');

  if (aliases) {
    return aliases.some((a) => status === a || status.includes(a));
  }

  if (column.statusCategory === 'TODO') {
    return status === 'todo' || status === 'backlog';
  }
  if (column.statusCategory === 'IN_PROGRESS') {
    return status.includes('progress') || status.includes('review');
  }
  if (column.statusCategory === 'DONE') {
    return status === 'done' || status === 'closed' || status.includes('complete');
  }

  return norm(column.name) === status;
}

/** Target workflow status label when dropping into a column. */
export function targetStatusForColumn(column: BoardColumn): string {
  const key = columnKey(column);
  switch (key) {
    case 'backlog':
      return column.name || 'Backlog';
    case 'selectedfordevelopment':
      return 'To Do';
    case 'inprogress':
      if (column.name.toLowerCase().includes('review')) return 'In Review';
      return column.name || 'In Progress';
    case 'done':
      return column.name || 'Done';
    default:
      return column.name;
  }
}
