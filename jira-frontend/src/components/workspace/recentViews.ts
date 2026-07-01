const STORAGE_KEY = 'sa.recent.views';
const MAX_ITEMS = 8;

export interface RecentView {
  id: string;
  type: 'project' | 'program';
  name: string;
  path: string;
  viewedAt: string;
}

function read(): RecentView[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as RecentView[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function write(items: RecentView[]): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items.slice(0, MAX_ITEMS)));
  } catch {
    /* ignore */
  }
}

export function recordRecentView(entry: Omit<RecentView, 'viewedAt'>): void {
  const items = read().filter((i) => !(i.type === entry.type && i.id === entry.id));
  write([{ ...entry, viewedAt: new Date().toISOString() }, ...items]);
}

export function getRecentViews(type?: RecentView['type']): RecentView[] {
  const items = read();
  return type ? items.filter((i) => i.type === type) : items;
}
