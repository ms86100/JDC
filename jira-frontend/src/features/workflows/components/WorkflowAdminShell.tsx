import { Link, Outlet, useLocation } from 'react-router-dom';

const ADMIN_TABS: { label: string; path: string; description: string }[] = [
  { label: 'Overview', path: '/workflows/admin', description: 'Admin API hub' },
  { label: 'Tools', path: '/workflows/admin/tools', description: 'Export, validate, migrate' },
  { label: 'Schemes', path: '/workflows/admin/schemes', description: 'Workflow scheme CRUD' },
  { label: 'Screens', path: '/workflows/admin/screens', description: 'Transition screens' },
  { label: 'Definitions', path: '/workflows/admin/definitions', description: 'C/V/PF catalogs' },
  { label: 'Audit log', path: '/workflows/admin/audit', description: 'Global workflow audit' },
];

export default function WorkflowAdminShell() {
  const { pathname } = useLocation();

  const isActive = (path: string) =>
    path === '/workflows/admin'
      ? pathname === '/workflows/admin' || pathname === '/workflows/admin/'
      : pathname === path || pathname.startsWith(`${path}/`);

  return (
    <div className="max-w-6xl mx-auto p-6" data-testid="workflow-admin-shell">
      <div className="flex flex-wrap items-start justify-between gap-4 mb-6">
        <div>
          <Link to="/workflows" className="text-sm text-jira-blue hover:underline">
            ← Workflow hub
          </Link>
          <h1 className="text-2xl font-bold mt-1">Workflow administration</h1>
          <p className="text-sm text-gray-600 mt-1">
            Structured UI for <code className="text-xs bg-gray-100 px-1 rounded">/api/admin/workflows/*</code>{' '}
            endpoints (schemes, screens, definitions, audit).
          </p>
        </div>
        <Link
          to="/workflows/screens"
          className="text-sm text-gray-600 hover:text-jira-blue underline"
        >
          Legacy screens page →
        </Link>
      </div>

      <nav
        className="flex flex-wrap gap-2 border-b border-gray-200 pb-3 mb-6"
        aria-label="Workflow admin sections"
      >
        {ADMIN_TABS.map((tab) => (
          <Link
            key={tab.path}
            to={tab.path}
            title={tab.description}
            className={`px-3 py-1.5 text-sm rounded-md border transition-colors ${
              isActive(tab.path)
                ? 'bg-jira-blue text-white border-jira-blue'
                : 'bg-white text-gray-700 border-gray-200 hover:border-gray-300'
            }`}
          >
            {tab.label}
          </Link>
        ))}
      </nav>

      <Outlet />
    </div>
  );
}
