import { useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useProgram } from '../hooks/usePlans';
import { planApi } from '../../../api/planApi';
import ProgramScheduleView from '../components/program/ProgramScheduleView';
import ProgramScopeView from '../components/program/ProgramScopeView';
import ProgramScheduleSettingsPopover from '../components/program/ProgramScheduleSettingsPopover';

type ProgramTab = 'schedule' | 'scope' | 'overview';

export default function ProgramDetailDcPage() {
  const { programId } = useParams<{ programId: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const tab = (searchParams.get('tab') as ProgramTab) || 'schedule';
  const setTab = (t: ProgramTab) => setSearchParams({ tab: t }, { replace: true });
  const queryClient = useQueryClient();
  const [lastSynced, setLastSynced] = useState('a few seconds ago');
  const [hierarchy, setHierarchy] = useState('Epic');
  const [scheduleSettingsOpen, setScheduleSettingsOpen] = useState(false);
  const [estimation, setEstimation] = useState('days');
  const [showDependencies, setShowDependencies] = useState(true);

  const { data: program, isLoading } = useProgram(programId || '');
  const { data: plans = [] } = useQuery({
    queryKey: ['program-plans', programId],
    queryFn: async () => (await planApi.getPlansByProgram(programId!)).data ?? [],
    enabled: !!programId,
  });

  const handleSync = () => {
    queryClient.invalidateQueries({ queryKey: ['program-plans', programId] });
    setLastSynced('just now');
  };

  if (isLoading) return <div className="ab-loading"><div className="ab-spinner" /></div>;
  if (!program) {
    return (
      <div className="ab-empty-state">
        <h3>Program not found</h3>
        <Link to="/programs">Back</Link>
      </div>
    );
  }

  return (
    <div className="jdc-program-page">
      <div style={{ padding: '16px 20px 0' }}>
        <p className="jdc-program-label">Program</p>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <h1 style={{ margin: 0, fontSize: 22, fontWeight: 500 }}>{program.name}</h1>
          <Link to={`/programs/${programId}/settings`} title="Configure program">⋯</Link>
          <div style={{ marginLeft: 'auto', display: 'flex', gap: 8, alignItems: 'center' }}>
            <span style={{ fontSize: 12, color: 'var(--jdc-text-subtle)' }}>Last synced {lastSynced}</span>
            <button type="button" className="jdc-btn" onClick={handleSync}>Sync</button>
            <button type="button" className="jdc-btn" onClick={() => navigator.clipboard?.writeText(window.location.href)}>Share</button>
            <Link to={`/programs/${programId}?tab=overview`} className="jdc-btn" style={{ textDecoration: 'none' }}>Portfolio view</Link>
          </div>
        </div>
        <div className="jdc-plan-tabs" style={{ marginTop: 12 }}>
          <button type="button" className={`jdc-plan-tab ${tab === 'schedule' ? 'active' : ''}`} onClick={() => setTab('schedule')}>Schedule</button>
          <button type="button" className={`jdc-plan-tab ${tab === 'scope' ? 'active' : ''}`} onClick={() => setTab('scope')}>Scope</button>
        </div>
      </div>

      {tab !== 'scope' && (
        <div className="jdc-program-toolbar">
          <button type="button" className="jdc-btn">{hierarchy} ▾</button>
          <span>🔽</span>
          <select className="jdc-btn" defaultValue="all"><option>Plans: All</option></select>
          <select className="jdc-btn" defaultValue="all"><option>Releases: All</option></select>
          <select className="jdc-btn" defaultValue="all"><option>Teams: All</option></select>
          <input type="search" placeholder="Filter by key, summary" style={{ flex: 1, maxWidth: 220, padding: '6px 8px' }} />
          <button type="button" className="jdc-btn" onClick={() => setScheduleSettingsOpen((o) => !o)}>⚙ Settings</button>
          <button type="button" className="jdc-btn">More ▾</button>
        </div>
      )}

      {scheduleSettingsOpen && tab === 'schedule' && (
        <ProgramScheduleSettingsPopover
          onClose={() => setScheduleSettingsOpen(false)}
          estimation={estimation}
          onEstimationChange={setEstimation}
          showDependencies={showDependencies}
          onShowDependenciesChange={setShowDependencies}
        />
      )}

      {tab === 'schedule' && programId && (
        <ProgramScheduleView programId={programId} hierarchy={hierarchy} onHierarchyChange={setHierarchy} />
      )}
      {tab === 'scope' && programId && (
        <ProgramScopeView programId={programId} onGetStarted={() => setTab('schedule')} />
      )}
      {tab === 'overview' && (
        <div style={{ padding: 20 }}>
          <p>{program.description || 'No description'}</p>
          <h3>Linked plans ({plans.length})</h3>
          <ul>
            {plans.map((p) => (
              <li key={p.id}><Link to={`/plans/${p.id}`}>{p.name}</Link></li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
