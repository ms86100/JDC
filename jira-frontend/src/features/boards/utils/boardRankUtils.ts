/** LexoRank-style rank strings used by issue-service (rank|NNNNNNNNN). */
export function rankForIndex(index: number): string {
  return `rank|${String((index + 1) * 1000).padStart(9, '0')}`;
}

export function sortIssuesByRank<T extends { rank?: string }>(issues: T[]): T[] {
  return [...issues].sort((a, b) => {
    const ra = a.rank ?? '';
    const rb = b.rank ?? '';
    return ra.localeCompare(rb);
  });
}
