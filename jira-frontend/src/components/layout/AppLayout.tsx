import { Outlet, NavLink } from 'react-router-dom';
import { useAuth } from '../../features/auth/context/AuthContext';
import { useState } from 'react';
import '../../features/auth/pages/AuthStyles.css';

const navigation = [
  { name: 'Dashboard', path: '/dashboard', icon: 'ab-icon-dashboard' },
  { name: 'Projects', path: '/projects', icon: 'ab-icon-folder' },
  { name: 'Programs', path: '/programs', icon: 'ab-icon-program' },
  { name: 'Issues', path: '/issues', icon: 'ab-icon-list' },
  { name: 'Boards', path: '/boards', icon: 'ab-icon-board' },
  { name: 'Sprints', path: '/sprints', icon: 'ab-icon-sprint' },
  { name: 'Workflows', path: '/workflows', icon: 'ab-icon-flow' },
  { name: 'Search', path: '/search', icon: 'ab-icon-search' },
  { name: 'Notifications', path: '/notifications', icon: 'ab-icon-bell' },
];

export default function AppLayout() {
  const { user, logout } = useAuth();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  return (
    <div className="ab-app-layout">
      {/* Header */}
      <header className="ab-app-header">
        <div className="ab-header-left">
          <button className="ab-sidebar-toggle" onClick={() => setSidebarCollapsed(!sidebarCollapsed)}>
            <span className="ab-icon-menu"></span>
          </button>
          <div className="ab-brand">
            <span className="ab-logo">JP</span>
            <span className="ab-brand-name">Jira Platform</span>
          </div>
        </div>

        <div className="ab-header-center">
          <div className="ab-global-search">
            <span className="ab-icon-search"></span>
            <input type="text" placeholder="Search issues, projects..." className="ab-search-input" />
            <span className="ab-search-shortcut">/</span>
          </div>
        </div>

        <div className="ab-header-right">
          <button className="ab-icon-button" title="Create Issue" onClick={() => window.dispatchEvent(new CustomEvent('openCreateIssue'))}>
            <span className="ab-icon-plus"></span>
          </button>
          <button className="ab-icon-button" title="Notifications">
            <span className="ab-icon-bell"></span>
          </button>
          <div className="ab-user-menu">
            <div className="ab-avatar">{user?.username?.charAt(0).toUpperCase() || 'U'}</div>
            <div className="ab-user-info">
              <span className="ab-user-name">{user?.username || 'User'}</span>
              <span className="ab-user-role">{user?.roles?.[0] || 'User'}</span>
            </div>
            <button className="ab-logout-btn" onClick={logout} title="Logout">
              <span className="ab-icon-logout"></span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <div className="ab-app-container">
        {/* Side Navigation */}
        <nav className={`ab-sidebar ${sidebarCollapsed ? 'ab-collapsed' : ''}`}>
          <div className="ab-sidebar-nav">
            {navigation.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `ab-nav-item ${isActive ? 'ab-active' : ''}`
                }
              >
                <span className={`${item.icon} ab-nav-icon`}></span>
                <span className="ab-nav-label">{item.name}</span>
              </NavLink>
            ))}
          </div>

          <div className="ab-sidebar-footer">
            <div className="ab-system-status">
              <span className="ab-status-indicator ab-status-ok"></span>
              <span className="ab-status-text">All systems operational</span>
            </div>
          </div>
        </nav>

        {/* Main Content */}
        <main className="ab-main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}