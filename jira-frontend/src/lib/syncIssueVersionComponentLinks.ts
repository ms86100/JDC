import { versionApi } from '../api/versionApi';
import { componentApi } from '../api/componentApi';

/** Mirror fix-version links into version-service junction tables (best-effort). */
export async function syncIssueFixVersions(issueId: string, versionIds: string[] | undefined) {
  if (!issueId || !versionIds?.length) return;
  await Promise.allSettled(
    versionIds.map((versionId) => versionApi.assignFixVersion(issueId, versionId)),
  );
}

export async function syncIssueAffectsVersions(
  issueId: string,
  versionIds: string[] | undefined,
) {
  if (!issueId || !versionIds?.length) return;
  await Promise.allSettled(
    versionIds.map((versionId) => versionApi.assignAffectsVersion(issueId, versionId)),
  );
}

/** Mirror component links into component-service junction tables (best-effort). */
export async function syncIssueComponents(issueId: string, componentIds: string[] | undefined) {
  if (!issueId || !componentIds?.length) return;
  await Promise.allSettled(
    componentIds.map((componentId) => componentApi.assignToIssue(issueId, componentId)),
  );
}

export async function syncIssueVersionComponentLinks(
  issueId: string,
  opts: {
    fixVersionIds?: string[];
    affectsVersionIds?: string[];
    componentIds?: string[];
  },
) {
  await Promise.all([
    syncIssueFixVersions(issueId, opts.fixVersionIds),
    syncIssueAffectsVersions(issueId, opts.affectsVersionIds),
    syncIssueComponents(issueId, opts.componentIds),
  ]);
}
