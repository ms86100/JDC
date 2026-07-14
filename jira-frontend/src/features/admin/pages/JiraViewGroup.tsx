import React from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useJiraGroupByName } from '../hooks/useAdminApi';
import './JiraViewGroup.css';

export default function JiraViewGroup() {
  const [searchParams] = useSearchParams();
  const groupName = searchParams.get('name') || '';

  const { data: group, isLoading, error } = useJiraGroupByName(groupName);

  if (!groupName) {
    return (
      <div className="view-group-error">
        <p>No group specified. Please select a group from the{' '}
          <Link to="/admin/groups">Groups list</Link>.
        </p>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="view-group-loading">Loading...</div>
    );
  }

  if (error || !group) {
    return (
      <div className="view-group-error">
        <p>Group not found: {groupName}</p>
        <Link to="/admin/groups">← Back to Groups</Link>
      </div>
    );
  }

  return (
    <div className="view-group">
        {/* Breadcrumb */}
        <div className="view-group-breadcrumb">
          <Link to="/admin/groups" className="breadcrumb-link">Groups</Link>
          <span className="breadcrumb-sep">/</span>
          <span className="breadcrumb-current">{group.name}</span>
        </div>

        {/* Header */}
        <div className="view-group-header">
          <div className="view-group-title-row">
            <h1 className="view-group-title">{group.name}</h1>
            {group.isSystem && (
              <div className="group-badges">
                <span className="badge badge-admin">ADMIN</span>
                <span className="badge badge-jira">JIRA SOFTWARE</span>
              </div>
            )}
          </div>
          <div className="view-group-actions">
            <Link to={`/admin/groups/members/${group.id}`} className="btn-secondary">
              View Users
            </Link>
            <Link to={`/admin/groups/members/${group.id}`} className="btn-secondary">
              Add/Remove Users
            </Link>
          </div>
        </div>

        {/* Scheme Associations - Definition List Style */}
        <div className="view-group-schemes">
          <h2 className="schemes-heading">Group Details</h2>

          <div className="scheme-list">
            <SchemeItem
              label="Permission Schemes"
              schemes={group.permissionSchemes}
              emptyMessage="There are no Permission Schemes associated with this Group."
            />
            <SchemeItem
              label="Notification Schemes"
              schemes={group.notificationSchemes}
              emptyMessage="There are no Notification Schemes associated with this Group."
            />
            <SchemeItem
              label="Issue Security Schemes"
              schemes={group.securitySchemes}
              emptyMessage="There are no Issue Security Schemes associated with this Group."
            />
            <SchemeItem
              label="Saved Filters"
              schemes={[]}
              emptyMessage="There are no Saved Filters associated with this Group."
            />
          </div>
        </div>
      </div>
  );
}

function SchemeItem({
  label,
  schemes,
  emptyMessage,
}: {
  label: string;
  schemes: { id: string; name: string }[];
  emptyMessage: string;
}) {
  return (
    <div className="scheme-item">
      <div className="scheme-label">{label}</div>
      <div className="scheme-content">
        {!schemes || schemes.length === 0 ? (
          <span className="scheme-empty">{emptyMessage}</span>
        ) : (
          <ul className="scheme-list-items">
            {schemes.map((scheme) => (
              <li key={scheme.id} className="scheme-list-item">
                <span className="scheme-link">{scheme.name}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}