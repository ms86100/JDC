import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { planApi } from '../../../../api/planApi';

interface ProgramScheduleViewProps {
  programId: string;
  hierarchy: string;
  onHierarchyChange: (h: string) => void;
}

function flattenIssues(
  agg: Awaited<ReturnType<typeof planApi.getProgramAggregation>>['data'],
  hierarchy: string,
) {
  const rows: Array<{
    planName: string;
    planId: string;
    key?: string;
    title?: string;
    type: string;
    targetDate?: string;
    targetEndDate?: string;
    status?: string;
  }> = [];
  if (!agg?.plans) return rows;
  for (const plan of agg.plans) {
    const byType = plan.issuesByType ?? {};
    for (const [type, issues] of Object.entries(byType)) {
      if (hierarchy === 'Epic' && type !== 'EPIC') continue;
      if (hierarchy === 'Story' && type !== 'STORY') continue;
      if (hierarchy === 'Initiative') continue;
      for (const issue of issues) {
        rows.push({
          planName: plan.planName,
          planId: plan.planId,
          key: issue.issueKey,
          title: issue.issueTitle,
          type,
          targetDate: issue.targetDate,
          targetEndDate: issue.targetEndDate,
          status: issue.status,
        });
      }
    }
  }
  return rows;
}

export default function ProgramScheduleView({
  programId,
  hierarchy,
  onHierarchyChange,
}: ProgramScheduleViewProps) {
  const { data: agg, isLoading } = useQuery({
    queryKey: ['program-aggregation', programId],
    queryFn: async () => (await planApi.getProgramAggregation(programId)).data,
    enabled: !!programId,
  });

  const rows = useMemo(() => flattenIssues(agg, hierarchy), [agg, hierarchy]);
  const hasIssues = rows.length > 0;

  if (isLoading) {
    return <div className="ab-loading"><div className="ab-spinner" /></div>;
  }

  return (
    <div className="jdc-program-schedule">
      <div className="jdc-program-schedule-controls">
        <label>
          Hierarchy:{' '}
          <select value={hierarchy} onChange={(e) => onHierarchyChange(e.target.value)}>
            <option value="Initiative">Initiative</option>
            <option value="Epic">Epic</option>
            <option value="Story">Story</option>
          </select>
        </label>
        <span className="jdc-muted">{agg?.planCount ?? 0} plans linked</span>
      </div>

      {!hasIssues ? (
        <div className="jdc-program-empty">
          <p style={{ fontSize: 48, margin: 0 }}>📋</p>
          <h3>Nothing to see here</h3>
          <p>
            The plans don&apos;t have issues at the <strong>{hierarchy}</strong> level.
            Check the other hierarchies to see some issues.
          </p>
        </div>
      ) : (
        <table className="jdc-program-schedule-grid">
          <thead>
            <tr>
              <th>Plan</th>
              <th>Key</th>
              <th>Summary</th>
              <th>Type</th>
              <th>Target start</th>
              <th>Target end</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, i) => (
              <tr key={`${row.planId}-${row.key}-${i}`}>
                <td><Link to={`/plans/${row.planId}`}>{row.planName}</Link></td>
                <td>{row.key ?? '—'}</td>
                <td>{row.title ?? '—'}</td>
                <td>{row.type}</td>
                <td>{row.targetDate?.slice(0, 10) ?? '—'}</td>
                <td>{row.targetEndDate?.slice(0, 10) ?? '—'}</td>
                <td><span className="jdc-lozenge">{row.status ?? 'TO DO'}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
