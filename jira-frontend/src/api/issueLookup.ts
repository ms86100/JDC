import { issueApi, IssueResponse } from './issueApi';

export async function resolveIssueByKey(issueKey: string): Promise<IssueResponse | null> {
  const key = issueKey.trim();
  if (!key) return null;
  try {
    const res = await issueApi.getByKey(key);
    return res.data;
  } catch {
    return null;
  }
}
