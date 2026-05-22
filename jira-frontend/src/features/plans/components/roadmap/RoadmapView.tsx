import { useMemo, useState, useCallback, useRef } from 'react';
import { Link } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { useBacklog } from '../../hooks/useBacklog';
import { usePlanDraftChanges } from '../../hooks/usePlanDraftChanges';
import type { PlanItemResponse, PlanResponse } from '../../../../api/planApi';
import { planApi } from '../../../../api/planApi';
import { issueApi } from '../../../../api/issueApi';
import RoadmapToolbar from './RoadmapToolbar';
import RoadmapTimeline from './RoadmapTimeline';
import RoadmapFiltersPanel from './RoadmapFiltersPanel';
import RoadmapViewSettings from './RoadmapViewSettings';
import AutoSchedulePanel from './AutoSchedulePanel';
import PlanActionBar from './PlanActionBar';
import PlanCreateIssueModal from './PlanCreateIssueModal';
import { appNotify } from '../../../../lib/appNotify';
import '../../styles/plan-roadmap.css';

interface RoadmapViewProps {
  plan: PlanResponse;
}

type RowKind = 'group' | 'epic' | 'story' | 'item';

interface ScopeRow {
  key: string;
  kind: RowKind;
  label?: string;
  item?: PlanItemResponse;
  depth: number;
}

const HIERARCHY_RANK: Record<string, number> = {
  Initiative: 0,
  Epic: 1,
  Story: 2,
  'Sub-task': 3,
};

function issueIcon(type: string) {
  if (type === 'EPIC') return '⚡';
  if (type === 'STORY') return '📗';
  return '✓';
}

export default function RoadmapView({ plan }: RoadmapViewProps) {
  const planId = plan.id;
  const { data: backlog, isLoading } = useBacklog(planId);
  const draft = usePlanDraftChanges(planId);
  const queryClient = useQueryClient();

  const [zoom, setZoom] = useState<'3M' | '1Y' | 'Fit'>('3M');
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [viewSettingsOpen, setViewSettingsOpen] = useState(false);
  const [autoScheduleOpen, setAutoScheduleOpen] = useState(false);
  const [showFullHierarchy, setShowFullHierarchy] = useState(true);
  const [sortBy, setSortBy] = useState('Rank (default)');
  const [textFilter, setTextFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('All');
  const [hierarchyFrom, setHierarchyFrom] = useState('Epic');
  const [hierarchyTo, setHierarchyTo] = useState('Sub-task');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [inlineCreate, setInlineCreate] = useState(false);
  const [inlineName, setInlineName] = useState('');
  const [inlineSaving, setInlineSaving] = useState(false);
  const [showCreateIssue, setShowCreateIssue] = useState(false);
  const [scopeWidthPct, setScopeWidthPct] = useState(48);
  const [scheduleBanner, setScheduleBanner] = useState<string | null>(null);
  const splitRef = useRef<HTMLDivElement>(null);

  const settings = (plan.settings ?? {}) as Record<string, unknown>;
  const issueSources = (settings.issueSources as Array<{ id: string; type: string }>) ?? [];
  const defaultProjectId = issueSources.find((s) => s.type === 'project')?.id;

  const itemMap = useMemo(() => {
    const m = new Map<string, PlanItemResponse>();
    backlog?.items.forEach((i) => m.set(i.id, i));
    return m;
  }, [backlog]);

  const scopeRows = useMemo((): ScopeRow[] => {
    if (!backlog?.items.length) return [];
    let items = [...backlog.items];

    const fromRank = HIERARCHY_RANK[hierarchyFrom] ?? 1;
    const toRank = HIERARCHY_RANK[hierarchyTo] ?? 3;
    items = items.filter((i) => {
      const level =
        i.issueType === 'EPIC' ? 1 : i.issueType === 'STORY' ? 2 : i.issueType === 'SUBTASK' ? 3 : 2;
      return level >= fromRank && level <= toRank;
    });

    if (textFilter.trim()) {
      const q = textFilter.toLowerCase();
      items = items.filter(
        (i) =>
          (i.issueKey ?? '').toLowerCase().includes(q) ||
          (i.summary ?? '').toLowerCase().includes(q),
      );
    }
    if (statusFilter !== 'All') {
      items = items.filter((i) => (i.status ?? 'TO DO').toUpperCase().includes(statusFilter));
    }

    if (sortBy !== 'Rank (default)') {
      items.sort((a, b) => (a.summary ?? '').localeCompare(b.summary ?? ''));
    }

    const epics = items.filter((i) => i.issueType === 'EPIC');
    const orphans = items.filter((i) => !i.parentId && i.issueType !== 'EPIC');
    const rows: ScopeRow[] = [];

    epics.forEach((epic) => {
      rows.push({ key: `epic-${epic.id}`, kind: 'epic', item: epic, depth: 0 });
      items
        .filter((c) => c.parentId === epic.id)
        .forEach((c) => {
          rows.push({ key: `child-${c.id}`, kind: 'item', item: c, depth: 1 });
        });
    });

    if (orphans.length > 0 && showFullHierarchy) {
      rows.push({
        key: 'orphan-group',
        kind: 'group',
        label: `${orphans.length} issues without parent`,
        depth: 0,
      });
      orphans.forEach((o) => {
        rows.push({ key: `orphan-${o.id}`, kind: 'item', item: o, depth: 1 });
      });
    } else if (orphans.length && !showFullHierarchy) {
      orphans.forEach((o) => {
        rows.push({ key: `orphan-${o.id}`, kind: 'item', item: o, depth: 0 });
      });
    }

    return rows;
  }, [backlog, sortBy, showFullHierarchy, textFilter, statusFilter, hierarchyFrom, hierarchyTo]);

  const timelineRows = scopeRows.filter((r) => r.item);

  const handleDateChange = (item: PlanItemResponse, field: 'target_start' | 'target_end', value: string) => {
    draft.addChange({
      itemId: item.id,
      issueKey: item.issueKey,
      type: field,
      value,
      previousValue: field === 'target_start' ? item.targetDate : item.targetEndDate,
    });
  };

  const handleBarDatesChange = useCallback(
    (itemId: string, start: string, end: string) => {
      const item = itemMap.get(itemId);
      if (!item) return;
      handleDateChange(item, 'target_start', start);
      handleDateChange(item, 'target_end', end);
    },
    [itemMap, draft],
  );

  const handleReview = async () => {
    if (draft.pendingCount === 0) return;
    await draft.commit((id) => itemMap.get(id));
  };

  const handleAutoPreview = async () => {
    try {
      const res = await planApi.runAutoSchedule(planId);
      const dates = res.data?.scheduleDates ?? {};
      for (const [itemId, range] of Object.entries(dates)) {
        const start = (range as { startDate?: string }).startDate;
        const end = (range as { endDate?: string }).endDate;
        const item = itemMap.get(itemId);
        if (!item || !start) continue;
        draft.addChange({ itemId, issueKey: item.issueKey, type: 'target_start', value: start });
        if (end) draft.addChange({ itemId, issueKey: item.issueKey, type: 'target_end', value: end });
      }
      setAutoScheduleOpen(false);
      setScheduleBanner(null);
      appNotify.success('Auto-schedule preview applied. Review changes before committing.');
    } catch {
      const msg =
        'Auto-schedule could not complete. Add issues to the plan and configure dependencies, then try again.';
      setScheduleBanner(msg);
      appNotify.error(msg);
    }
  };

  const handleInlineCreate = async () => {
    if (!inlineName.trim() || !defaultProjectId) {
      if (!defaultProjectId) {
        appNotify.warning('Add a project issue source in plan settings first.');
        return;
      }
      return;
    }
    setInlineSaving(true);
    try {
      const created = await issueApi.create({
        projectId: defaultProjectId,
        title: inlineName.trim(),
        issueTypeId: '',
      });
      const issue = created.data;
      await planApi.addItemToBacklog(planId, {
        issueId: issue.id,
        issueType: 'STORY',
      });
      setInlineName('');
      setInlineCreate(false);
      await queryClient.invalidateQueries({ queryKey: ['backlog', planId] });
    } catch {
      appNotify.error('Failed to create issue. Check project permissions and try again.');
    } finally {
      setInlineSaving(false);
    }
  };

  const onResizeStart = (e: React.MouseEvent) => {
    e.preventDefault();
    const split = splitRef.current;
    if (!split) return;
    const startX = e.clientX;
    const startPct = scopeWidthPct;
    const onMove = (ev: MouseEvent) => {
      const rect = split.getBoundingClientRect();
      const pct = ((ev.clientX - rect.left) / rect.width) * 100;
      setScopeWidthPct(Math.min(70, Math.max(25, pct)));
    };
    const onUp = () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
  };

  if (isLoading) {
    return <div className="ab-loading"><div className="ab-spinner" /></div>;
  }

  return (
    <div className="sa-roadmap-view">
      <PlanActionBar
        plan={plan}
        pendingCount={draft.pendingCount}
        viewLabel={draft.viewLabel}
        onReviewChanges={handleReview}
        onDiscardChanges={draft.discard}
        onAutoSchedule={() => {
          setScheduleBanner(null);
          setAutoScheduleOpen(true);
        }}
        onShare={() => {
          void navigator.clipboard?.writeText(window.location.href);
          appNotify.success('Plan link copied to clipboard.');
        }}
        warningCount={0}
      />

      {scheduleBanner && (
        <div className="sa-rm-inline-banner sa-rm-inline-banner--error" role="alert">
          <span>{scheduleBanner}</span>
          <button
            type="button"
            className="sa-rm-inline-banner__dismiss"
            aria-label="Dismiss"
            onClick={() => setScheduleBanner(null)}
          >
            ×
          </button>
        </div>
      )}

      <div className="sa-roadmap-toolbar-zone">
        <RoadmapToolbar
          hierarchyFrom={hierarchyFrom}
          hierarchyTo={hierarchyTo}
          onHierarchyFromChange={setHierarchyFrom}
          onHierarchyToChange={setHierarchyTo}
          zoom={zoom}
          onZoomChange={setZoom}
          onOpenFilters={() => setFiltersOpen((o) => !o)}
          onOpenViewSettings={() => setViewSettingsOpen((o) => !o)}
          filtersOpen={filtersOpen}
          viewSettingsOpen={viewSettingsOpen}
        />

        {filtersOpen && (
          <RoadmapFiltersPanel
            showFullHierarchy={showFullHierarchy}
            onShowFullHierarchyChange={setShowFullHierarchy}
            textFilter={textFilter}
            onTextFilterChange={setTextFilter}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
            onClose={() => setFiltersOpen(false)}
          />
        )}
        {viewSettingsOpen && (
          <RoadmapViewSettings
            sortBy={sortBy}
            onSortByChange={setSortBy}
            onClose={() => setViewSettingsOpen(false)}
          />
        )}
      </div>
      {autoScheduleOpen && (
        <AutoSchedulePanel
          onClose={() => setAutoScheduleOpen(false)}
          onPreview={() => void handleAutoPreview()}
        />
      )}

      <div className="jdc-roadmap-split" ref={splitRef}>
        <div className="jdc-roadmap-scope" style={{ flex: `0 0 ${scopeWidthPct}%` }}>
          <div className="sa-roadmap-scope-table-wrap">
            <table className="jdc-scope-table">
              <thead>
                <tr>
                  <th className="col-check">#</th>
                  <th className="col-issue">Issue</th>
                  <th className="col-date">Target start</th>
                  <th className="col-date">Target end</th>
                  <th className="col-status">Status</th>
                </tr>
              </thead>
              <tbody>
                {scopeRows.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="sa-roadmap-scope-empty">
                      No issues in plan. Use <strong>+ Create issue</strong> below or add issue sources in{' '}
                      <Link to={`/plans/${planId}/settings`}>settings</Link>.
                    </td>
                  </tr>
                ) : (
                  scopeRows.map((row, index) => {
                    if (row.kind === 'group') {
                      return (
                        <tr key={row.key} className="jdc-scope-row-group">
                          <td colSpan={5} style={{ paddingLeft: 12 + row.depth * 16 }}>
                            ▾ {row.label}
                          </td>
                        </tr>
                      );
                    }
                    const item = row.item!;
                    const selected = selectedId === item.id;
                    return (
                      <tr
                        key={row.key}
                        className={`${row.depth > 0 ? 'jdc-scope-row-child' : ''} ${selected ? 'selected' : ''}`}
                        onClick={() => setSelectedId(item.id)}
                      >
                        <td className="col-check">
                          <input type="checkbox" defaultChecked onClick={(e) => e.stopPropagation()} />
                          {index + 1}
                        </td>
                        <td className="col-issue">
                          <div
                            className="sa-roadmap-scope-issue-cell"
                            style={{ paddingLeft: row.depth * 16 }}
                          >
                            <span aria-hidden="true">{issueIcon(item.issueType)}</span>
                            {item.issueKey && (
                              <Link
                                to={`/issues/${item.issueId}`}
                                className="issue-key"
                                onClick={(e) => e.stopPropagation()}
                              >
                                {item.issueKey}
                              </Link>
                            )}
                            <span className="issue-summary" title={item.summary ?? undefined}>
                              {item.summary ?? '—'}
                            </span>
                          </div>
                        </td>
                        <td className="col-date" onClick={(e) => e.stopPropagation()}>
                          <input
                            type="date"
                            value={
                              draft.getDraftValue(item.id, 'target_start') ??
                              item.targetDate?.slice(0, 10) ??
                              ''
                            }
                            onChange={(e) => handleDateChange(item, 'target_start', e.target.value)}
                          />
                        </td>
                        <td className="col-date" onClick={(e) => e.stopPropagation()}>
                          <input
                            type="date"
                            value={
                              draft.getDraftValue(item.id, 'target_end') ??
                              item.targetEndDate?.slice(0, 10) ??
                              ''
                            }
                            onChange={(e) => handleDateChange(item, 'target_end', e.target.value)}
                          />
                        </td>
                        <td className="col-status">
                          <span className="jdc-lozenge">{item.status ?? 'TO DO'}</span>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
          <div className="sa-roadmap-scope-footer">
            {inlineCreate ? (
              <div className="jdc-inline-create" style={{ flex: 1 }}>
                <input
                  type="text"
                  placeholder="Issue name..."
                  value={inlineName}
                  onChange={(e) => setInlineName(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') void handleInlineCreate();
                    if (e.key === 'Escape') {
                      setInlineCreate(false);
                      setInlineName('');
                    }
                  }}
                  autoFocus
                />
                <button
                  type="button"
                  className="sa-rm-btn sa-rm-btn--primary"
                  disabled={inlineSaving}
                  onClick={() => void handleInlineCreate()}
                  title="Create"
                >
                  Create
                </button>
                <button
                  type="button"
                  className="sa-rm-btn"
                  onClick={() => {
                    setInlineCreate(false);
                    setInlineName('');
                  }}
                >
                  Cancel
                </button>
              </div>
            ) : (
              <>
                <button
                  type="button"
                  className="sa-rm-btn sa-rm-btn--link"
                  onClick={() => setInlineCreate(true)}
                >
                  + Create issue
                </button>
                <button
                  type="button"
                  className="sa-rm-btn sa-rm-btn--link"
                  onClick={() => setShowCreateIssue(true)}
                >
                  Open create dialog
                </button>
              </>
            )}
          </div>
        </div>
        <div
          className="jdc-roadmap-splitter"
          role="separator"
          aria-orientation="vertical"
          onMouseDown={onResizeStart}
        />
        <RoadmapTimeline
          rows={timelineRows}
          zoom={zoom}
          getStartDate={(id) => draft.getDraftValue(id, 'target_start')}
          getEndDate={(id) => draft.getDraftValue(id, 'target_end')}
          onBarDatesChange={handleBarDatesChange}
        />
      </div>

      {showCreateIssue && (
        <PlanCreateIssueModal
          planId={planId}
          planName={plan.name}
          defaultProjectId={defaultProjectId}
          onClose={() => setShowCreateIssue(false)}
          onSuccess={() => {
            void queryClient.invalidateQueries({ queryKey: ['backlog', planId] });
          }}
        />
      )}
    </div>
  );
}
