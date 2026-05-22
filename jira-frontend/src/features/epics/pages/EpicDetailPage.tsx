import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { epicApi, EpicProgressResponse } from '../../../api/epicApi';
import { issueApi, IssueResponse } from '../../../api/issueApi';
import { resolveIssueByKey } from '../../../api/issueLookup';

export default function EpicDetailPage() {
  const { epicId } = useParams<{ epicId: string }>();
  const queryClient = useQueryClient();
  const [linkIssueKey, setLinkIssueKey] = useState('');
  const [linkError, setLinkError] = useState('');

  const { data: epic, isLoading } = useQuery({
    queryKey: ['epic', epicId],
    queryFn: async () => {
      const res = await epicApi.getById(epicId!);
      return res.data;
    },
    enabled: !!epicId,
  });

  const { data: issueIds = [] } = useQuery({
    queryKey: ['epic-issues', epicId],
    queryFn: async () => {
      const res = await epicApi.getIssues(epicId!);
      return res.data;
    },
    enabled: !!epicId,
  });

  const { data: linkedIssues = [] } = useQuery({
    queryKey: ['epic-issue-details', issueIds],
    queryFn: async () => {
      if (issueIds.length === 0) return [];
      const res = await issueApi.getBatch(issueIds);
      return res.data;
    },
    enabled: issueIds.length > 0,
  });

  const { data: progressHistory = [] } = useQuery<EpicProgressResponse[]>({
    queryKey: ['epic-progress-history', epicId],
    queryFn: async () => {
      const res = await epicApi.getProgressHistory(epicId!);
      return res.data;
    },
    enabled: !!epicId,
  });

  const recalcMutation = useMutation({
    mutationFn: () => epicApi.recalculateProgress(epicId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['epic', epicId] });
      queryClient.invalidateQueries({ queryKey: ['epic-progress-history', epicId] });
    },
  });

  const linkMutation = useMutation({
    mutationFn: async (key: string) => {
      const found = await resolveIssueByKey(key);
      if (!found?.id) {
        throw new Error(`Issue not found: ${key}`);
      }
      await epicApi.addIssue(epicId!, found.id);
      return found;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['epic-issues', epicId] });
      queryClient.invalidateQueries({ queryKey: ['epic-issue-details'] });
      queryClient.invalidateQueries({ queryKey: ['epic', epicId] });
      setLinkIssueKey('');
      setLinkError('');
    },
    onError: (err: Error) => setLinkError(err.message),
  });

  const unlinkMutation = useMutation({
    mutationFn: (issueId: string) => epicApi.removeIssue(epicId!, issueId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['epic-issues', epicId] });
      queryClient.invalidateQueries({ queryKey: ['epic-issue-details'] });
      queryClient.invalidateQueries({ queryKey: ['epic', epicId] });
    },
  });

  if (isLoading || !epic) {
    return <div className="p-6 text-gray-500">Loading epic…</div>;
  }

  return (
    <div className="p-6 max-w-4xl mx-auto">
      <Link to="/epics" className="text-sm text-blue-600 hover:underline mb-4 inline-block">
        ← Back to epics
      </Link>

      <div className="flex items-start justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{epic.name}</h1>
          {epic.summary && <p className="text-gray-600 mt-1">{epic.summary}</p>}
          <span className="inline-block mt-2 text-xs font-medium px-2 py-1 rounded bg-gray-100">
            {epic.status}
          </span>
        </div>
        <button
          type="button"
          className="px-3 py-2 border rounded text-sm hover:bg-gray-50"
          onClick={() => recalcMutation.mutate()}
          disabled={recalcMutation.isPending}
        >
          Recalculate progress
        </button>
      </div>

      {epic.progressPercentage != null && (
        <div className="border rounded-lg p-4 mb-6 bg-white">
          <div className="flex justify-between text-sm mb-2">
            <span>Progress</span>
            <span className="font-semibold">{Number(epic.progressPercentage).toFixed(0)}%</span>
          </div>
          <div className="h-3 bg-gray-100 rounded-full overflow-hidden">
            <div
              className="h-full bg-blue-500"
              style={{ width: `${Math.min(100, Number(epic.progressPercentage))}%` }}
            />
          </div>
          <p className="text-xs text-gray-500 mt-2">
            {epic.completedIssueCount ?? 0} / {epic.totalIssueCount ?? 0} issues ·{' '}
            {epic.completedStoryPoints ?? 0} / {epic.totalStoryPoints ?? 0} story points
          </p>
        </div>
      )}

      {progressHistory.length > 0 && (
        <div className="border rounded-lg p-4 mb-6 bg-white">
          <h2 className="font-semibold text-sm mb-3">Progress history</h2>
          <ul className="space-y-2 text-sm">
            {progressHistory.map((entry, idx) => (
              <li key={idx} className="flex justify-between border-b pb-2 last:border-0">
                <span>
                  {entry.completedIssueCount ?? 0}/{entry.totalIssueCount ?? 0} issues
                </span>
                <span className="text-gray-600">
                  {entry.progressPercentage != null
                    ? `${Number(entry.progressPercentage).toFixed(0)}%`
                    : '—'}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {epic.description && (
        <div className="border rounded-lg p-4 mb-6 bg-white">
          <h2 className="font-semibold text-sm mb-2">Description</h2>
          <p className="text-sm text-gray-700 whitespace-pre-wrap">{epic.description}</p>
        </div>
      )}

      <div className="border rounded-lg p-4 bg-white">
        <h2 className="font-semibold mb-3">Linked issues</h2>
        <div className="flex gap-2 mb-2">
          <input
            className="flex-1 border rounded px-3 py-2 text-sm"
            placeholder="Issue key (e.g. PROJ-42)"
            value={linkIssueKey}
            onChange={(e) => {
              setLinkIssueKey(e.target.value);
              setLinkError('');
            }}
          />
          <button
            type="button"
            className="px-4 py-2 bg-blue-600 text-white rounded text-sm disabled:opacity-50"
            disabled={!linkIssueKey.trim() || linkMutation.isPending}
            onClick={() => linkMutation.mutate(linkIssueKey.trim())}
          >
            Link
          </button>
        </div>
        {linkError && <p className="text-sm text-red-600 mb-3">{linkError}</p>}
        {linkedIssues.length === 0 && issueIds.length === 0 ? (
          <p className="text-sm text-gray-500">No issues linked to this epic.</p>
        ) : (
          <ul className="divide-y">
            {(linkedIssues.length > 0
              ? linkedIssues
              : issueIds.map((id) => ({ id, issueKey: id, title: '' } as IssueResponse))
            ).map((issue) => (
              <li key={issue.id} className="flex items-center justify-between py-2 text-sm">
                <Link to={`/issues/${issue.id}`} className="text-blue-600 hover:underline">
                  <span className="font-medium">{issue.issueKey || issue.id}</span>
                  {issue.title ? ` — ${issue.title}` : ''}
                </Link>
                <button
                  type="button"
                  className="text-red-600 text-xs hover:underline"
                  onClick={() => unlinkMutation.mutate(issue.id)}
                >
                  Unlink
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
