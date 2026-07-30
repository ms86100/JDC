const KEY = 'jdc.recent.plans';

export interface RecentPlanView {
  id: string;
  name: string;
  viewedAt: number;
}

export function getRecentPlanViews(): string[] {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return [];
    const list = JSON.parse(raw) as RecentPlanView[];
    return list.sort((a, b) => b.viewedAt - a.viewedAt).map((x) => x.id);
  } catch {
    return [];
  }
}

export function recordRecentPlanView(id: string, name: string): void {
  try {
    const raw = localStorage.getItem(KEY);
    let list: RecentPlanView[] = raw ? JSON.parse(raw) : [];
    list = list.filter((x) => x.id !== id);
    list.unshift({ id, name, viewedAt: Date.now() });
    localStorage.setItem(KEY, JSON.stringify(list.slice(0, 10)));
  } catch {
    /* ignore */
  }
}
