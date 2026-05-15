import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';

const TOPNAV_ITEMS = [
  { label: 'Dashboards', path: '/dashboard' },
  { label: 'Projects', path: '/projects' },
  { label: 'Issues', path: '/issues' },
  { label: 'Boards', path: '/boards' },
  { label: 'Plans', path: '/plans' },
];

interface JiraGlobalLayoutProps {
  children: React.ReactNode;
  projectName?: string;
  projectKey?: string;
  projectAvatar?: string;
  boardName?: string;
  activeSection?: string;
}

export default function JiraGlobalLayout({
  children,
  projectName = 'My Kanban',
  projectKey,
  projectAvatar,
  boardName,
  activeSection = 'board',
}: JiraGlobalLayoutProps) {
  const location = useLocation();
  const navigate = useNavigate();

  return (
    <div className="ab-jira-root">
      {/* 1. Access Banner */}
      <div className="ab-access-banner">
        <span className="ab-banner-warning-icon">⚠</span>
        <span className="ab-banner-text">You have temporary access to Jira administration.</span>
        <a href="#" className="ab-banner-link" onClick={(e) => e.preventDefault()}>Drop access</a>
        <span className="ab-banner-text">·</span>
        <a href="#" className="ab-banner-link" onClick={(e) => e.preventDefault()}>Learn more</a>
      </div>

      {/* 2. Top Nav Bar */}
      <header className="ab-jira-topnav">
        <div className="ab-topnav-left">
          <Link to="/" className="ab-jira-logo-btn">
            <div className="ab-jira-logo-icon">J</div>
            <span className="ab-jira-logo-text">Jira</span>
          </Link>
        </div>

        <div className="ab-topnav-center">
          <div className="ab-topnav-items">
            {TOPNAV_ITEMS.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                className={`ab-topnav-item ${location.pathname.startsWith(item.path) ? 'active' : ''}`}
              >
                {item.label}
              </Link>
            ))}
          </div>
          <button className="ab-create-btn" onClick={() => navigate('/projects/new')}>
            <span>+</span> Create
          </button>
        </div>

        <div className="ab-topnav-right">
          <div className="ab-search-wrapper">
            <span className="ab-search-icon-inp">🔍</span>
            <input
              type="text"
              className="ab-topnav-search"
              placeholder="Search..."
              onFocus={(e) => { (e.target as HTMLInputElement).style.width = '240px'; }}
              onBlur={(e) => { (e.target as HTMLInputElement).style.width = '180px'; }}
            />
          </div>
          <button className="ab-topnav-icon-btn" title="Notifications">🔔</button>
          <button className="ab-topnav-icon-btn" title="Help">?</button>
          <div className="ab-topnav-divider" />
          <button className="ab-user-avatar-btn" title="User menu">SS</button>
        </div>
      </header>

      {/* 3. Body */}
      <div className="ab-jira-body">
        {/* Left Sidebar */}
        <aside className="ab-project-sidebar">
          <div className="ab-sidebar-project-header">
            <div className="ab-project-avatar">
              {projectAvatar ? (
                <img src={projectAvatar} alt={projectName} />
              ) : (
                projectKey ? projectKey.substring(0, 2).toUpperCase() : 'PR'
              )}
            </div>
            <div className="ab-project-info">
              <h2 className="ab-project-title">{projectName}</h2>
              {boardName && (
                <button className="ab-board-selector">
                  {boardName} ▾
                </button>
              )}
            </div>
          </div>

          <nav className="ab-sidebar-nav">
            <div className="ab-sidebar-section-label">Project</div>

            <button className={`ab-sidebar-nav-item ${activeSection === 'board' ? 'active' : ''}`}>
              <span className="ab-nav-icon">📋</span>
              <span className="ab-nav-text">Kanban board</span>
            </button>
            <button className={`ab-sidebar-nav-item ${activeSection === 'releases' ? 'active' : ''}`}>
              <span className="ab-nav-icon">🚀</span>
              <span className="ab-nav-text">Releases</span>
            </button>
            <button className={`ab-sidebar-nav-item ${activeSection === 'reports' ? 'active' : ''}`}>
              <span className="ab-nav-icon">📊</span>
              <span className="ab-nav-text">Reports</span>
            </button>
            <button className={`ab-sidebar-nav-item ${activeSection === 'issues' ? 'active' : ''}`}>
              <span className="ab-nav-icon">🐛</span>
              <span className="ab-nav-text">Issues</span>
              <span className="ab-nav-count">1</span>
            </button>
            <button className={`ab-sidebar-nav-item ${activeSection === 'components' ? 'active' : ''}`}>
              <span className="ab-nav-icon">🧩</span>
              <span className="ab-nav-text">Components</span>
            </button>
          </nav>

          <div className="ab-sidebar-shortcuts">
            <div className="ab-shortcuts-label">Shortcuts</div>
            <div className="ab-shortcuts-hint">Access frequent pages faster</div>
            <button className="ab-add-shortcut-btn">+ Add shortcut</button>
          </div>

          <div className="ab-sidebar-footer">
            <button className="ab-settings-btn">
              <span>⚙</span> Project settings
            </button>
            <button className="ab-collapse-btn" title="Collapse sidebar">‹</button>
          </div>
        </aside>

        {/* Main Content */}
        <main className="ab-main-content">
          <div className="ab-content-scroll">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}