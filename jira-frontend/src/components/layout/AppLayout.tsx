import { Outlet, NavLink, Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../features/auth/context/AuthContext';
import { useState } from 'react';

const TOP_NAV = [
  { label: 'Dashboards', path: '/dashboard' },
  { label: 'Projects', path: '/projects' },
  { label: 'Issues', path: '/issues' },
  { label: 'Boards', path: '/boards' },
  { label: 'Plans', path: '/programs' },
];

const SIDE_NAV = [
  { name: 'Dashboard', path: '/dashboard' },
  { name: 'Projects', path: '/projects' },
  { name: 'Programs', path: '/programs' },
  { name: 'Issues', path: '/issues' },
  { name: 'Boards', path: '/boards' },
  { name: 'Sprints', path: '/sprints' },
  { name: 'Workflows', path: '/workflows' },
  { name: 'Search', path: '/search' },
  { name: 'Notifications', path: '/notifications' },
];

const SYSTEM_NAV = [
  { name: 'Administration', path: '/admin' },
  { name: 'Audit logs', path: '/audit' },
  { name: 'Migration', path: '/migration' },
];

export default function AppLayout() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  const initials = user?.username
    ? user.username.slice(0, 2).toUpperCase()
    : 'U';

  const isTopNavActive = (path: string) => {
    if (path === '/dashboard') {
      return location.pathname === '/dashboard' || location.pathname === '/';
    }
    if (path === '/admin') {
      return location.pathname.startsWith('/admin');
    }
    return location.pathname === path || location.pathname.startsWith(`${path}/`);
  };

  return (
    <div className="ab-jira-root ab-app-shell">
      <header className="ab-jira-topnav">
        <div className="ab-topnav-left">
          <Link to="/dashboard" className="ab-jira-logo-btn" title="Jira">
            <div className="ab-jira-logo-icon">J</div>
            <span className="ab-jira-logo-text">Jira</span>
          </Link>
        </div>

        <div className="ab-topnav-center">
          <nav className="ab-topnav-items" aria-label="Primary">
            {TOP_NAV.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                className={`ab-topnav-item ${isTopNavActive(item.path) ? 'active' : ''}`}
              >
                {item.label}
              </Link>
            ))}
          </nav>
          <button
            type="button"
            className="ab-create-btn"
            onClick={() => window.dispatchEvent(new CustomEvent('openCreateIssue'))}
          >
            <span>+</span> Create
          </button>
        </div>

        <div className="ab-topnav-right">
          <div className="ab-search-wrapper">
            <span className="ab-search-icon-inp" aria-hidden>⌕</span>
            <input
              type="search"
              className="ab-topnav-search"
              placeholder="Search"
              onKeyDown={(e) => {
                if (e.key === 'Enter') navigate('/search');
              }}
            />
          </div>
          <button
            type="button"
            className="ab-topnav-icon-btn"
            title="Notifications"
            onClick={() => navigate('/notifications')}
          >
            🔔
          </button>
          <button type="button" className="ab-topnav-icon-btn" title="Help">
            ?
          </button>
          <div className="ab-topnav-divider" />
          <button
            type="button"
            className="ab-user-avatar-btn"
            title={`${user?.username || 'User'} — sign out`}
            onClick={logout}
          >
            {initials}
          </button>
        </div>
      </header>

      <div className="ab-jira-body">
        <aside className={`ab-project-sidebar ab-app-sidebar ${sidebarCollapsed ? 'ab-app-sidebar-collapsed' : ''}`}>
          <div className="ab-sidebar-project-header">
            <div className="ab-project-avatar">JP</div>
            <div className="ab-project-info">
              <h2 className="ab-project-title">Jira Platform</h2>
              {!sidebarCollapsed && (
                <span className="ab-app-sidebar-subtitle">Data Center</span>
              )}
            </div>
          </div>

          <nav className="ab-sidebar-nav" aria-label="Application">
            {!sidebarCollapsed && <div className="ab-sidebar-section-label">Navigation</div>}
            {SIDE_NAV.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `ab-sidebar-nav-item ${isActive ? 'active' : ''}`
                }
                title={sidebarCollapsed ? item.name : undefined}
              >
                <span className="ab-nav-icon ab-nav-dot" />
                {!sidebarCollapsed && <span className="ab-nav-text">{item.name}</span>}
              </NavLink>
            ))}

            {!sidebarCollapsed && (
              <div className="ab-sidebar-section-label ab-sidebar-section-spaced">System</div>
            )}
            {SYSTEM_NAV.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                end={item.path === '/admin'}
                className={({ isActive }) =>
                  `ab-sidebar-nav-item ${isActive ? 'active' : ''}`
                }
                title={sidebarCollapsed ? item.name : undefined}
              >
                <span className="ab-nav-icon ab-nav-dot" />
                {!sidebarCollapsed && <span className="ab-nav-text">{item.name}</span>}
              </NavLink>
            ))}
          </nav>

          <div className="ab-sidebar-footer">
            {!sidebarCollapsed && (
              <div className="ab-system-status-inline">
                <span className="ab-status-dot" />
                <span>All systems operational</span>
              </div>
            )}
            <button
              type="button"
              className="ab-collapse-btn"
              title={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
              onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
            >
              {sidebarCollapsed ? '»' : '‹'}
            </button>
          </div>
        </aside>

        <main className="ab-main-content ab-app-main">
          <div className="ab-content-scroll ab-app-page">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
