import { useQuery } from '@tanstack/react-query';
import { workflowAdminApi } from '../../../api/workflowAdminApi';

function DefinitionPanel({
  title,
  data,
  isLoading,
}: {
  title: string;
  data: unknown;
  isLoading: boolean;
}) {
  return (
    <div className="bg-white border rounded-lg p-4">
      <h2 className="font-semibold text-sm mb-2">{title}</h2>
      {isLoading ? (
        <p className="text-sm text-gray-500">Loading…</p>
      ) : (
        <pre className="text-xs overflow-auto max-h-56 bg-gray-50 p-3 rounded">
          {JSON.stringify(data ?? [], null, 2)}
        </pre>
      )}
    </div>
  );
}

export default function WorkflowAdminDefinitionsPage() {
  const conditions = useQuery({
    queryKey: ['workflow-admin', 'conditions-def'],
    queryFn: () => workflowAdminApi.conditionDefinitions().then((r) => r.data),
  });
  const validators = useQuery({
    queryKey: ['workflow-admin', 'validators-def'],
    queryFn: () => workflowAdminApi.validatorDefinitions().then((r) => r.data),
  });
  const postFunctions = useQuery({
    queryKey: ['workflow-admin', 'post-functions-def'],
    queryFn: () => workflowAdminApi.postFunctionDefinitions().then((r) => r.data),
  });

  return (
    <div
      className="grid md:grid-cols-3 gap-4"
      data-testid="workflow-admin-definitions"
    >
      <DefinitionPanel
        title="Conditions"
        data={conditions.data}
        isLoading={conditions.isLoading}
      />
      <DefinitionPanel
        title="Validators"
        data={validators.data}
        isLoading={validators.isLoading}
      />
      <DefinitionPanel
        title="Post functions"
        data={postFunctions.data}
        isLoading={postFunctions.isLoading}
      />
    </div>
  );
}
