import React from 'react';
import { Link } from 'react-router-dom';
import './AdminIssueConfig.css';

export default function FieldConfigurationPage() {
  return (
    <div className="dc-page ab-issue-config-page">
      <header className="dc-page-header">
        <h1 className="dc-page-title">Field configuration</h1>
        <p className="dc-page-subtitle">
          Field configurations control which fields appear for each issue type (required, hidden,
          renderer). In Avionics Systems this is separate from{' '}
          <Link to="/admin/custom-fields">Custom fields</Link> and screen schemes.
        </p>
      </header>

      <div className="ab-issue-config-info-panel">
        <h2>Planned (Phase 2)</h2>
        <ul>
          <li>Field configuration schemes list</li>
          <li>Per issue type: field visibility and required flags</li>
          <li>Association to projects via field configuration scheme</li>
        </ul>
        <p className="ab-issue-config-muted">
          Related today: <Link to="/admin/screens">Screens</Link>,{' '}
          <Link to="/admin/screen-schemes">Screen schemes</Link>,{' '}
          <Link to="/admin/custom-fields">Custom fields</Link>.
        </p>
      </div>
    </div>
  );
}
