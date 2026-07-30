import IssueCustomFieldsPanel, { IssueCustomFieldsPanelProps } from './IssueCustomFieldsPanel';

/** @deprecated Use IssueCustomFieldsPanel */
export default function IssueMigratedFieldsPanel(
  props: { issueId: string } & Partial<IssueCustomFieldsPanelProps>,
) {
  return (
    <IssueCustomFieldsPanel
      issueId={props.issueId}
      issueKey={props.issueKey}
      projectId={props.projectId}
      issueTypeId={props.issueTypeId}
      variant={props.variant ?? 'inline'}
    />
  );
}
