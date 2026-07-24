import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { workflowApi } from '../../../api/workflowApi';

const SECTIONS = [
  {
    title: 'Tools',
    path: '/workflows/admin/tools',
    description: 'Export, import, validate, migration preview, usage, transition stats, revert.',
  },
  {
    title: 'Schemes',
    path: '/workflows/admin/schemes',
    description: 'List and manage workflow schemes and issue-type mappings via admin API.',
  },
  {
    title: 'Screens',
    path: '/workflows/admin/screens',
    description: 'Create and delete transition screens; assign from designer or transition config.',
  },
  {
    title: 'Definitions',
    path: '/workflows/admin/definitions',
    description: 'Condition, validator, and post-function definition catalogs.',
  },
  {
    title: 'Scripts',
    path: '/workflows/admin/scripts',
    description: 'Create and manage JavaScript scripts for workflow conditions, validators, and post-functions.',
  },
  {
    title: 'Audit log',
    path: '/workflows/admin/audit',
    description: 'Global workflow administration audit trail.',
  },
];

export default function WorkflowAdminHubPage() {
  const { data: workflows = [] } = useQuery({
    queryKey: ['workflows'],
    queryFn: () => workflowApi.getAll().then((r) => Array.isArray(r.data) ? r.data : []),
  });

  return (
    <div className="space-y-6" data-testid="workflow-admin-hub">
      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4">
        {SECTIONS.map((s) => (
          <Link
            key={s.path}
            to={s.path}
            className="block p-4 bg-white border rounded-lg hover:border-jira-blue hover:shadow-sm transition"
          >
            <h2 className="font-semibold text-gray-900">{s.title}</h2>
            <p className="text-sm text-gray-600 mt-1">{s.description}</p>
          </Link>
        ))}
      </div>

      <div className="bg-white border rounded-lg p-4">
        <h2 className="font-semibold text-sm mb-2">Quick open</h2>
        <p className="text-sm text-gray-600 mb-3">
          {workflows.length} workflow(s) available. Use the hub or open picker for detail / designer.
        </p>
        <div className="flex flex-wrap gap-2">
          <Link to="/workflows/open?view=detail" className="text-sm text-jira-blue underline">
            Open configuration…
          </Link>
          <span className="text-gray-300">|</span>
          <Link to="/workflows/open?view=designer" className="text-sm text-jira-blue underline">
            Open designer…
          </Link>
          <span className="text-gray-300">|</span>
          <Link to="/workflows" className="text-sm text-jira-blue underline">
            Workflow hub
          </Link>
        </div>
      </div>
    </div>
  );
}
