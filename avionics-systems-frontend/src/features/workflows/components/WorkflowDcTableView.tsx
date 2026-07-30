import { Link, useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { workflowApi, Workflow, WorkflowScheme } from '../../../api/workflowApi';

interface Props {
  workflows: Workflow[];
  schemes: WorkflowScheme[];
  search: string;
  mode: 'workflows' | 'schemes';
}

export default function WorkflowDcTableView({ workflows, schemes, search, mode }: Props) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const cloneMutation = useMutation({
    mutationFn: ({ id, name }: { id: string; name: string }) => workflowApi.clone(id, name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['workflows'] }),
  });

  const safeWorkflows = Array.isArray(workflows) ? workflows : [];
  const safeSchemes = Array.isArray(schemes) ? schemes : [];

  const filteredWorkflows = safeWorkflows.filter(
    (w) =>
      w.name.toLowerCase().includes(search.toLowerCase()) ||
      (w.description ?? '').toLowerCase().includes(search.toLowerCase()),
  );

  const filteredSchemes = safeSchemes.filter(
    (s) =>
      s.name.toLowerCase().includes(search.toLowerCase()) ||
      (s.description ?? '').toLowerCase().includes(search.toLowerCase()),
  );

  if (mode === 'workflows') {
    return (
      <div className="jdc-wf-dc-table-wrap">
        <table className="jdc-settings-table jdc-wf-dc-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Description</th>
              <th>Statuses</th>
              <th>Transitions</th>
              <th>State</th>
              <th>Operations</th>
            </tr>
          </thead>
          <tbody>
            {filteredWorkflows.length === 0 ? (
              <tr>
                <td colSpan={6} className="jdc-muted" style={{ padding: 16 }}>
                  No workflows match your search.
                </td>
              </tr>
            ) : (
              filteredWorkflows.map((wf) => (
                <tr key={wf.id}>
                  <td>
                    <strong>{wf.name}</strong>
                    {wf.isSystem && <span className="jdc-badge" style={{ marginLeft: 6 }}>System</span>}
                  </td>
                  <td className="jdc-muted">{wf.description || '—'}</td>
                  <td>{wf.statusCount ?? 0}</td>
                  <td>{wf.transitionCount ?? 0}</td>
                  <td>
                    {wf.isDraft && <span className="jdc-badge">Draft</span>}
                    {wf.isActive && !wf.isDraft && <span className="jdc-badge">Active</span>}
                    {!wf.isActive && !wf.isDraft && <span className="jdc-muted">Inactive</span>}
                  </td>
                  <td>
                    <nav className="jdc-wf-ops" aria-label={`Actions for ${wf.name}`}>
                      <Link
                        to={`/workflows/${wf.id}/designer`}
                        className="jdc-wf-op jdc-wf-op--view"
                        title="Open workflow diagram"
                      >
                        View diagram
                      </Link>
                      <span className="jdc-wf-op-sep" aria-hidden="true" />
                      <button
                        type="button"
                        className="jdc-wf-op jdc-wf-op--edit"
                        title="Edit workflow settings and transitions"
                        onClick={() => navigate(`/workflows/${wf.id}`)}
                      >
                        Edit
                      </button>
                      <span className="jdc-wf-op-sep" aria-hidden="true" />
                      <button
                        type="button"
                        className="jdc-wf-op jdc-wf-op--copy"
                        title="Create a copy of this workflow"
                        disabled={cloneMutation.isPending}
                        onClick={() => {
                          const name = window.prompt('Copy workflow as:', `${wf.name} (Copy)`);
                          if (name?.trim()) cloneMutation.mutate({ id: wf.id, name: name.trim() });
                        }}
                      >
                        {cloneMutation.isPending ? 'Copying…' : 'Duplicate'}
                      </button>
                    </nav>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        <p className="jdc-muted" style={{ marginTop: 8, fontSize: 12 }}>
          Full administration: <Link to="/admin/workflows" className="jdc-link">Administration → Workflows</Link>
        </p>
      </div>
    );
  }

  return (
    <div className="jdc-wf-dc-table-wrap">
      <table className="jdc-settings-table jdc-wf-dc-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Description</th>
            <th>Issue types</th>
            <th>Projects</th>
            <th>Operations</th>
          </tr>
        </thead>
        <tbody>
          {filteredSchemes.length === 0 ? (
            <tr>
              <td colSpan={5} className="jdc-muted" style={{ padding: 16 }}>
                No workflow schemes found.
              </td>
            </tr>
          ) : (
            filteredSchemes.map((s) => (
              <tr key={s.id}>
                <td>
                  <strong>{s.name}</strong>
                  {s.isDefault && <span className="jdc-badge" style={{ marginLeft: 6 }}>Default</span>}
                </td>
                <td className="jdc-muted">{s.description || '—'}</td>
                <td>{s.issueTypeCount ?? s.mappings?.length ?? '—'}</td>
                <td>{s.projectCount ?? '—'}</td>
                <td>
                  <nav className="jdc-wf-ops" aria-label={`Actions for ${s.name}`}>
                    <Link
                      to="/admin/workflows"
                      className="jdc-wf-op jdc-wf-op--view"
                      title="View scheme in administration"
                    >
                      View
                    </Link>
                    <span className="jdc-wf-op-sep" aria-hidden="true" />
                    <Link
                      to="/workflows/admin/schemes"
                      className="jdc-wf-op jdc-wf-op--edit"
                      title="Edit workflow scheme mappings"
                    >
                      Edit scheme
                    </Link>
                  </nav>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
