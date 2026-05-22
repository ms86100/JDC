import { issueApi } from '../../../api/issueApi';

function normalizeStatus(s: string): string {
  return s.toLowerCase().replace(/[\s_-]+/g, '');
}

/**
 * Move an issue to a target status via the workflow engine when possible;
 * falls back to PATCH status for boards without scheme mappings.
 */
export async function transitionIssueToTargetStatus(
  issueId: string,
  projectId: string,
  targetStatusLabel: string,
): Promise<void> {
  const target = normalizeStatus(targetStatusLabel);

  try {
    const { data } = await issueApi.getAvailableTransitions(issueId, projectId);
    const transitions = data.transitions ?? [];

    const match = transitions.find((t) => {
      const name = normalizeStatus(t.name ?? '');
      return (
        name.includes(target) ||
        target.includes(name) ||
        name === `to${target}` ||
        name === `move${target}`
      );
    });

    if (match?.id) {
      await issueApi.executeTransition({
        issueId,
        projectId,
        transitionId: match.id,
      });
      return;
    }
  } catch {
    /* use fallback */
  }

  await issueApi.transitionStatus(issueId, projectId, { statusId: targetStatusLabel });
}
