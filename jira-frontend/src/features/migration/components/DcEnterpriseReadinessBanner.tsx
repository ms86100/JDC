/** MG-P0-1: Points users to the live AC checklist after validate. */
export default function DcEnterpriseReadinessBanner() {
  return (
    <div
      className="rounded-lg border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-900"
      data-testid="dc-enterprise-readiness-banner"
      role="status"
    >
      <strong>Systems DC issue import — enterprise AC checklist</strong>
      <p className="mt-1">
        Click <strong>Validate now</strong> below. The <strong>Enterprise AC sign-off</strong> table (AC-1–AC-10) appears
        under the validation results when migration-service is running (port 8094). After import, the same checklist
        updates on the Complete step and in Job history.
      </p>
    </div>
  );
}
