import { useMemo, useState, ReactNode } from 'react';
import { Outlet, useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { issueApi, IssueResponse } from '../../../api/issueApi';
import { projectApi } from '../../../api/projectApi';
import IssueListPane from '../components/IssueListPane';
import CreateIssueModal from '../components/CreateIssueModal';

const PAGE_SIZE = 25;

export interface IssuesLayoutProps {
  projectId?: string;
  issuesBasePath?: string;
  detailOutlet?: ReactNode;
}

export default function IssuesLayout({
  projectId: projectIdProp,
  issuesBasePath,
  detailOutlet,
}: IssuesLayoutProps = {}) {
  const { issueId: routeIssueId, projectId: routeProjectId } = useParams<{
    issueId?: string;
    projectId?: string;
  }>();
  const projectId = projectIdProp ?? routeProjectId;
  const issueId = routeIssueId;
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const base = issuesBasePath ?? (projectId ? `/projects/${projectId}/issues` : '/issues');
  const issuePath = (id: string) => `${base}/${id}`;

  const [showCreate, setShowCreate] = useState(false);
  const [orderBy, setOrderBy] = useState('priority');
  const [textFilter, setTextFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [pageIndex, setPageIndex] = useState(0);

  const { data: project } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.getById(projectId!).then((r) => r.data),
    enabled: !!projectId,
  });

  const { data: issuesResponse, isPending } = useQuery<{ content: IssueResponse[]; totalElements: number }>({
    queryKey: ['issues-navigator', orderBy, projectId],
    queryFn: async () => {
      const response = await issueApi.getAll(projectId ? { projectId } : undefined);
      return response.data;
    },
    retry: 1,
  });

  const filtered = useMemo(() => {
    let issues = issuesResponse?.content ?? [];
    if (textFilter.trim()) {
      const q = textFilter.toLowerCase();
      issues = issues.filter(
        (i) =>
          (i.issueKey ?? '').toLowerCase().includes(q) ||
          (i.title ?? '').toLowerCase().includes(q),
      );
    }
    if (statusFilter !== 'all') {
      issues = issues.filter((i) => (i.status ?? '').toLowerCase() === statusFilter);
    }
    if (orderBy === 'priority') {
      issues = [...issues].sort((a, b) => (a.priority ?? '').localeCompare(b.priority ?? ''));
    } else if (orderBy === 'key') {
      issues = [...issues].sort((a, b) => (a.issueKey ?? '').localeCompare(b.issueKey ?? ''));
    } else if (orderBy === 'updated') {
      issues = [...issues].sort(
        (a, b) => new Date(b.updatedAt ?? 0).getTime() - new Date(a.updatedAt ?? 0).getTime(),
      );
    }
    return issues;
  }, [issuesResponse, orderBy, textFilter, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const safePage = Math.min(pageIndex, totalPages - 1);
  const pageIssues = filtered.slice(safePage * PAGE_SIZE, safePage * PAGE_SIZE + PAGE_SIZE);

  const page = issueId
    ? Math.max(1, filtered.findIndex((i) => i.id === issueId) + 1)
    : safePage * PAGE_SIZE + 1;
  const total = filtered.length || 1;

  const goPrev = () => {
    if (issueId) {
      const idx = filtered.findIndex((i) => i.id === issueId);
      if (idx > 0) navigate(issuePath(filtered[idx - 1].id));
    } else {
      setPageIndex((p) => Math.max(0, p - 1));
    }
  };

  const goNext = () => {
    if (issueId) {
      const idx = filtered.findIndex((i) => i.id === issueId);
      if (idx >= 0 && idx < filtered.length - 1) navigate(issuePath(filtered[idx + 1].id));
    } else {
      setPageIndex((p) => Math.min(totalPages - 1, p + 1));
    }
  };

  return (
    <div className={`sa-issues-page${projectId ? ' sa-issues-page--project' : ''}`}>
      {projectId && (
        <div className="sa-issues-page-top jdc-project-issues-top">
          <span className="jdc-muted">
            {project?.name ?? 'Project'} · <strong>{project?.projectKey}</strong>
          </span>
          <Link to={`/projects/${projectId}/backlog`} className="jdc-link">
            Back to backlog
          </Link>
        </div>
      )}
      <div className="jdc-issue-navigator">
        <IssueListPane
          issues={pageIssues}
          selectedId={issueId}
          orderBy={orderBy}
          page={page}
          total={total}
          onOrderChange={(o) => {
            setOrderBy(o);
            setPageIndex(0);
          }}
          onCreateIssue={() => setShowCreate(true)}
          textFilter={textFilter}
          onTextFilterChange={(v) => {
            setTextFilter(v);
            setPageIndex(0);
          }}
          statusFilter={statusFilter}
          onStatusFilterChange={(v) => {
            setStatusFilter(v);
            setPageIndex(0);
          }}
          onPrevPage={goPrev}
          onNextPage={goNext}
          pageLabel={`${safePage + 1} of ${totalPages}`}
          issueLink={issuePath}
          title={projectId ? `${project?.projectKey ?? 'Project'} issues` : 'Open issues'}
        />
        <div className="jdc-issue-detail-pane">
          {isPending && !issueId ? (
            <div className="jdc-issue-empty-detail">Loading…</div>
          ) : detailOutlet ?? (
            <Outlet context={{ embedded: true }} />
          )}
        </div>
      </div>
      {showCreate && (
        <CreateIssueModal
          projectId={projectId}
          projectKey={project?.projectKey}
          onClose={() => setShowCreate(false)}
          onSuccess={() => {
            queryClient.invalidateQueries({ queryKey: ['issues-navigator'] });
            setShowCreate(false);
          }}
        />
      )}
    </div>
  );
}
