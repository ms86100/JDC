import { IssueResponse } from '../../../api/issueApi';

/** LexoRank-style values for backlog ordering (lower = higher on backlog). */
export function rankForTop(): string {
  return `0|i${String(Date.now()).padStart(12, '0')}:`;
}

export function rankForBottom(): string {
  return `z|i${String(Date.now()).padStart(12, '0')}:`;
}

export function sortByRank(issues: IssueResponse[]): IssueResponse[] {
  return [...issues].sort((a, b) => {
    const ra = a.rank ?? 'm|';
    const rb = b.rank ?? 'm|';
    return ra.localeCompare(rb);
  });
}
