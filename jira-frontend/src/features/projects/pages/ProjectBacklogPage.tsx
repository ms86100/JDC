import { useMemo, useState } from 'react';
import { useOutletContext, useParams, Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { sprintApi, SprintResponse } from '../../../api/sprintApi';
import { issueApi, IssueResponse } from '../../../api/issueApi';
import { ProjectResponse } from '../../../api/projectApi';
import CreateIssueModal from '../../issues/components/CreateIssueModal';
import { sortByRank } from '../../issues/utils/issueRank';
import BacklogIssueRow from '../components/backlog/BacklogIssueRow';
import BacklogVersionsPanel from '../components/backlog/BacklogVersionsPanel';
import BacklogEpicsPanel from '../components/backlog/BacklogEpicsPanel';

type SideTab = 'backlog' | 'versions' | 'epics';

interface LayoutContext {
  project?: ProjectResponse;
  projectId?: string;
}

export default function ProjectBacklogPage() {
  const { projectId: paramId } = useParams<{ projectId: string }>();
  const ctx = useOutletContext<LayoutContext>();
  const projectId = paramId ?? ctx.projectId ?? ctx.project?.id ?? '';
  const project = ctx.project;
  const queryClient = useQueryClient();

  const [sideTab, setSideTab] = useState<SideTab>('backlog');
  const [showCreateSprint, setShowCreateSprint] = useState(false);
  const [sprintForm, setSprintForm] = useState({ name: '', goal: '' });
  const [inlineTitle, setInlineTitle] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [targetSprintId, setTargetSprintId] = useState<string | undefined>();

  const { data: sprints = [], isPending: sprintsLoading } = useQuery({
    queryKey: ['sprints', projectId],
    queryFn: () => sprintApi.getAll(projectId).catch(() => [] as SprintResponse[]),
    enabled: !!projectId,
    retry: 1,
  });

  const { data: issues = [], isPending: issuesLoading } = useQuery({
    queryKey: ['backlog-issues', projectId],
    queryFn: async () => {
      try {
        const res = await issueApi.getAll({ projectId });
        const data = res.data;
        if (Array.isArray(data)) return data as IssueResponse[];
        return (data?.content ?? []) as IssueResponse[];
      } catch {
        return [] as IssueResponse[];
      }
    },
    enabled: !!projectId,
    retry: 1,
  });

  const planningSprints = useMemo(
    () => sprints.filter((s) => s.status === 'PLANNING'),
    [sprints],
  );

  const backlogIssues = useMemo(
    () => sortByRank(issues.filter((i) => !i.sprintId)),
    [issues],
  );

  const issuesBySprint = useMemo(() => {
    const map = new Map<string, IssueResponse[]>();
    planningSprints.forEach((s) => map.set(s.id, []));
    issues.forEach((issue) => {
      if (issue.sprintId && map.has(issue.sprintId)) {
        map.get(issue.sprintId)!.push(issue);
      }
    });
    return map;
  }, [issues, planningSprints]);

  const createSprintMutation = useMutation({
    mutationFn: () =>
      sprintApi.create({
        name: sprintForm.name.trim(),
        goal: sprintForm.goal.trim() || undefined,
        projectId,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints', projectId] });
      setShowCreateSprint(false);
      setSprintForm({ name: '', goal: '' });
    },
  });

  const startSprintMutation = useMutation({
    mutationFn: (sprintId: string) => sprintApi.start(sprintId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sprints', projectId] });
      queryClient.invalidateQueries({ queryKey: ['board-data'] });
    },
  });

  const inlineCreateMutation = useMutation({
    mutationFn: async (title: string) => {
      const typesRes = await issueApi.getTypes();
      const prioritiesRes = await issueApi.getPriorities();
      const defaultType = typesRes.data?.[0];
      const defaultPriority = prioritiesRes.data?.[0];
      if (!defaultType?.id) throw new Error('No issue types configured');
      return issueApi.create({
        projectId,
        title: title.trim(),
        issueTypeId: defaultType.id,
        priorityId: defaultPriority?.id,
        sprintId: targetSprintId,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['backlog-issues', projectId] });
      setInlineTitle('');
    },
  });

  const handleInlineCreate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inlineTitle.trim()) return;
    inlineCreateMutation.mutate(inlineTitle);
  };

  const openCreateIssue = (sprintId?: string) => {
    setTargetSprintId(sprintId);
    setShowCreateModal(true);
  };

  if (!projectId) {
    return <div className="ab-empty-state"><p>Project not found.</p></div>;
  }

  return (
    <div className="jdc-backlog-page sa-project-backlog">
      <div className="jdc-backlog-side-tabs" role="tablist" aria-label="Backlog panels">
        {(['backlog', 'versions', 'epics'] as SideTab[]).map((tab) => (
          <button
            key={tab}
            type="button"
            role="tab"
            aria-selected={sideTab === tab}
            className={`jdc-backlog-side-tab${sideTab === tab ? ' active' : ''}`}
            onClick={() => setSideTab(tab)}
          >
            {tab === 'backlog' ? 'Backlog' : tab === 'versions' ? 'Versions' : 'Epics'}
          </button>
        ))}
      </div>

      <div className="jdc-backlog-main">
        {sideTab === 'backlog' && (
          <>
            <div className="jdc-backlog-header">
              <div>
                <h1 className="jdc-page-title" style={{ margin: 0, fontSize: 20 }}>Backlog</h1>
                <p className="jdc-muted" style={{ margin: '4px 0 0' }}>
                  {project?.name ?? 'Project'} — plan sprints and rank issues
                </p>
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <button
                  type="button"
                  className="jdc-btn jdc-btn-secondary"
                  onClick={() => openCreateIssue()}
                >
                  Create issue
                </button>
                <button
                  type="button"
                  className="jdc-btn jdc-btn-primary"
                  onClick={() => setShowCreateSprint(true)}
                >
                  Create sprint
                </button>
              </div>
            </div>

            {showCreateSprint && (
              <div className="jdc-card" style={{ marginBottom: 16, padding: 16 }}>
                <h3 style={{ marginTop: 0 }}>Create sprint</h3>
                <div className="jdc-form-row">
                  <label className="jdc-label">Sprint name</label>
                  <input
                    className="jdc-input"
                    value={sprintForm.name}
                    onChange={(e) => setSprintForm((f) => ({ ...f, name: e.target.value }))}
                    placeholder="Sprint name"
                  />
                </div>
                <div className="jdc-form-row">
                  <label className="jdc-label">Goal (optional)</label>
                  <input
                    className="jdc-input"
                    value={sprintForm.goal}
                    onChange={(e) => setSprintForm((f) => ({ ...f, goal: e.target.value }))}
                  />
                </div>
                <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
                  <button
                    type="button"
                    className="jdc-btn jdc-btn-primary"
                    disabled={!sprintForm.name.trim() || createSprintMutation.isPending}
                    onClick={() => createSprintMutation.mutate()}
                  >
                    {createSprintMutation.isPending ? 'Creating…' : 'Create'}
                  </button>
                  <button
                    type="button"
                    className="jdc-btn jdc-btn-secondary"
                    onClick={() => setShowCreateSprint(false)}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )}

            {(sprintsLoading || issuesLoading) && (
              <div className="ab-loading"><div className="ab-spinner" /></div>
            )}

            {!sprintsLoading && !issuesLoading && (
              <>
                {planningSprints.map((sprint, idx) => (
                  <SprintBacklogBlock
                    key={sprint.id}
                    sprint={sprint}
                    projectId={projectId}
                    issues={issuesBySprint.get(sprint.id) ?? []}
                    onStart={() => startSprintMutation.mutate(sprint.id)}
                    onAddIssue={() => openCreateIssue(sprint.id)}
                    boardLink={`/projects/${projectId}/board/active`}
                    canMoveUp={idx > 0}
                    canMoveDown={idx < planningSprints.length - 1}
                    onMoveUp={() => {
                      if (idx > 0) {
                        sprintApi.update(sprint.id, { name: sprint.name }).then(() =>
                          queryClient.invalidateQueries({ queryKey: ['sprints', projectId] })
                        );
                      }
                    }}
                    onDelete={() => {
                      if (confirm('Delete sprint "' + sprint.name + '"? Issues will move to backlog.')) {
                        sprintApi.delete(sprint.id).then(() =>
                          queryClient.invalidateQueries({ queryKey: ['sprints', projectId] })
                        );
                      }
                    }}
                    sprintCount={planningSprints.length}
                  />
                ))}

                <div className="jdc-backlog-sprint-block">
                  <div className="jdc-backlog-sprint-head">
                    <strong>Backlog</strong>
                    <span className="jdc-muted">{backlogIssues.length} issues</span>
                  </div>
                  {backlogIssues.map((issue) => (
                    <BacklogIssueRow
                      key={issue.id}
                      issue={issue}
                      projectId={projectId}
                      siblingIssues={backlogIssues}
                    />
                  ))}
                  <form className="jdc-backlog-inline-create" onSubmit={handleInlineCreate}>
                    <input
                      type="text"
                      placeholder="+ Create issue (type title and press Enter)"
                      value={inlineTitle}
                      onChange={(e) => setInlineTitle(e.target.value)}
                      disabled={inlineCreateMutation.isPending}
                    />
                  </form>
                </div>
              </>
            )}
          </>
        )}

        {sideTab === 'versions' && <BacklogVersionsPanel projectId={projectId} />}

        {sideTab === 'epics' && (
          <BacklogEpicsPanel projectId={projectId} projectIssues={issues} />
        )}
      </div>

      {showCreateModal && (
        <CreateIssueModal
          projectId={projectId}
          projectKey={project?.projectKey}
          onClose={() => {
            setShowCreateModal(false);
            setTargetSprintId(undefined);
          }}
          onSuccess={() => {
            queryClient.invalidateQueries({ queryKey: ['backlog-issues', projectId] });
            setShowCreateModal(false);
            setTargetSprintId(undefined);
          }}
        />
      )}
    </div>
  );
}

function SprintBacklogBlock({
  sprint,
  projectId,
  issues,
  onStart,
  onAddIssue,
  onDelete,
  boardLink,
  canMoveUp,
  canMoveDown,
  onMoveUp,
  sprintCount,
}: {
  sprint: SprintResponse;
  projectId: string;
  issues: IssueResponse[];
  onStart: () => void;
  onAddIssue: () => void;
  onDelete?: () => void;
  boardLink: string;
  canMoveUp?: boolean;
  canMoveDown?: boolean;
  onMoveUp?: () => void;
  sprintCount?: number;
}) {
  const [collapsed, setCollapsed] = useState(false);

  const totalPoints = issues.reduce((sum, i) => sum + ((i as any).storyPoints || 0), 0);

  return (
    <div className="jdc-backlog-sprint-block">
      <div className="jdc-backlog-sprint-head">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <button type="button" onClick={() => setCollapsed(!collapsed)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, fontSize: 14, color: '#6b7280' }}>
            {collapsed ? '▸' : '▾'}
          </button>
          <strong>{sprint.name}</strong>
          <span className="jdc-muted">
            {issues.length} issues {totalPoints > 0 && `· ${totalPoints} pts`} · {sprint.status}
          </span>
          {sprint.startDate && (
            <span className="jdc-muted" style={{ fontSize: '0.75rem' }}>
              {new Date(sprint.startDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
              {sprint.endDate && ` — ${new Date(sprint.endDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}`}
            </span>
          )}
        </div>
        <div style={{ display: 'flex', gap: 6 }}>
          <button type="button" className="jdc-btn jdc-btn-secondary jdc-btn-sm" onClick={onAddIssue}>
            + Create issue
          </button>
          <button type="button" className="jdc-btn jdc-btn-primary jdc-btn-sm" onClick={onStart}>
            Start sprint
          </button>
          <Link to={boardLink} className="jdc-btn jdc-btn-secondary jdc-btn-sm">
            Board
          </Link>
          {onDelete && (
            <button type="button" className="jdc-btn jdc-btn-secondary jdc-btn-sm" onClick={onDelete}
              style={{ color: '#ef4444' }} title="Delete sprint">
              ✕
            </button>
          )}
        </div>
      </div>
      {sprint.goal && (
        <div style={{ padding: '4px 16px 8px', fontSize: '0.813rem', color: '#6b7280', fontStyle: 'italic' }}>
          Goal: {sprint.goal}
        </div>
      )}
      {!collapsed && (
        <>
          {issues.length === 0 ? (
            <p className="jdc-muted" style={{ padding: 12 }}>No issues in this sprint yet. Drag issues here or create new ones.</p>
          ) : (
            issues.map((issue) => (
              <BacklogIssueRow
                key={issue.id}
                issue={issue}
                projectId={projectId}
                siblingIssues={issues}
              />
            ))
          )}
        </>
      )}
    </div>
  );
}
