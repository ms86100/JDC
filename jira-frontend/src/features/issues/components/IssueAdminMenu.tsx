import { useState } from 'react';
import { Link } from 'react-router-dom';

interface Props {
  projectId?: string;
  issueKey?: string;
}

export default function IssueAdminMenu({ projectId, issueKey }: Props) {
  const [open, setOpen] = useState(false);

  const permUrl = projectId
    ? `/admin/permissions?projectId=${encodeURIComponent(projectId)}`
    : '/admin/permissions';
  const notifUrl = '/admin/notification-schemes';

  return (
    <div className="idc-dropdown-wrapper">
      <button
        type="button"
        className="idc-action-btn"
        onClick={() => setOpen(!open)}
        aria-haspopup="true"
        aria-expanded={open}
      >
        Admin <span className="idc-dropdown-caret">▾</span>
      </button>
      {open && (
        <div className="idc-dropdown-menu">
          <Link
            to={permUrl}
            className="idc-dropdown-item"
            style={{ display: 'block', textDecoration: 'none' }}
            onClick={() => setOpen(false)}
          >
            Permission helper
          </Link>
          <Link
            to={notifUrl}
            className="idc-dropdown-item"
            style={{ display: 'block', textDecoration: 'none' }}
            onClick={() => setOpen(false)}
          >
            Notification helper
          </Link>
          {issueKey && (
            <button
              type="button"
              className="idc-dropdown-item"
              onClick={() => {
                navigator.clipboard?.writeText(issueKey);
                setOpen(false);
              }}
            >
              Copy issue key
            </button>
          )}
        </div>
      )}
    </div>
  );
}
