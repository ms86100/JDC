import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { issueApi, IssueResponse } from '../../../../api/issueApi';
import { rankForBottom, rankForTop } from '../../../issues/utils/issueRank';

interface Props {
  issue: IssueResponse;
  projectId: string;
  siblingIssues: IssueResponse[];
}

export default function BacklogIssueRow({ issue, projectId, siblingIssues }: Props) {
  const queryClient = useQueryClient();
  const [menuOpen, setMenuOpen] = useState(false);

  const rankMutation = useMutation({
    mutationFn: (rank: string) => issueApi.update(issue.id, { rank }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['backlog-issues', projectId] });
      setMenuOpen(false);
    },
  });

  return (
    <div className="jdc-backlog-issue-row">
      <Link to={`/projects/${projectId}/issues/${issue.id}`} className="jdc-backlog-issue-link">
        <span className="jdc-issue-type-icon" aria-hidden>●</span>
        <span className="jdc-backlog-issue-key">{issue.issueKey}</span>
        <span className="jdc-backlog-issue-title">{issue.title}</span>
        <span className="jdc-badge">{issue.status ?? 'To Do'}</span>
      </Link>
      <div className="idc-dropdown-wrapper">
        <button
          type="button"
          className="jdc-btn jdc-btn-secondary jdc-btn-sm"
          onClick={() => setMenuOpen(!menuOpen)}
          aria-label="Issue actions"
        >
          ···
        </button>
        {menuOpen && (
          <div className="idc-dropdown-menu" style={{ right: 0, left: 'auto' }}>
            <button
              type="button"
              className="idc-dropdown-item"
              disabled={rankMutation.isPending}
              onClick={() => rankMutation.mutate(rankForTop())}
            >
              Rank to top
            </button>
            <button
              type="button"
              className="idc-dropdown-item"
              disabled={rankMutation.isPending}
              onClick={() => rankMutation.mutate(rankForBottom())}
            >
              Rank to bottom
            </button>
            <Link
              to={`/issues/${issue.id}`}
              className="idc-dropdown-item"
              style={{ display: 'block', textDecoration: 'none' }}
              onClick={() => setMenuOpen(false)}
            >
              Open issue
            </Link>
          </div>
        )}
      </div>
    </div>
  );
}
