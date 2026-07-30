import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { issueApi, IssueResponse } from '../../../api/issueApi';
import { useAuth } from '../../auth/context/AuthContext';

export default function AssignedToMeGadget() {
  const { user } = useAuth();
  const { data: issues = [], isLoading } = useQuery<IssueResponse[]>({
    queryKey: ['gadget-assigned', user?.id],
    queryFn: async () => {
      const res = await issueApi.getAll();
      const content = res.data?.content ?? [];
      return content.filter(
        (i) => i.assigneeId === user?.id || i.assigneeName === user?.username,
      ).slice(0, 6);
    },
    enabled: !!user,
  });

  return (
    <section className="jdc-gadget" aria-label="Assigned to Me">
      <div className="jdc-gadget-header">
        <span>Assigned to Me</span>
      </div>
      <div className="jdc-gadget-body">
        {isLoading ? (
          <p>Loading…</p>
        ) : issues.length === 0 ? (
          <p style={{ color: 'var(--jdc-text-subtle)' }}>No issues assigned to you.</p>
        ) : (
          <table className="jdc-gadget-table">
            <thead>
              <tr>
                <th>T</th>
                <th>Key</th>
                <th>Summary</th>
                <th>P</th>
              </tr>
            </thead>
            <tbody>
              {issues.map((issue) => (
                <tr key={issue.id}>
                  <td>☑</td>
                  <td><Link to={`/issues/${issue.id}`}>{issue.issueKey}</Link></td>
                  <td>{issue.title}</td>
                  <td>{issue.priority ? '=' : ''}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        <p style={{ fontSize: 12, marginTop: 8, color: 'var(--jdc-text-subtle)' }}>
          1–{issues.length} of {issues.length}
        </p>
      </div>
    </section>
  );
}
