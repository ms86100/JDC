import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { workflowApi } from '../../../api/workflowApi';

interface Props {
  projectId: string;
}

export default function ProjectWorkflowSchemePanel({ projectId }: Props) {
  const queryClient = useQueryClient();
  const [selectedSchemeId, setSelectedSchemeId] = useState('');

  const { data: projectScheme, isLoading: schemeLoading } = useQuery({
    queryKey: ['project-workflow-scheme', projectId],
    queryFn: () => workflowApi.getProjectScheme(projectId).then((r) => r.data),
    enabled: !!projectId,
  });

  const { data: schemes = [] } = useQuery({
    queryKey: ['workflow-schemes'],
    queryFn: () => workflowApi.getSchemes().then((r) => r.data),
  });

  const assignMutation = useMutation({
    mutationFn: (schemeId: string) => workflowApi.assignSchemeToProject(projectId, schemeId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project-workflow-scheme', projectId] });
    },
  });

  const currentScheme = schemes.find((s) => s.id === projectScheme?.schemeId);

  return (
    <div className="wf-project-scheme">
      <p className="wf-muted">
        Assign a workflow scheme to map issue types to workflows for this project (Systems Data Center model).
      </p>
      {schemeLoading ? (
        <div className="ab-loading"><div className="ab-spinner" /></div>
      ) : (
        <>
          <p>
            <strong>Current scheme:</strong>{' '}
            {currentScheme?.name ?? projectScheme?.schemeId ?? 'None assigned'}
          </p>
          <div className="wf-inline-form">
            <select
              className="ab-select"
              value={selectedSchemeId || projectScheme?.schemeId || ''}
              onChange={(e) => setSelectedSchemeId(e.target.value)}
            >
              <option value="">Select workflow scheme</option>
              {schemes.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                  {s.isDraft ? ' (draft)' : ''}
                </option>
              ))}
            </select>
            <button
              type="button"
              className="ab-btn ab-btn-primary ab-btn-sm"
              disabled={!selectedSchemeId && !projectScheme?.schemeId}
              onClick={() =>
                assignMutation.mutate(selectedSchemeId || projectScheme!.schemeId)
              }
            >
              {assignMutation.isPending ? 'Saving…' : 'Assign scheme'}
            </button>
          </div>
        </>
      )}
    </div>
  );
}
