import { Link } from 'react-router-dom';
import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { epicApi, CreateEpicRequest } from '../../../../api/epicApi';
import { IssueResponse } from '../../../../api/issueApi';

interface Props {
  projectId: string;
  projectIssues: IssueResponse[];
}

export default function BacklogEpicsPanel({ projectId, projectIssues }: Props) {
  const queryClient = useQueryClient();
  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState<CreateEpicRequest>({ name: '', summary: '', color: '#0052CC' });

  const { data: allEpics = [], isLoading } = useQuery({
    queryKey: ['epics-backlog'],
    queryFn: () => epicApi.getAll().then((r) => r.data),
  });

  const projectEpicIds = useMemo(() => {
    const ids = new Set<string>();
    projectIssues.forEach((i) => {
      if (i.epicId) ids.add(i.epicId);
    });
    return ids;
  }, [projectIssues]);

  const epics = useMemo(
    () => allEpics.filter((e) => projectEpicIds.has(e.id)),
    [allEpics, projectEpicIds],
  );

  const createMutation = useMutation({
    mutationFn: (data: CreateEpicRequest) => epicApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['epics-backlog'] });
      setShowCreate(false);
      setForm({ name: '', summary: '', color: '#0052CC' });
    },
  });

  return (
    <div className="jdc-backlog-side-panel">
      <div className="jdc-backlog-side-panel-head">
        <h2 className="jdc-page-title" style={{ fontSize: 16, margin: 0 }}>Epics</h2>
        <button type="button" className="jdc-btn jdc-btn-secondary jdc-btn-sm" onClick={() => setShowCreate(!showCreate)}>
          {showCreate ? 'Cancel' : 'Create epic'}
        </button>
      </div>
      {showCreate && (
        <div className="jdc-card" style={{ padding: 12, marginBottom: 12 }}>
          <input
            className="jdc-input"
            placeholder="Epic name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            style={{ marginBottom: 8 }}
          />
          <input
            className="jdc-input"
            placeholder="Summary (optional)"
            value={form.summary ?? ''}
            onChange={(e) => setForm({ ...form, summary: e.target.value })}
          />
          <button
            type="button"
            className="jdc-btn jdc-btn-primary jdc-btn-sm"
            style={{ marginTop: 8 }}
            disabled={!form.name.trim() || createMutation.isPending}
            onClick={() => createMutation.mutate(form)}
          >
            Create
          </button>
        </div>
      )}
      {isLoading ? (
        <div className="ab-loading"><div className="ab-spinner" /></div>
      ) : epics.length === 0 ? (
        <p className="jdc-muted">
          No epics linked to issues in this project yet. Create an epic or assign issues to epics from the issue view.
        </p>
      ) : (
        <ul className="jdc-backlog-side-list">
          {epics.map((epic) => {
            const count = projectIssues.filter((i) => i.epicId === epic.id).length;
            return (
              <li key={epic.id}>
                <span
                  className="jdc-epic-dot"
                  style={{ background: epic.color ?? '#0052CC' }}
                  aria-hidden
                />
                <Link to={`/epics/${epic.id}`} className="jdc-link">
                  {epic.name}
                </Link>
                <span className="jdc-muted"> · {count} issues</span>
                {epic.progressPercentage != null && (
                  <span className="jdc-muted"> · {Math.round(epic.progressPercentage)}%</span>
                )}
              </li>
            );
          })}
        </ul>
      )}
      <p style={{ marginTop: 12 }}>
        <Link to={`/epics`} className="jdc-link">Open epics navigator →</Link>
      </p>
    </div>
  );
}
