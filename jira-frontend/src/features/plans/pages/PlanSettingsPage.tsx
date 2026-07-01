import { useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { usePlan } from '../hooks/usePlans';
import { planApi } from '../../../api/planApi';
import { recordRecentPlanView } from '../utils/recentPlanViews';
import { appNotify } from '../../../lib/appNotify';
import '../styles/plan-settings.css';

type SettingsSection =
  | 'scheduling'
  | 'saved-views'
  | 'issue-sources'
  | 'exclusion'
  | 'removed'
  | 'custom-fields'
  | 'permissions'
  | 'scenarios';

const SECTION_LABELS: Record<SettingsSection, string> = {
  scheduling: 'Scheduling',
  'saved-views': 'Saved views',
  'issue-sources': 'Issue sources',
  exclusion: 'Exclusion',
  removed: 'Removed',
  'custom-fields': 'Custom fields',
  permissions: 'Permissions',
  scenarios: 'Scenarios',
};

export default function PlanSettingsPage() {
  const { planId } = useParams<{ planId: string }>();
  const [searchParams, setSearchParams] = useSearchParams();
  const section = (searchParams.get('section') as SettingsSection) || 'scheduling';
  const { data: plan, isLoading } = usePlan(planId || '');
  const queryClient = useQueryClient();

  const settings = (plan?.settings ?? {}) as Record<string, unknown>;
  const [estimation, setEstimation] = useState<string>((settings.estimation as string) ?? 'days');
  const [startField, setStartField] = useState<string>((settings.startField as string) ?? 'Target start');
  const [endField, setEndField] = useState<string>((settings.endField as string) ?? 'Target end');
  const [useSprintDates, setUseSprintDates] = useState<boolean>((settings.useSprintDates as boolean) ?? true);
  const [dependencyMode, setDependencyMode] = useState<string>((settings.dependencyMode as string) ?? 'sequential');
  const [savedViews, setSavedViews] = useState<Array<{ id: string; name: string }>>(
    (settings.savedViews as Array<{ id: string; name: string }>) ?? [{ id: 'basic', name: 'Basic' }],
  );
  const [newViewName, setNewViewName] = useState('');
  const [permissions, setPermissions] = useState({
    viewers: (settings.viewers as string) ?? 'Anyone with plan access',
    editors: (settings.editors as string) ?? 'Plan owner',
  });
  const [scenariosEnabled, setScenariosEnabled] = useState((settings.scenariosEnabled as boolean) ?? false);

  const [exField, setExField] = useState('status');
  const [exOp, setExOp] = useState('EQUALS');
  const [exValue, setExValue] = useState('Done');

  const { data: issueSources = [] } = useQuery({
    queryKey: ['plan-issue-sources', planId],
    queryFn: async () => (await planApi.getIssueSources(planId!)).data ?? [],
    enabled: !!planId && section === 'issue-sources',
  });

  const { data: exclusionRules = [] } = useQuery({
    queryKey: ['plan-exclusion', planId],
    queryFn: async () => (await planApi.getExclusionRules(planId!)).data ?? [],
    enabled: !!planId && section === 'exclusion',
  });

  if (plan && planId) {
    recordRecentPlanView(planId, plan.name);
  }

  const setSection = (s: SettingsSection) => setSearchParams({ section: s });

  const persistSettings = async (extra?: Record<string, unknown>) => {
    if (!planId) return;
    await planApi.updatePlanSettings(planId, {
      ...settings,
      estimation,
      startField,
      endField,
      useSprintDates,
      dependencyMode,
      savedViews,
      viewers: permissions.viewers,
      editors: permissions.editors,
      scenariosEnabled,
      ...extra,
    });
    await queryClient.invalidateQueries({ queryKey: ['plan', planId] });
  };

  const handleSave = async () => {
    await persistSettings();
    appNotify.success('Settings saved.');
  };

  const addSavedView = () => {
    if (!newViewName.trim()) return;
    setSavedViews((v) => [...v, { id: `view-${Date.now()}`, name: newViewName.trim() }]);
    setNewViewName('');
  };

  const addExclusionRule = async () => {
    if (!planId) return;
    await planApi.createExclusionRule(planId, {
      fieldName: exField,
      operator: exOp,
      fieldValue: exValue,
    });
    await queryClient.invalidateQueries({ queryKey: ['plan-exclusion', planId] });
  };

  if (isLoading || !plan) {
    return <div className="ab-loading"><div className="ab-spinner" /></div>;
  }

  const navBtn = (id: SettingsSection) => (
    <button
      key={id}
      type="button"
      className={`jdc-settings-nav-item ${section === id ? 'active' : ''}`}
      onClick={() => setSection(id)}
    >
      {SECTION_LABELS[id]}
    </button>
  );

  return (
    <div className="sa-plan-settings">
      <div className="jdc-settings-layout">
        <aside className="jdc-settings-sidebar" aria-label="Plan settings sections">
          <div className="sa-plan-settings-sidebar-head">
            <Link to={`/plans/${planId}`} className="sa-plan-settings-back">
              ← Back to plan
            </Link>
            <p className="sa-plan-settings-plan-name">{plan.name}</p>
            <p className="sa-plan-settings-subtitle">Plan settings</p>
          </div>
          <nav className="sa-plan-settings-nav">
            <div className="jdc-settings-nav-section">General</div>
            {navBtn('scheduling')}
            {navBtn('saved-views')}
            <div className="jdc-settings-nav-section">Source data</div>
            {navBtn('issue-sources')}
            {navBtn('exclusion')}
            {navBtn('removed')}
            {navBtn('custom-fields')}
            <div className="jdc-settings-nav-section">Access control</div>
            {navBtn('permissions')}
            <div className="jdc-settings-nav-section">Advanced</div>
            {navBtn('scenarios')}
          </nav>
        </aside>

        <main className="jdc-settings-main">
          <div className="sa-plan-settings-panel">
            {section === 'scheduling' && (
              <>
                <h2>Scheduling</h2>
                <h3>Estimation</h3>
                <div className="sa-plan-settings-radio-inline">
                  <label className="sa-plan-settings-radio">
                    <input type="radio" name="est" checked={estimation === 'days'} onChange={() => setEstimation('days')} />
                    Days
                  </label>
                  <label className="sa-plan-settings-radio">
                    <input type="radio" name="est" checked={estimation === 'hours'} onChange={() => setEstimation('hours')} />
                    Hours
                  </label>
                </div>
                <h3>Dates</h3>
                <div className="sa-plan-settings-field-row">
                  <div className="sa-plan-settings-field">
                    <label htmlFor="ps-start-field">Start date</label>
                    <select id="ps-start-field" value={startField} onChange={(e) => setStartField(e.target.value)}>
                      <option>Target start</option>
                      <option>Due date</option>
                    </select>
                  </div>
                  <div className="sa-plan-settings-field">
                    <label htmlFor="ps-end-field">End date</label>
                    <select id="ps-end-field" value={endField} onChange={(e) => setEndField(e.target.value)}>
                      <option>Target end</option>
                      <option>Due date</option>
                    </select>
                  </div>
                </div>
                <label className="sa-plan-settings-check">
                  <input type="checkbox" checked={useSprintDates} onChange={(e) => setUseSprintDates(e.target.checked)} />
                  Use sprint dates when issues lack start/end dates
                </label>
                <h3>Dependencies</h3>
                <div className="sa-plan-settings-radio-group">
                  <label className="sa-plan-settings-radio">
                    <input type="radio" name="dep" checked={dependencyMode === 'sequential'} onChange={() => setDependencyMode('sequential')} />
                    Sequential
                  </label>
                  <label className="sa-plan-settings-radio">
                    <input type="radio" name="dep" checked={dependencyMode === 'concurrent'} onChange={() => setDependencyMode('concurrent')} />
                    Concurrent
                  </label>
                </div>
              </>
            )}

            {section === 'saved-views' && (
              <>
                <h2>Saved views</h2>
                <ul>{savedViews.map((v) => <li key={v.id}>{v.name}</li>)}</ul>
                <div className="sa-plan-settings-field-row" style={{ marginTop: 16 }}>
                  <input
                    className="sa-plan-settings-field"
                    style={{ minWidth: 240 }}
                    value={newViewName}
                    onChange={(e) => setNewViewName(e.target.value)}
                    placeholder="New view name"
                  />
                  <button type="button" className="jdc-btn" onClick={addSavedView}>
                    Add view
                  </button>
                </div>
              </>
            )}

            {section === 'issue-sources' && (
              <>
                <h2>Issue sources</h2>
                <table className="jdc-settings-table">
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Type</th>
                      <th>Issues</th>
                    </tr>
                  </thead>
                  <tbody>
                    {issueSources.map((s) => (
                      <tr key={s.id}>
                        <td>{s.sourceName}</td>
                        <td>{s.sourceType}</td>
                        <td>{s.issueCount ?? 0}</td>
                      </tr>
                    ))}
                    {issueSources.length === 0 && (
                      <tr>
                        <td colSpan={3}>No issue sources configured. Add sources when creating the plan or via API.</td>
                      </tr>
                    )}
                  </tbody>
                </table>
                <p className="jdc-muted" style={{ marginTop: 12 }}>
                  Sources from plan creation are stored in settings; sync them here via the plan import job.
                </p>
              </>
            )}

            {section === 'exclusion' && (
              <>
                <h2>Exclusion rules</h2>
                <table className="jdc-settings-table">
                  <thead>
                    <tr>
                      <th>Field</th>
                      <th>Operator</th>
                      <th>Value</th>
                      <th />
                    </tr>
                  </thead>
                  <tbody>
                    {exclusionRules.map((r) => (
                      <tr key={r.id}>
                        <td>{r.fieldName}</td>
                        <td>{r.operator}</td>
                        <td>{r.fieldValue}</td>
                        <td>
                          <button
                            type="button"
                            className="jdc-btn"
                            onClick={() =>
                              planApi.deleteExclusionRule(planId!, r.id).then(() =>
                                queryClient.invalidateQueries({ queryKey: ['plan-exclusion', planId] }),
                              )
                            }
                          >
                            Remove
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <div className="sa-plan-settings-field-row" style={{ marginTop: 16 }}>
                  <input value={exField} onChange={(e) => setExField(e.target.value)} placeholder="Field" />
                  <select value={exOp} onChange={(e) => setExOp(e.target.value)}>
                    <option value="EQUALS">equals</option>
                    <option value="NOT_EQUALS">not equals</option>
                    <option value="CONTAINS">contains</option>
                  </select>
                  <input value={exValue} onChange={(e) => setExValue(e.target.value)} placeholder="Value" />
                  <button type="button" className="jdc-btn jdc-btn-primary" onClick={() => void addExclusionRule()}>
                    Add rule
                  </button>
                </div>
              </>
            )}

            {section === 'removed' && (
              <>
                <h2>Removed issues</h2>
                <p className="jdc-muted">Issues removed from the plan appear here. None removed.</p>
              </>
            )}

            {section === 'custom-fields' && (
              <>
                <h2>Custom fields</h2>
                <p className="jdc-muted">
                  Map custom fields for scheduling and roadmap columns (configure per project).
                </p>
              </>
            )}

            {section === 'permissions' && (
              <>
                <h2>Permissions</h2>
                <div className="sa-plan-settings-field" style={{ marginBottom: 16 }}>
                  <label htmlFor="ps-viewers">Who can view</label>
                  <input
                    id="ps-viewers"
                    style={{ width: '100%', maxWidth: 480 }}
                    value={permissions.viewers}
                    onChange={(e) => setPermissions((p) => ({ ...p, viewers: e.target.value }))}
                  />
                </div>
                <div className="sa-plan-settings-field">
                  <label htmlFor="ps-editors">Who can edit</label>
                  <input
                    id="ps-editors"
                    style={{ width: '100%', maxWidth: 480 }}
                    value={permissions.editors}
                    onChange={(e) => setPermissions((p) => ({ ...p, editors: e.target.value }))}
                  />
                </div>
              </>
            )}

            {section === 'scenarios' && (
              <>
                <h2>Scenarios</h2>
                <label className="sa-plan-settings-check">
                  <input
                    type="checkbox"
                    checked={scenariosEnabled}
                    onChange={(e) => setScenariosEnabled(e.target.checked)}
                  />
                  Enable scenario planning
                </label>
                <p className="jdc-muted" style={{ marginTop: 8 }}>
                  Compare alternative schedules without changing the committed plan.
                </p>
              </>
            )}

            <div className="sa-plan-settings-actions">
              <button type="button" className="jdc-btn jdc-btn-primary" onClick={() => void handleSave()}>
                Save
              </button>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}
