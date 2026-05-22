import { useQuery } from '@tanstack/react-query';
import { planApi } from '../../../../api/planApi';

interface ProgramScopeViewProps {
  programId: string;
  onGetStarted: () => void;
}

export default function ProgramScopeView({ programId, onGetStarted }: ProgramScopeViewProps) {
  const { data: initiatives = [], isLoading } = useQuery({
    queryKey: ['program-initiatives', programId],
    queryFn: async () => (await planApi.getInitiativesByProgram(programId)).data ?? [],
    enabled: !!programId,
  });

  if (isLoading) {
    return <div className="ab-loading"><div className="ab-spinner" /></div>;
  }

  if (initiatives.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: 64 }}>
        <p style={{ fontSize: 64, margin: 0, opacity: 0.25 }}>📊</p>
        <h2>Plan work on a whole new level</h2>
        <p style={{ color: 'var(--jdc-text-subtle)' }}>Visualize higher-level work using initiatives</p>
        <button type="button" className="jdc-btn jdc-btn-primary" onClick={onGetStarted}>
          Get started
        </button>
      </div>
    );
  }

  return (
    <div className="jdc-program-scope" style={{ padding: 24 }}>
      <h3 style={{ marginTop: 0 }}>Initiatives</h3>
      <table className="jdc-program-schedule-grid">
        <thead>
          <tr>
            <th>Name</th>
            <th>Status</th>
            <th>Target date</th>
            <th>Epics</th>
          </tr>
        </thead>
        <tbody>
          {initiatives.map((init) => (
            <tr key={init.id}>
              <td><strong>{init.name}</strong>{init.description && <div className="jdc-muted">{init.description}</div>}</td>
              <td><span className="jdc-lozenge">{init.status ?? 'OPEN'}</span></td>
              <td>{init.targetDate?.slice(0, 10) ?? '—'}</td>
              <td>{init.epicCount ?? 0}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
