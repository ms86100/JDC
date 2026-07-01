import { useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { JIRA_DC_MORE_TOP_NAV } from './jiraDcNavRegistry';

interface MoreTopNavDropdownProps {
  active: boolean;
}

const MORE_PATH_PREFIXES = [
  '/tests',
  '/migration',
  '/epics',
  '/workflows',
  '/search',
  '/audit',
  '/admin',
  '/reports/time-tracking',
  '/developer',
  '/issues/batch',
  '/sprints',
  '/kanban',
  '/board/classic',
];

export default function MoreTopNavDropdown({ active }: MoreTopNavDropdownProps) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const location = useLocation();

  useEffect(() => {
    const onDoc = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onDoc);
    return () => document.removeEventListener('mousedown', onDoc);
  }, []);

  const isMoreActive =
    active ||
    MORE_PATH_PREFIXES.some(
      (p) => location.pathname === p || location.pathname.startsWith(`${p}/`),
    );

  return (
    <div ref={ref} className="sa-dc-more-nav" style={{ position: 'relative' }}>
      <button
        type="button"
        className={`sa-dc-nav-link ${isMoreActive ? 'active' : ''}`}
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
        aria-haspopup="true"
        style={{
          padding: '6px 12px',
          borderRadius: 'var(--jdc-radius, 3px)',
          border: 'none',
          cursor: 'pointer',
          fontSize: 'var(--sa-fs-sm, 14px)',
          fontWeight: 500,
          display: 'inline-flex',
          alignItems: 'center',
          gap: 4,
          background: 'transparent',
          color: 'inherit',
        }}
      >
        More <span aria-hidden="true">▾</span>
      </button>
      {open && (
        <div
          className="sa-dc-more-menu"
          role="menu"
          style={{
            position: 'absolute',
            top: '100%',
            left: 0,
            marginTop: 4,
            minWidth: 240,
            background: 'var(--sa-n0, #fff)',
            border: '1px solid var(--sa-n200, #dfe1e6)',
            borderRadius: 'var(--sa-radius-sm, 3px)',
            boxShadow: 'var(--sa-elev-2, 0 4px 12px rgba(0,0,0,0.15))',
            zIndex: 1000,
            padding: '8px 0',
          }}
        >
          {JIRA_DC_MORE_TOP_NAV.map((item) => (
            <Link
              key={item.path}
              to={item.path}
              role="menuitem"
              className="sa-dc-more-menu-item"
              onClick={() => setOpen(false)}
              style={{
                display: 'block',
                padding: '8px 16px',
                fontSize: 13,
                color: 'var(--sa-n900, #172b4d)',
                textDecoration: 'none',
              }}
            >
              <div style={{ fontWeight: 600 }}>{item.label}</div>
              <div style={{ fontSize: 11, color: 'var(--sa-n600, #5e6c84)', marginTop: 2 }}>
                {item.description}
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
