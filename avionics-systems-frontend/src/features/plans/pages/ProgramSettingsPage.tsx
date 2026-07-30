import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useProgram } from '../hooks/usePlans';
import { planApi } from '../../../api/planApi';
import { asArray } from '../../../utils/apiList';

export default function ProgramSettingsPage() {
  const { programId } = useParams<{ programId: string }>();
  const { data: program } = useProgram(programId || '');
  const queryClient = useQueryClient();
  const [section, setSection] = useState<'connected-plans' | 'custom-fields' | 'permissions'>('connected-plans');

  const { data: plans = [] } = useQuery({
    queryKey: ['all-plans-settings'],
    queryFn: async () => {
      const res = await planApi.getPlans();
      return asArray(res.data);
    },
  });

  const { data: linked = [] } = useQuery({
    queryKey: ['program-plans', programId],
    queryFn: async () => asArray((await planApi.getPlansByProgram(programId!)).data),
    enabled: !!programId,
  });

  const linkedIds = new Set(linked.map((p) => p.id));

  const togglePlan = async (planId: string, checked: boolean) => {
    if (!programId) return;
    if (checked) await planApi.linkPlanToProgram(programId, planId);
    else await planApi.unlinkPlanFromProgram(programId, planId);
    await queryClient.invalidateQueries({ queryKey: ['program-plans', programId] });
  };

  if (!program) return <div className="ab-loading"><div className="ab-spinner" /></div>;

  return (
    <div className="jdc-settings-layout">
      <aside className="jdc-settings-sidebar">
        <Link to={`/programs/${programId}`} style={{ padding: '0 16px 12px', fontSize: 13, display: 'block' }}>
          ← Back to program
        </Link>
        <p style={{ padding: '0 16px', fontWeight: 600 }}>Configure {program.name}</p>
        <div className="jdc-settings-nav-section">Source data</div>
        <button type="button" className={`jdc-settings-nav-item ${section === 'connected-plans' ? 'active' : ''}`} onClick={() => setSection('connected-plans')}>Connected plans</button>
        <button type="button" className={`jdc-settings-nav-item ${section === 'custom-fields' ? 'active' : ''}`} onClick={() => setSection('custom-fields')}>Custom fields</button>
        <div className="jdc-settings-nav-section">Access control</div>
        <button type="button" className={`jdc-settings-nav-item ${section === 'permissions' ? 'active' : ''}`} onClick={() => setSection('permissions')}>Permissions</button>
      </aside>
      <div className="jdc-settings-main">
        {section === 'connected-plans' && (
          <>
            <h2>Plans</h2>
            <p>Connected plans ({linked.length} selected)</p>
            <ul style={{ listStyle: 'none', padding: 0 }}>
              {plans.map((p) => (
                <li key={p.id} style={{ marginBottom: 8 }}>
                  <label>
                    <input
                      type="checkbox"
                      checked={linkedIds.has(p.id)}
                      onChange={(e) => togglePlan(p.id, e.target.checked)}
                    />{' '}
                    {p.name}
                  </label>
                </li>
              ))}
            </ul>
            <div style={{ marginTop: 16, padding: 12, background: '#deebff', border: '1px dashed var(--jdc-border)' }}>
              <strong>Can&apos;t find the plan you&apos;re looking for?</strong>
              <p style={{ fontSize: 13, margin: '8px 0 0' }}>You may not have permissions, or the plan may be in another program.</p>
            </div>
            <button type="button" className="jdc-btn jdc-btn-primary" style={{ marginTop: 16 }}>Save</button>
          </>
        )}
        {section !== 'connected-plans' && <p>Configure {section}.</p>}
      </div>
    </div>
  );
}
